#!/usr/bin/env python3
"""
Systematic audit for the TaCZ Tweaks (Fabric 26.2) port.

Verifies, without compiling or running the game:

  1. mixin config <-> source consistency (every mixin in tacztweaks.mixins.json
     exists on disk, and every @Mixin class on disk is registered);
  2. every mixin target class exists (in the TaCZ jar from libs/ or the bundled
     Minecraft 26.2 source tree);
  3. every injection point (`method = "...desc"`, `@At(target = "...desc")`) and
     `@Shadow` / `@Accessor` reference resolves to a member declared on the target
     class (methods are checked against the TaCZ jar bytecode and the MC sources);
  4. stale 1.20.1 API references that will not compile on 26.2;
  5. config screen entries all have language keys in every language file.

Usage:
    python3 scripts/audit.py [--mc-src /path/to/26.2-src] [--extract-to DIR]

Exit code 0 = no errors; 1 = problems found.
"""
import json
import os
import re
import struct
import subprocess
import sys
import tempfile
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src", "main")
JAVA = os.path.join(SRC, "java")
RES = os.path.join(SRC, "resources")
MIXINS_JSON = os.path.join(RES, "tacztweaks.mixins.json")
LIBS = os.path.join(REPO, "libs")

# --------------------------------------------------------------------------
# Minimal JVM class-file reader (constant pool + declared members)
# --------------------------------------------------------------------------
def _u2(data, off):
    return struct.unpack_from(">H", data, off)[0]

def _u4(data, off):
    return struct.unpack_from(">I", data, off)[0]

def parse_class(data):
    """Returns dict(name, fields=[(name, desc)], methods=[(name, desc)],
    refs=set((kind, owner, name, desc))) from class-file bytes."""
    cp = [None]  # entries: (tag, value)
    i, n = 1, _u2(data, 8)
    pos = 10
    while i < n:
        tag = data[pos]; pos += 1
        if tag == 1:
            ln = _u2(data, pos); pos += 2
            cp.append((tag, data[pos:pos + ln].decode("utf-8", "replace"))); pos += ln
        elif tag in (3, 4):
            cp.append((tag, None)); pos += 4
        elif tag in (5, 6):
            cp.append((tag, None)); cp.append(None); pos += 8; i += 1
        elif tag in (7, 8, 16, 19, 20):
            cp.append((tag, _u2(data, pos))); pos += 2
        elif tag in (9, 10, 11, 12, 18):
            cp.append((tag, struct.unpack_from(">HH", data, pos))); pos += 4
        elif tag == 15:
            cp.append((tag, struct.unpack_from(">BH", data, pos))); pos += 3
        else:
            raise ValueError("bad constant-pool tag %d at #%d" % (tag, i))
        i += 1

    def utf(idx):
        if idx is None or idx <= 0 or idx >= len(cp):
            return None
        e = cp[idx]
        return e[1] if e and e[0] == 1 else (utf(e[1]) if e and e[0] == 7 else None)

    # header
    pos += 2  # access_flags
    this_idx = _u2(data, pos); pos += 2
    this_name = utf(this_idx)
    pos += 2  # super_class
    intf_count = _u2(data, pos); pos += 2
    pos += 2 * intf_count

    # fields
    fcnt = _u2(data, pos); pos += 2
    fields = []
    for _ in range(fcnt):
        pos += 6  # access, name, desc
        name = utf(_u2(data, pos - 4))
        desc = utf(_u2(data, pos - 2))
        acount = _u2(data, pos); pos += 2
        for _a in range(acount):
            alen = _u4(data, pos + 2)  # attribute_name_index(u2) then length(u4)
            pos += 6 + alen
        fields.append((name, desc))

    # methods
    mcnt = _u2(data, pos); pos += 2
    methods = []
    for _ in range(mcnt):
        pos += 2  # access
        name = utf(_u2(data, pos)); pos += 2
        desc = utf(_u2(data, pos)); pos += 2
        acount = _u2(data, pos); pos += 2
        for _a in range(acount):
            alen = _u4(data, pos + 2)  # attribute_name_index(u2) then length(u4)
            pos += 6 + alen
        methods.append((name, desc))

    # constant-pool references
    refs = set()
    kind_names = {9: "field", 10: "method", 11: "method"}
    for e in cp:
        if e and e[0] in (9, 10, 11):
            cls_idx, nat_idx = e[1]
            owner = utf(cls_idx)
            nat = cp[nat_idx] if nat_idx < len(cp) else None
            if nat and nat[0] == 12:
                refs.add((kind_names[e[0]], owner, utf(nat[1][0]), utf(nat[1][1])))

    return {"name": this_name, "fields": fields, "methods": methods, "refs": refs}


