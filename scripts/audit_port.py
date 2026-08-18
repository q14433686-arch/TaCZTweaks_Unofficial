#!/usr/bin/env python3
"""Static audit for the Fabric 26.2 TaCZ Tweaks port.

The script deliberately uses only the Python standard library so it can run before
Gradle in CI.  It catches the easy-to-miss failures that compilation alone does not:

* mixin classes omitted from (or misspelled in) the mixin JSON;
* mixins aimed at methods absent from the bundled TaCZ/LRTactical jar;
* config switches which are persisted/synchronised but never read by behaviour code;
* language-file drift;
* a bundled MixinExtras version lower than the mixin config's declared minimum.

Pass --minecraft-jar after Loom has prepared Minecraft to validate vanilla mixin
method names too. Pass --upstream-root with a TaCZTweaks v2.14.2 checkout to print a
source inventory comparison; that report is informational because many upstream
mixins were intentionally merged or redesigned in this port.
"""

from __future__ import annotations

import argparse
import json
import re
import struct
import sys
import zipfile
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src/main"
MIXIN_ROOT = SOURCE_ROOT / "java/me/muksc/tacztweaks/mixin"
MIXIN_JSON = SOURCE_ROOT / "resources/tacztweaks.mixins.json"
TA_CZ_JAR = ROOT / "libs/TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar"
CONFIG = SOURCE_ROOT / "kotlin/me/muksc/tacztweaks/config/Config.kt"

# Any intentionally persisted but dormant legacy fields must be justified here. The
# current port keeps none: unavailable compat switches were removed rather than no-op'd.
KNOWN_DORMANT_OPTIONS: dict[str, str] = {}


@dataclass(frozen=True)
class ClassInfo:
    methods: frozenset[str]
    fields: frozenset[str]
    method_signatures: frozenset[tuple[str, str]]
    field_signatures: frozenset[tuple[str, str]]
    method_references: dict[tuple[str, str], frozenset[tuple[str, str, str]]]
    super_name: str | None
    interfaces: tuple[str, ...]


def _u1(data: memoryview, offset: int) -> tuple[int, int]:
    return data[offset], offset + 1


def _u2(data: memoryview, offset: int) -> tuple[int, int]:
    return struct.unpack_from(">H", data, offset)[0], offset + 2


def _u4(data: memoryview, offset: int) -> tuple[int, int]:
    return struct.unpack_from(">I", data, offset)[0], offset + 4