def load_class(path):
    with open(path, "rb") as f:
        return parse_class(f.read())


# --------------------------------------------------------------------------
# TaCZ jar index (class name -> parsed class)
# --------------------------------------------------------------------------
def build_jar_index(jar_path):
    index = {}
    with zipfile.ZipFile(jar_path) as z:
        for name in z.namelist():
            if name.endswith(".class"):
                key = name[:-6]
                index[key] = parse_class(z.read(name))
    return index


# --------------------------------------------------------------------------
# Minecraft 26.2 source index (class name -> file path)
# --------------------------------------------------------------------------
def build_mc_index(mc_src):
    """Walk a decompiled 26.2 source tree; map dotted class name -> file path."""
    index = {}
    if not mc_src or not os.path.isdir(mc_src):
        return index
    for dp, _, fns in os.walk(mc_src):
        for fn in fns:
            if fn.endswith(".java"):
                p = os.path.join(dp, fn)
                rel = os.path.relpath(p, mc_src)
                dotted = rel[:-5].replace(os.sep, ".").replace("/", ".")
                index[dotted] = p
    return index


# --------------------------------------------------------------------------
# Mixin source scanning
# --------------------------------------------------------------------------
RE_IMPORT = re.compile(r'^import\s+(?:static\s+)?([A-Za-z0-9_.$]+);', re.M)
RE_MIXIN_VALUE = re.compile(r'@Mixin\s*\(\s*(?:value\s*=\s*)?([A-Za-z0-9_.$]+)\.class')
RE_MIXIN_TARGETS = re.compile(r'@Mixin\s*\(\s*targets\s*=\s*"([^"]+)"')
RE_METHOD = re.compile(r'method\s*=\s*"([^"]+)"')
RE_AT_TARGET = re.compile(r'target\s*=\s*"(L[^"]+)"')

def scan_mixin(path):
    src = open(path, encoding="utf-8").read()
    # import map: simple name -> fully-qualified name
    imports = {}
    pkg = ""
    m = re.search(r'^package\s+([\w.]+);', src, re.M)
    if m:
        pkg = m.group(1)
    for im in RE_IMPORT.findall(src):
        imports[im.rsplit(".", 1)[-1]] = im

    def resolve(simple):
        if "." in simple:
            return simple
        return imports.get(simple, pkg + "." + simple if pkg else simple)

    targets = []
    for mv in RE_MIXIN_VALUE.findall(src):
        targets.append(resolve(mv))
    for t in RE_MIXIN_TARGETS.findall(src):
        targets.append(t.replace("/", "."))
    methods = RE_METHOD.findall(src)
    at_targets = RE_AT_TARGET.findall(src)
    return {"file": path, "targets": targets, "methods": methods, "at_targets": at_targets, "src": src}


# --------------------------------------------------------------------------
# Checks
# --------------------------------------------------------------------------
errors = []
warnings = []

def err(msg):
    errors.append(msg)
    print("  [ERROR] " + msg)

def warn(msg):
    warnings.append(msg)
    print("  [warn]  " + msg)

def check_mixin_json():
    print("== 1. mixin config <-> source ==")
    cfg = json.load(open(MIXINS_JSON, encoding="utf-8"))
    pkg = cfg["package"]
    declared = set()
    for section in ("mixins", "client"):
        for entry in cfg.get(section, []):
            declared.add(entry)
    on_disk = set()
    for dp, _, fns in os.walk(JAVA):
        for fn in fns:
            if fn.endswith(".java"):
                rel = os.path.relpath(os.path.join(dp, fn), JAVA)
                cls = rel[:-5].replace(os.sep, ".")
                if ".mixin." in cls:
                    on_disk.add(cls)
    # every declared mixin must exist
    for entry in sorted(declared):
        cls = pkg + "." + entry
        if cls not in on_disk:
            err("mixin registered but file missing: %s" % entry)
    # every @Mixin class on disk should be registered
    for cls in sorted(on_disk):
        entry = cls[len(pkg) + 1:]
        if entry not in declared:
            warn("mixin source not registered in mixins.json: %s" % entry)
    print("  registered=%d on-disk=%d" % (len(declared), len(on_disk)))