def read_class_info(raw: bytes) -> ClassInfo:
    """Read field and method names from a class file (including Java 25 files)."""
    data = memoryview(raw)
    magic, off = _u4(data, 0)
    if magic != 0xCAFEBABE:
        raise ValueError("not a class file")
    off += 4  # minor + major
    cp_count, off = _u2(data, off)
    utf8: dict[int, str] = {}
    class_name_indices: dict[int, int] = {}
    name_and_type_indices: dict[int, tuple[int, int]] = {}
    member_ref_indices: dict[int, tuple[int, int]] = {}
    index = 1
    while index < cp_count:
        tag, off = _u1(data, off)
        if tag == 1:  # CONSTANT_Utf8
            length, off = _u2(data, off)
            utf8[index] = bytes(data[off : off + length]).decode("utf-8", "replace")
            off += length
        elif tag in (3, 4):
            off += 4
        elif tag in (5, 6):
            off += 8
            index += 1  # long/double occupy two entries
        elif tag == 7:
            name_index, off = _u2(data, off)
            class_name_indices[index] = name_index
        elif tag in (8, 16, 19, 20):
            off += 2
        elif tag in (9, 10, 11):
            class_index, off = _u2(data, off)
            name_and_type_index, off = _u2(data, off)
            member_ref_indices[index] = (class_index, name_and_type_index)
        elif tag == 12:
            name_index, off = _u2(data, off)
            descriptor_index, off = _u2(data, off)
            name_and_type_indices[index] = (name_index, descriptor_index)
        elif tag in (17, 18):
            off += 4
        elif tag == 15:
            off += 3
        else:
            raise ValueError(f"unknown constant-pool tag {tag}")
        index += 1

    off += 2  # access_flags
    _this_class, off = _u2(data, off)
    super_class, off = _u2(data, off)
    super_name = None
    if super_class:
        super_name = utf8.get(class_name_indices.get(super_class, -1), "").replace("/", ".") or None
    interface_count, off = _u2(data, off)
    interface_names: list[str] = []
    for _ in range(interface_count):
        interface_index, off = _u2(data, off)
        name = utf8.get(class_name_indices.get(interface_index, -1), "").replace("/", ".")
        if name:
            interface_names.append(name)

    def resolve_member_ref(cp_index: int) -> tuple[str, str, str] | None:
        pair = member_ref_indices.get(cp_index)
        if pair is None:
            return None
        class_index, nat_index = pair
        nat = name_and_type_indices.get(nat_index)
        if nat is None:
            return None
        owner = utf8.get(class_name_indices.get(class_index, -1), "")
        name = utf8.get(nat[0], "")
        descriptor = utf8.get(nat[1], "")
        return (owner, name, descriptor) if owner and name and descriptor else None

    method_references: dict[tuple[str, str], frozenset[tuple[str, str, str]]] = {}

    def members(offset: int, collect_code: bool = False) -> tuple[list[tuple[str, str]], int]:
        count, offset = _u2(data, offset)
        members_found: list[tuple[str, str]] = []
        for _ in range(count):
            offset += 2  # access flags
            name_index, offset = _u2(data, offset)
            descriptor_index, offset = _u2(data, offset)
            signature = (
                utf8.get(name_index, f"<cp:{name_index}>"),
                utf8.get(descriptor_index, f"<cp:{descriptor_index}>"),
            )
            members_found.append(signature)
            refs: set[tuple[str, str, str]] = set()
            attribute_count, offset = _u2(data, offset)
            for _ in range(attribute_count):
                attribute_name_index, offset = _u2(data, offset)
                length, offset = _u4(data, offset)
                if collect_code and utf8.get(attribute_name_index) == "Code" and length >= 8:
                    code_length = struct.unpack_from(">I", data, offset + 4)[0]
                    code = data[offset + 8 : offset + 8 + code_length]
                    # All field/method invocation opcodes carry a two-byte constant-pool
                    # index. Scanning only indices which resolve to an actual Memberref
                    # avoids needing a full bytecode interpreter while retaining no misses.
                    for pos in range(max(0, len(code) - 2)):
                        if code[pos] not in (0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9):
                            continue
                        cp_index = (code[pos + 1] << 8) | code[pos + 2]
                        resolved = resolve_member_ref(cp_index)
                        if resolved is not None:
                            refs.add(resolved)
                offset += length
            if collect_code:
                method_references[signature] = frozenset(refs)
        return members_found, offset

    fields, off = members(off)
    methods, off = members(off, collect_code=True)
    return ClassInfo(
        frozenset(name for name, _ in methods),
        frozenset(name for name, _ in fields),
        frozenset(methods),
        frozenset(fields),
        method_references,
        super_name,
        tuple(interface_names),
    )


class JarIndex:
    def __init__(self, paths: Iterable[Path]):
        self.paths = [p for p in paths if p and p.is_file()]
        self._zips = [zipfile.ZipFile(p) for p in self.paths]
        self._cache: dict[str, ClassInfo | None] = {}

    def close(self) -> None:
        for archive in self._zips:
            archive.close()

    def class_info(self, fqcn: str) -> ClassInfo | None:
        if fqcn in self._cache:
            return self._cache[fqcn]
        # A targets="a.b.Outer$Inner" string already uses '$'. A source-level inner
        # class can occasionally be written with dots, so progressively try '$'.
        candidates = [fqcn.replace(".", "/") + ".class"]
        parts = fqcn.split(".")
        for split in range(len(parts) - 1, 0, -1):
            candidates.append("/".join(parts[:split]) + "$" + "$".join(parts[split:]) + ".class")
        for archive in self._zips:
            names = set(archive.namelist())
            for candidate in candidates:
                if candidate in names:
                    info = read_class_info(archive.read(candidate))
                    self._cache[fqcn] = info
                    return info
        self._cache[fqcn] = None
        return None

    def has_method(self, fqcn: str, signature: tuple[str, str], seen: set[str] | None = None) -> bool:
        seen = seen or set()
        if fqcn in seen:
            return False
        seen.add(fqcn)
        info = self.class_info(fqcn)
        if info is None:
            return False
        if signature in info.method_signatures:
            return True
        return any(
            self.has_method(parent, signature, seen)
            for parent in ((info.super_name,) if info.super_name else ()) + info.interfaces
        )

    def has_field(self, fqcn: str, signature: tuple[str, str], seen: set[str] | None = None) -> bool:
        seen = seen or set()
        if fqcn in seen:
            return False
        seen.add(fqcn)
        info = self.class_info(fqcn)
        if info is None:
            return False
        if signature in info.field_signatures:
            return True
        return any(
            self.has_field(parent, signature, seen)
            for parent in ((info.super_name,) if info.super_name else ()) + info.interfaces
        )