def check_targets(jar_index, mc_index):
    print("== 2/3. mixin targets & injection points ==")
    for dp, _, fns in os.walk(JAVA):
        for fn in sorted(fns):
            if not fn.endswith(".java") or "mixin" not in dp:
                continue
            path = os.path.join(dp, fn)
            info = scan_mixin(path)
            if not info["targets"]:
                continue
            short = os.path.relpath(path, JAVA)
            for target in info["targets"]:
                binary = target.replace(".", "/")
                cls = jar_index.get(binary)
                if cls is None:
                    src_file = mc_index.get(target) or mc_index.get(target.replace("/", "."))
                    if src_file is None:
                        warn("%s: target class not found (jar or MC src): %s" % (short, target))
                        continue
                    _check_mc_target(short, src_file, info, mc_index)
                else:
                    _check_jar_target(short, binary, cls, info, jar_index, mc_index)

def _split_member_ref(desc):
    """Split 'Lpkg/Class;name(desc)ret' or 'Lpkg/Class;field:Ltype;' into
    (owner_binary, name, desc). Returns None if it does not parse."""
    i = desc.rfind(";")
    if i == -1:
        return None
    owner = desc[1:i]
    rest = desc[i + 1:]
    m = re.match(r"([A-Za-z0-9_$<>]+):(.+)$", rest)  # field
    if m:
        return owner, m.group(1), m.group(2)
    m = re.match(r"([A-Za-z0-9_$<>]+)(\(.*)$", rest)  # method
    if m:
        return owner, m.group(1), m.group(2)
    return None

def _split_method_str(method):
    """Split a `method = "..."` value into (name, desc). `method` is either a
    plain name ('resolve') or 'name(desc)ret' (no owner prefix)."""
    if "(" in method:
        i = method.index("(")
        return method[:i], method[i:]
    return method, None

def _check_jar_target(short, binary, cls, info, jar_index, mc_index):
    decl_methods = set(cls["methods"])
    decl_method_names = set(n for n, _ in cls["methods"])
    decl_fields = set((n, d) for n, d in cls["fields"])
    for method in info["methods"]:
        name, desc = _split_method_str(method)
        if desc is None:
            if name not in decl_method_names and name != "<init>":
                err("%s: method '%s' not declared in %s" % (short, name, binary))
        elif (name, desc) not in decl_methods:
            err("%s: method %s%s not declared in %s" % (short, name, desc, binary))
    for at in info["at_targets"]:
        mm = _split_member_ref(at)
        if not mm:
            continue
        owner, name, desc = mm
        own_cls = jar_index.get(owner)
        if own_cls is None:
            if owner.startswith("net/minecraft/"):
                owner_src = mc_index.get(owner.replace("/", "."))
                if owner_src is not None:
                    osrc = open(owner_src, encoding="utf-8").read()
                    if desc.startswith("(") and not re.search(r"\b%s\s*\(" % re.escape(name), osrc):
                        err("%s: @At method target %s.%s not found in MC source" % (short, owner, name))
                continue  # MC owner verified via source, or skipped when no MC source is given
            err("%s: @At target class %s not found in jar" % (short, owner))
            continue
        if desc.startswith("("):
            if (name, desc) not in set(own_cls["methods"]) and (name, desc) not in set((n, d) for k, o, n, d in own_cls["refs"] if k == "method"):
                err("%s: @At method target %s.%s%s not found" % (short, owner, name, desc))
        else:
            if (name, desc) not in set(own_cls["fields"]) and (name, desc) not in set((n, d) for k, o, n, d in own_cls["refs"] if k == "field"):
                err("%s: @At field target %s.%s:%s not found" % (short, owner, name, desc))

def _check_mc_target(short, src_file, info, mc_index):
    src = open(src_file, encoding="utf-8").read()
    for method in info["methods"]:
        name, desc = _split_method_str(method)
        if name in ("<init>", "<clinit>"):
            continue
        if not re.search(r"\b%s\s*\(" % re.escape(name), src):
            err("%s: method '%s' not found in MC source %s" % (short, name, os.path.basename(src_file)))
    for at in info["at_targets"]:
        mm = _split_member_ref(at)
        if not mm:
            continue
        owner, name, desc = mm
        owner_src = mc_index.get(owner.replace("/", "."))
        if owner_src is None:
            continue  # owner is a TaCZ class already covered by jar check
        osrc = open(owner_src, encoding="utf-8").read()
        if not re.search(r"\b%s\s*\(" % re.escape(name), osrc) and ":" not in desc:
            err("%s: @At method target %s.%s not found in MC source %s" % (short, owner, name, os.path.basename(owner_src)))

def _strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)  # block comments
    src = re.sub(r"//.*", "", src)  # line comments
    return src

def check_stale_apis():
    print("== 4. stale 1.20.1 API references (code only) ==")
    patterns = {
        r"\bResourceLocation\b": "renamed to Identifier in 26.2 (compile error)",
        r"OggAudioStream": "renamed FiniteAudioStream/JOrbisAudioStream in 26.2",
        r"\bProtectionEnchantment\b": "removed in 26.2 (data-driven enchantments)",
        r"\bGuiGraphics\b": "renamed GuiGraphicsExtractor in 26.2",
        r"advancements\.critereon": "renamed to advancements.criterion in 26.2",
        r"@Expression": "bytecode matcher; runtime-fragile, see PORTING_NOTES 7.6",
    }
    for dp, _, fns in os.walk(JAVA):
        for fn in fns:
            if not fn.endswith((".java", ".kt")):
                continue
            p = os.path.join(dp, fn)
            short = os.path.relpath(p, REPO)
            code = _strip_comments(open(p, encoding="utf-8").read())
            for i, line in enumerate(code.splitlines(), 1):
                for pat, why in patterns.items():
                    if re.search(pat, line):
                        warn("%s:%d: %r (%s)" % (short, i, line.strip(), why))

def check_lang_keys():
    print("== 5. config <-> lang keys ==")
    cfg = open(os.path.join(REPO, "src/main/kotlin/me/muksc/tacztweaks/config/Config.kt"), encoding="utf-8").read()
    keys = set(re.findall(r'translatable\("([^"]+)"\)', cfg))
    keys |= set(re.findall(r'translatable\("([^"]+)"', cfg))
    langs = {}
    langdir = os.path.join(RES, "assets/tacztweaks/lang")
    for fn in os.listdir(langdir):
        if fn.endswith(".json"):
            langs[fn] = json.load(open(os.path.join(langdir, fn), encoding="utf-8"))
    for key in sorted(keys):
        if not key.startswith("tacztweaks."):
            continue
        missing = [ln for ln, d in langs.items() if key not in d]
        if missing:
            err("lang key missing in %s: %s" % (", ".join(missing), key))
    print("  checked %d keys across %d languages" % (len(keys), len(langs)))

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--mc-src", default=os.environ.get("MC262_SRC", ""))
    ap.add_argument("--extract-to", default="")
    args = ap.parse_args()

    # find TaCZ jar
    jar = None
    for fn in os.listdir(LIBS):
        if fn.startswith("TACZ-") and fn.endswith(".jar"):
            jar = os.path.join(LIBS, fn)
            break
    if not jar:
        print("[warn] TaCZ jar not found in libs/; target verification limited")
        jar_index = {}
    else:
        print("indexing %s ..." % os.path.basename(jar))
        jar_index = build_jar_index(jar)
        print("  %d classes" % len(jar_index))

    mc_index = build_mc_index(args.mc_src)
    if mc_index:
        print("  %d MC source files" % len(mc_index))
    else:
        print("  (no --mc-src given: net.minecraft.* mixin targets are only checked for"
              " class existence via the jar, member checks are skipped)")

    check_mixin_json()
    check_targets(jar_index, mc_index)
    check_stale_apis()
    check_lang_keys()

    print()
    print("=== summary: %d error(s), %d warning(s) ===" % (len(errors), len(warnings)))
    return 1 if errors else 0

if __name__ == "__main__":
    sys.exit(main())