def version_tuple(value: str) -> tuple[int, ...]:
    return tuple(int(piece) for piece in re.findall(r"\d+", value))


def listed_mixins(config: dict) -> set[str]:
    package = config["package"]
    return {f"{package}.{name}" for side in ("mixins", "client", "server") for name in config.get(side, [])}


def source_mixin_classes() -> set[str]:
    result: set[str] = set()
    for path in MIXIN_ROOT.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if "@Mixin" not in text:
            continue
        package_match = re.search(r"\bpackage\s+([\w.]+)\s*;", text)
        type_match = re.search(
            r"^(?:public\s+)?(?:abstract\s+)?(?:final\s+)?(?:class|interface)\s+([\w$]+)",
            text,
            re.M,
        )
        if package_match and type_match:
            result.add(f"{package_match.group(1)}.{type_match.group(1)}")
    return result


def resolve_target(text: str) -> str | None:
    target_string = re.search(r"@Mixin\s*\([^)]*?targets\s*=\s*\"([^\"]+)\"", text, re.S)
    if target_string:
        return target_string.group(1)
    target_type = re.search(
        r"@Mixin\s*\(\s*(?:value\s*=\s*)?([A-Za-z_$][\w$]*)\.class", text, re.S
    )
    if not target_type:
        return None
    simple = target_type.group(1)
    imported = re.search(rf"\bimport\s+([\w.$]+\.{re.escape(simple)})\s*;", text)
    if imported:
        return imported.group(1)
    package_match = re.search(r"\bpackage\s+([\w.]+)\s*;", text)
    return f"{package_match.group(1)}.{simple}" if package_match else simple


INJECTOR_ANNOTATIONS = (
    "Inject|ModifyArg|ModifyArgs|ModifyVariable|Redirect|ModifyConstant|"
    "ModifyExpressionValue|ModifyReturnValue|WrapOperation|WrapMethod|WrapWithCondition"
)


def _annotation_body(text: str, open_paren: int) -> str | None:
    depth = 0
    quoted = False
    escaped = False
    for index in range(open_paren, len(text)):
        char = text[index]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[open_paren + 1 : index]
    return None


def _string_constants(text: str) -> dict[str, str]:
    return dict(re.findall(r"\bString\s+(\w+)\s*=\s*\"([^\"]+)\"", text))


def injection_method_names(text: str) -> set[str]:
    names: set[str] = set()
    constants = _string_constants(text)
    for annotation in re.finditer(rf"@({INJECTOR_ANNOTATIONS})\s*\(", text):
        body = _annotation_body(text, text.index("(", annotation.start()))
        if body is None:
            continue
        assignment = re.search(r"\bmethod\s*=\s*(\{[^}}]*\}|\"[^\"]+\"|\w+)", body, re.S)
        if assignment is None:
            continue
        value = assignment.group(1)
        raw_values = re.findall(r'\"([^\"]+)\"', value)
        if not raw_values and value in constants:
            raw_values = [constants[value]]
        for raw in raw_values:
            name = raw.split("(", 1)[0]
            if name and not name.startswith("@"):
                names.add(name)
    return names


def audit_injection_references(
    text: str,
    target: str,
    info: ClassInfo,
) -> list[str]:
    """Return injection call-sites absent from the specific target method bytecode."""
    missing: list[str] = []
    constants = _string_constants(text)
    for annotation in re.finditer(rf"@({INJECTOR_ANNOTATIONS})\s*\(", text):
        body = _annotation_body(text, text.index("(", annotation.start()))
        if body is None:
            continue
        method_match = re.search(r"\bmethod\s*=\s*(\"[^\"]+\"|\w+)", body)
        if method_match is None:
            continue
        method_value = method_match.group(1)
        method_raw = (
            method_value[1:-1]
            if method_value.startswith('"')
            else constants.get(method_value)
        )
        if not method_raw:
            continue
        method_name = method_raw.split("(", 1)[0]
        method_descriptor = (
            "(" + method_raw.split("(", 1)[1]
            if "(" in method_raw else None
        )
        signatures = [
            signature for signature in info.method_signatures
            if signature[0] == method_name
            and (method_descriptor is None or signature[1] == method_descriptor)
        ]
        if not signatures:
            continue  # Reported separately as an absent target method.

        targets: list[str] = []
        for target_match in re.finditer(r"\btarget\s*=\s*(\"[^\"]+\"|\w+)", body):
            target_value = target_match.group(1)
            resolved = (
                target_value[1:-1]
                if target_value.startswith('"')
                else constants.get(target_value)
            )
            if resolved:
                targets.append(resolved)
        for at_target in targets:
            member_match = re.fullmatch(r"L([^;]+);([^(:]+)(\(.*)", at_target)
            if member_match is None:
                field_match = re.fullmatch(r"L([^;]+);([^:]+):(.+)", at_target)
                if field_match is None:
                    continue
                reference = field_match.groups()
            else:
                reference = member_match.groups()
            if any(reference in info.method_references.get(signature, frozenset()) for signature in signatures):
                continue
            missing.append(
                f"{target}#{method_raw} does not contain @At reference {at_target}"
            )
    return missing


def audit_mixins(config: dict, jars: JarIndex, strict: bool) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    listed = listed_mixins(config)
    sources = source_mixin_classes()
    mixin_package = config["package"]
    common_mixins = {f"{mixin_package}.{name}" for name in config.get("mixins", [])}
    for name in sorted(listed - sources):
        errors.append(f"mixin JSON lists a source class that does not exist: {name}")
    for name in sorted(sources - listed):
        errors.append(f"mixin source is not registered in tacztweaks.mixins.json: {name}")

    for path in sorted(MIXIN_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        target = resolve_target(text)
        package_match = re.search(r"\bpackage\s+([\w.]+)\s*;", text)
        type_match = re.search(
            r"^(?:public\s+)?(?:abstract\s+)?(?:final\s+)?(?:class|interface)\s+([\w$]+)",
            text,
            re.M,
        )
        source_name = (
            f"{package_match.group(1)}.{type_match.group(1)}"
            if package_match and type_match else None
        )
        if source_name in common_mixins and target and (
            target.startswith("net.minecraft.client.") or ".client." in target
        ):
            errors.append(
                f"client-only target is listed in common mixins: {source_name} -> {target}"
            )
        if not target:
            warnings.append(f"could not resolve @Mixin target in {path.relative_to(ROOT)}")
            continue
        should_validate = target.startswith(("com.tacz.", "me.xjqsh.", "net.minecraft."))
        if not should_validate:
            continue
        info = jars.class_info(target)
        if info is None:
            message = f"target class not found in supplied jars: {target} ({path.relative_to(ROOT)})"
            # Vanilla targets cannot be checked before a Minecraft jar is supplied.
            if target.startswith("net.minecraft.") and not any("minecraft" in p.name for p in jars.paths):
                continue
            (errors if strict else warnings).append(message)
            continue
        for method in sorted(injection_method_names(text) - info.methods):
            message = f"target method {target}#{method} is absent ({path.relative_to(ROOT)})"
            (errors if strict else warnings).append(message)
        for missing_reference in audit_injection_references(text, target, info):
            message = f"{missing_reference} ({path.relative_to(ROOT)})"
            (errors if strict else warnings).append(message)

        # Validate exact bytecode references used by @At targets whenever the owner is in
        # the supplied Minecraft/TaCZ jars. This catches valid target methods whose inner
        # invocation descriptor drifted (a common source of runtime Mixin apply failures).
        for owner, name, descriptor in re.findall(
            r'target\s*=\s*"L([^;]+);([^(:\"]+)(\([^\"]+)', text
        ):
            owner_name = owner.replace("/", ".")
            owner_info = jars.class_info(owner_name)
            if owner_info is None:
                continue
            if not jars.has_method(owner_name, (name, descriptor)):
                message = (
                    f"@At method reference is absent: {owner_name}#{name}{descriptor} "
                    f"({path.relative_to(ROOT)})"
                )
                (errors if strict else warnings).append(message)
        for owner, name, descriptor in re.findall(
            r'target\s*=\s*"L([^;]+);([^:\"]+):([^\"]+)', text
        ):
            owner_name = owner.replace("/", ".")
            owner_info = jars.class_info(owner_name)
            if owner_info is None:
                continue
            if not jars.has_field(owner_name, (name, descriptor)):
                message = (
                    f"@At field reference is absent: {owner_name}#{name}:{descriptor} "
                    f"({path.relative_to(ROOT)})"
                )
                (errors if strict else warnings).append(message)
    return errors, warnings


def config_options() -> set[str]:
    text = CONFIG.read_text(encoding="utf-8")
    options: set[str] = set()
    object_pattern = re.compile(r"^    object (Gun|Crawl|Compat|Tweaks|Debug)\b[^\n]*\{", re.M)
    for match in object_pattern.finditer(text):
        depth = 1
        cursor = match.end()
        # Config.kt contains no braces in these objects' string literals. A tiny brace
        # scanner is more accurate here than ending at the next selected object because
        # ModifierConfig/Modifiers sit between Gun and Crawl.
        while cursor < len(text) and depth:
            if text[cursor] == "{":
                depth += 1
            elif text[cursor] == "}":
                depth -= 1
            cursor += 1
        block = text[match.end() : cursor - 1]
        for func in re.finditer(r"^        fun\s+(\w+)\(\):", block, re.M):
            options.add(f"{match.group(1)}.{func.group(1)}")
    return options


def audit_config_usage() -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    texts: list[str] = []
    for path in SOURCE_ROOT.rglob("*"):
        if not path.is_file() or path == CONFIG or path.suffix not in {".java", ".kt"}:
            continue
        texts.append(path.read_text(encoding="utf-8"))
    all_source = "\n".join(texts)
    for option in sorted(config_options()):
        group, name = option.split(".", 1)
        patterns = (
            rf"\bConfig\.{group}\.INSTANCE\.{name}\s*\(",
            rf"\bConfig\.{group}\.{name}\s*\(",
        )
        count = sum(len(re.findall(pattern, all_source)) for pattern in patterns)
        if count:
            continue
        reason = KNOWN_DORMANT_OPTIONS.get(option)
        if reason:
            warnings.append(f"dormant compatibility field {option}: {reason}")
        else:
            errors.append(f"config option has no behaviour reader outside Config.kt: {option}")
    return errors, warnings


def audit_firstaid_shader_overrides() -> list[str]:
    errors: list[str] = []
    shader_dir = SOURCE_ROOT / "resources/assets/firstaid/shaders/post"
    expected = {
        "pain_pulse_blur.fsh": "BlurSettings",
        "saturation_boost.fsh": "SaturationSettings",
    }
    for name, required_block in expected.items():
        path = shader_dir / name
        if not path.is_file():
            errors.append(f"missing First Aid 26.2 shader compatibility override: {name}")
            continue
        text = path.read_text(encoding="utf-8")
        if "dynamictransforms.glsl" in text or "DynamicTransforms" in text:
            errors.append(f"{name} reintroduces the unsupported DynamicTransforms block")
        if required_block not in text:
            errors.append(f"{name} no longer defines required block {required_block}")
    return errors


def audit_languages() -> list[str]:
    errors: list[str] = []
    lang_dir = SOURCE_ROOT / "resources/assets/tacztweaks/lang"
    languages = {path.stem: json.loads(path.read_text(encoding="utf-8")) for path in lang_dir.glob("*.json")}
    if "en_us" not in languages:
        return ["missing canonical en_us language file"]
    canonical = set(languages["en_us"])
    for name, values in sorted(languages.items()):
        missing = canonical - set(values)
        extra = set(values) - canonical
        if missing:
            errors.append(f"{name} is missing {len(missing)} keys: {', '.join(sorted(missing))}")
        if extra:
            errors.append(f"{name} has {len(extra)} keys absent from en_us: {', '.join(sorted(extra))}")
    return errors


def audit_versions(config: dict) -> list[str]:
    errors: list[str] = []
    properties = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip()
    required = config.get("mixinextras", {}).get("minVersion")
    bundled = properties.get("mixinextras_version")
    if required and bundled and version_tuple(bundled) < version_tuple(required):
        errors.append(f"MixinExtras {bundled} is lower than mixin JSON minimum {required}")

    mod_version = properties.get("mod_version")
    if not mod_version:
        errors.append("gradle.properties is missing mod_version")
        return errors
    if not re.fullmatch(r"\d+\.\d+\.\d+\+fabric\.26\.2\.R\d+", mod_version):
        errors.append(f"mod_version has an unexpected 26.2 format: {mod_version}")

    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    documented = set(re.findall(r"\d+\.\d+\.\d+\+fabric\.26\.2\.R\d+", readme))
    if documented != {mod_version}:
        errors.append(
            f"README release versions {sorted(documented)} do not uniquely match {mod_version}"
        )
    build_doc = (ROOT / "BUILD.md").read_text(encoding="utf-8")
    if mod_version not in build_doc:
        errors.append(f"BUILD.md does not name the current artifact version {mod_version}")
    return errors


def compare_upstream(path: Path) -> None:
    upstream_root = path / "src/main"
    if not upstream_root.is_dir():
        print(f"UPSTREAM: {upstream_root} is not a source tree", file=sys.stderr)
        return
    upstream = {str(p.relative_to(upstream_root)) for p in upstream_root.rglob("*") if p.is_file()}
    current = {str(p.relative_to(SOURCE_ROOT)) for p in SOURCE_ROOT.rglob("*") if p.is_file()}
    omitted = sorted(
        name for name in upstream - current
        if name.endswith((".java", ".kt")) and "/compat/" not in f"/{name}"
    )
    print(f"UPSTREAM: {len(upstream)} files; port: {len(current)} files; direct-path omissions: {len(omitted)}")
    for name in omitted:
        print(f"  upstream-only {name}")


def find_minecraft_jars(explicit: list[str]) -> list[Path]:
    paths = [Path(value) for value in explicit]
    return paths


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--strict", action="store_true", help="fail on target-class/method mismatches")
    parser.add_argument("--minecraft-jar", action="append", default=[], help="Loom Minecraft jar to inspect")
    parser.add_argument("--upstream-root", type=Path, help="optional MUKSC/TaCZTweaks v2.14.2 checkout")
    args = parser.parse_args()

    errors: list[str] = []
    warnings: list[str] = []
    config = json.loads(MIXIN_JSON.read_text(encoding="utf-8"))
    jar_paths = [TA_CZ_JAR, *find_minecraft_jars(args.minecraft_jar)]
    jars = JarIndex(jar_paths)
    try:
        mixin_errors, mixin_warnings = audit_mixins(config, jars, args.strict)
    finally:
        jars.close()
    errors.extend(mixin_errors)
    warnings.extend(mixin_warnings)
    config_errors, config_warnings = audit_config_usage()
    errors.extend(config_errors)
    warnings.extend(config_warnings)
    errors.extend(audit_languages())
    errors.extend(audit_firstaid_shader_overrides())
    errors.extend(audit_versions(config))

    if args.upstream_root:
        compare_upstream(args.upstream_root)

    print(f"AUDIT: {len(errors)} error(s), {len(warnings)} warning(s)")
    for message in errors:
        print(f"ERROR: {message}")
    for message in warnings:
        print(f"WARN:  {message}")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
