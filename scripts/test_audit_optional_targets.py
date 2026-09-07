#!/usr/bin/env python3
"""Simulated `--minecraft-jar` gate check for optional (`remap = false`) @At targets.

The shield-durability mixin wraps two `BlocksAttacks#hurtBlockingItem` call-site shapes:
the vanilla 26.1.2 5-parameter shape and a 6-parameter shape carrying an extra trailing
`int fixedDamage` (patched builds, e.g. the NeoForge 26.1.x line). Exactly one shape
exists per build, so the 6-parameter injector declares `require = 0, remap = false` and
the audit must report its absence as a *marked warning* instead of an error.

This script builds minimal synthetic jars (stdlib-only class-file writer) that simulate
a vanilla 26.1.2 build — every validated mixin target/method exists, every non-optional
@At reference resolves, and only the 6-parameter shape is absent — then runs the real
`audit_port.py --strict` gate against them:

* self-contained run (always): synthetic TaCZ jar + synthetic Minecraft jar;
* real-jar run (when `libs/` holds the exact TaCZ R2 artifact): real TaCZ jar +
  synthetic Minecraft jar with vanilla stubs only.

Expected result in both runs: 0 errors, exactly 1 warning carrying the
`[remap=false: optional call-site variant]` marker for the 6-parameter target.

Exit status is 0 on success, 1 on any mismatch.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))
import audit_port as audit

ROOT = audit.ROOT
SHIELD_MIXIN = (
    audit.MIXIN_ROOT / "features/bullet_interactions/LivingEntityMixin.java"
)
BLOCKS_ATTACKS = "net.minecraft.world.item.component.BlocksAttacks"
SIX_PARAM_DESCRIPTOR = (
    "(Lnet/minecraft/world/level/Level;"
    "Lnet/minecraft/world/item/ItemStack;"
    "Lnet/minecraft/world/entity/LivingEntity;"
    "Lnet/minecraft/world/InteractionHand;FI)V"
)
FIVE_PARAM_DESCRIPTOR = (
    "(Lnet/minecraft/world/level/Level;"
    "Lnet/minecraft/world/item/ItemStack;"
    "Lnet/minecraft/world/entity/LivingEntity;"
    "Lnet/minecraft/world/InteractionHand;F)V"
)
RESOLVE_DESCRIPTOR = (
    "(Lnet/minecraft/world/damagesource/DamageSource;FD)F"
)

FAILURES: list[str] = []


def fail(message: str) -> None:
    FAILURES.append(message)
    print(f"FAIL: {message}")


# ---------------------------------------------------------------------------
# Minimal class-file writer (just enough for audit_port.read_class_info).
# ---------------------------------------------------------------------------

class ClassWriter:
    """Build a Java 8 class file with stub methods/fields and optional refs."""

    def __init__(self, fqcn: str):
        self.internal_name = fqcn.replace(".", "/")
        self._entries: list[bytes] = []
        self._utf8: dict[str, int] = {}
        self._class: dict[str, int] = {}
        self._nat: dict[tuple[str, str], int] = {}
        self._memberref: dict[tuple[int, str, str, str], int] = {}

    # -- constant-pool allocation ----------------------------------------
    def _add(self, payload: bytes) -> int:
        self._entries.append(payload)
        return len(self._entries)

    def utf8(self, value: str) -> int:
        if value not in self._utf8:
            raw = value.encode("utf-8")
            self._utf8[value] = self._add(b"\x01" + len(raw).to_bytes(2, "big") + raw)
        return self._utf8[value]

    def klass(self, internal: str) -> int:
        if internal not in self._class:
            self._class[internal] = self._add(b"\x07" + self.utf8(internal).to_bytes(2, "big"))
        return self._class[internal]

    def nat(self, name: str, descriptor: str) -> int:
        key = (name, descriptor)
        if key not in self._nat:
            self._nat[key] = self._add(
                b"\x0c"
                + self.utf8(name).to_bytes(2, "big")
                + self.utf8(descriptor).to_bytes(2, "big")
            )
        return self._nat[key]

    def memberref(self, tag: int, owner: str, name: str, descriptor: str) -> int:
        key = (tag, owner, name, descriptor)
        if key not in self._memberref:
            self._memberref[key] = self._add(
                bytes([tag])
                + self.klass(owner).to_bytes(2, "big")
                + self.nat(name, descriptor).to_bytes(2, "big")
            )
        return self._memberref[key]

    # -- assembly ----------------------------------------------------------
    def build(
        self,
        methods: dict[tuple[str, str], dict],
        fields: set[tuple[str, str]],
    ) -> bytes:
        """Assemble the class.

        `methods` maps (name, descriptor) to {"refs": [(is_field, owner, name,
        descriptor), ...], "client_only": bool}.
        """
        this_index = self.klass(self.internal_name)
        super_index = self.klass("java/lang/Object")
        code_name = self.utf8("Code")
        annotations_name = self.utf8("RuntimeInvisibleAnnotations")
        environment_type = self.utf8("Lnet/fabricmc/api/Environment;")
        value_name = self.utf8("value")
        env_type_name = self.utf8("Lnet/fabricmc/api/EnvType;")
        client_name = self.utf8("CLIENT")

        field_infos = b"".join(
            b"\x00\x01"  # public
            + self.utf8(name).to_bytes(2, "big")
            + self.utf8(descriptor).to_bytes(2, "big")
            + b"\x00\x00"  # no attributes
            for name, descriptor in sorted(fields)
        )

        method_infos = []
        for (name, descriptor), spec in sorted(methods.items()):
            code = bytearray()
            for is_field, owner, ref_name, ref_desc in sorted(spec["refs"]):
                if is_field:
                    index = self.memberref(9, owner, ref_name, ref_desc)
                    code += b"\xb2" + index.to_bytes(2, "big")  # getstatic
                elif ref_name == "<init>":
                    index = self.memberref(10, owner, ref_name, ref_desc)
                    code += b"\xb7" + index.to_bytes(2, "big")  # invokespecial
                else:
                    index = self.memberref(10, owner, ref_name, ref_desc)
                    code += b"\xb6" + index.to_bytes(2, "big")  # invokevirtual
            code += b"\xb1"  # return
            code_attr = (
                code_name.to_bytes(2, "big")
                + (12 + len(code)).to_bytes(4, "big")
                + (len(spec["refs"]) + 2).to_bytes(2, "big")  # max_stack
                + b"\x00\x01"  # max_locals
                + len(code).to_bytes(4, "big")
                + bytes(code)
                + b"\x00\x00"  # exception table
                + b"\x00\x00"  # code attributes
            )
            attributes = [code_attr]
            if spec["client_only"]:
                annotation = (
                    environment_type.to_bytes(2, "big")
                    + b"\x00\x01"  # one element-value pair
                    + value_name.to_bytes(2, "big")
                    + b"e"
                    + env_type_name.to_bytes(2, "big")
                    + client_name.to_bytes(2, "big")
                )
                attributes.append(
                    annotations_name.to_bytes(2, "big")
                    + (2 + len(annotation)).to_bytes(4, "big")
                    + b"\x00\x01"  # one annotation
                    + annotation
                )
            method_infos.append(
                b"\x00\x01"  # public
                + self.utf8(name).to_bytes(2, "big")
                + self.utf8(descriptor).to_bytes(2, "big")
                + len(attributes).to_bytes(2, "big")
                + b"".join(attributes)
            )

        out = bytearray()
        out += b"\xca\xfe\xba\xbe"  # magic
        out += b"\x00\x00\x00\x34"  # Java 8
        out += (len(self._entries) + 1).to_bytes(2, "big")
        for entry in self._entries:
            out += entry
        out += b"\x00\x21"  # public + super
        out += this_index.to_bytes(2, "big")
        out += super_index.to_bytes(2, "big")
        out += b"\x00\x00"  # interfaces
        out += len(fields).to_bytes(2, "big")
        out += field_infos
        out += len(methods).to_bytes(2, "big")
        out += b"".join(method_infos)
        out += b"\x00\x00"  # class attributes
        return bytes(out)


# ---------------------------------------------------------------------------
# Stub planner: derive jar contents from the mixin sources with audit_port's
# own parsers, mirroring exactly which references the audit validates.
# ---------------------------------------------------------------------------

METHOD_TARGET_RE = re.compile(r'target\s*=\s*"L([^;]+);([^(:"]+)(\([^"]+)')
FIELD_TARGET_RE = re.compile(r'target\s*=\s*"L([^;]+);([^:"]+):([^"]+)')


def _parse_at_target(at_target: str) -> tuple[bool, str, str, str] | None:
    """Return (is_field, owner_slash, name, descriptor) like the audit does."""
    member = re.fullmatch(r"L([^;]+);([^(:]+)(\(.*)", at_target)
    if member is not None:
        owner, name, descriptor = member.groups()
        return (False, owner, name, descriptor)
    field = re.fullmatch(r"L([^;]+);([^:]+):(.+)", at_target)
    if field is None:
        return None
    owner, name, descriptor = field.groups()
    return (True, owner, name, descriptor)


def _injector_method_resolution(body: str, constants: dict[str, str]):
    """Mirror audit_injection_references' method resolution for one body."""
    method_match = re.search(r"\bmethod\s*=\s*(\"[^\"]+\"|\w+)", body)
    if method_match is None:
        return None
    method_value = method_match.group(1)
    method_raw = (
        method_value[1:-1]
        if method_value.startswith('"')
        else constants.get(method_value)
    )
    if not method_raw:
        return None
    name = method_raw.split("(", 1)[0]
    descriptor = "(" + method_raw.split("(", 1)[1] if "(" in method_raw else None
    return (name, descriptor)


def plan_stubs(vanilla_only: bool) -> dict[str, dict]:
    """Plan stub classes: every validated target plus BlocksAttacks.

    When `vanilla_only` is set, only `net.minecraft.*` targets are stubbed (for
    runs against the real TaCZ jar); otherwise every validated target is stubbed.
    """
    stubs: dict[str, dict] = {}

    def stub_for(fqcn: str) -> dict:
        return stubs.setdefault(fqcn, {"methods": {}, "fields": set()})

    def declare_method(fqcn: str, name: str, descriptor: str) -> dict:
        methods = stub_for(fqcn)["methods"]
        return methods.setdefault(
            (name, descriptor), {"refs": set(), "client_only": False}
        )

    # The simulated vanilla build carries BlocksAttacks with the 5-parameter
    # shape only; the 6-parameter shape must stay absent (it arrives solely via
    # the skipped remap=false injector, so nothing extra is needed here). Seed
    # before the passes so owner-side declarations can land on it.
    stub_for(BLOCKS_ATTACKS)

    # Pass 1: stub every validated target with all its injection methods, and
    # embed each non-optional @At reference into the methods the audit checks.
    for path in sorted(audit.MIXIN_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        if "@Mixin" not in text:
            continue
        target = audit.resolve_target(text)
        if target is None:
            continue
        if not target.startswith(("com.tacz.", "me.xjqsh.", "net.minecraft.")):
            continue
        if vanilla_only and not target.startswith("net.minecraft."):
            continue
        constants = audit._string_constants(text)
        for name, descriptor in audit.injection_method_targets(text):
            declare_method(target, name, descriptor or "()V")
        # Ensure the class exists even for accessor-only mixins.
        stub_for(target)
        for annotation in re.finditer(rf"@({audit.INJECTOR_ANNOTATIONS})\s*\(", text):
            body = audit._annotation_body(text, text.index("(", annotation.start()))
            if body is None:
                continue
            if re.search(r"\bremap\s*=\s*false\b", body):
                # Optional call-site variant: the simulated build carries the
                # other shape, so these references must stay absent.
                continue
            resolved = _injector_method_resolution(body, constants)
            if resolved is None:
                continue
            method_name, method_descriptor = resolved
            matches = [
                key for key in stub_for(target)["methods"]
                if key[0] == method_name
                and (method_descriptor is None or key[1] == method_descriptor)
            ]
            if not matches:
                continue  # The audit skips these references as well.
            for target_match in re.finditer(r"\btarget\s*=\s*(\"[^\"]+\"|\w+)", body):
                target_value = target_match.group(1)
                at_target = (
                    target_value[1:-1]
                    if target_value.startswith('"')
                    else constants.get(target_value)
                )
                if not at_target:
                    continue
                parsed = _parse_at_target(at_target)
                if parsed is None:
                    continue
                is_field, owner, name, descriptor = parsed
                for key in matches:
                    stub_for(target)["methods"][key]["refs"].add(
                        (is_field, owner, name, descriptor)
                    )

    # Pass 2: declare owner-side members for literal @At targets owned by
    # stubbed classes (mirrors the audit's owner-reference loops, which only
    # see literal `target = "L...` occurrences and skip remap=false injectors).
    for path in sorted(audit.MIXIN_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        if "@Mixin" not in text:
            continue
        target = audit.resolve_target(text)
        if target is None:
            continue
        if not target.startswith(("com.tacz.", "me.xjqsh.", "net.minecraft.")):
            continue
        if vanilla_only and not target.startswith("net.minecraft."):
            continue
        optional_spans = audit._optional_injector_spans(text)
        for target_match in METHOD_TARGET_RE.finditer(text):
            if audit._in_optional_span(optional_spans, target_match.start()):
                continue
            owner, name, descriptor = target_match.groups()
            owner_fqcn = owner.replace("/", ".")
            if owner_fqcn in stubs:
                declare_method(owner_fqcn, name, descriptor)
        for target_match in FIELD_TARGET_RE.finditer(text):
            if audit._in_optional_span(optional_spans, target_match.start()):
                continue
            owner, name, descriptor = target_match.groups()
            owner_fqcn = owner.replace("/", ".")
            if owner_fqcn in stubs:
                stub_for(owner_fqcn)["fields"].add((name, descriptor))

    # Pass 3: mark the dedicated-server-stripped diagram methods @Environment
    # (CLIENT) so the audit's annotation-parser check keeps passing. Declare
    # them when no injector names them (e.g. InaccuracyModifier's diagram mixin
    # only touches buildNormal/buildAim while the real class still carries the
    # stripped method).
    for target, expected in audit.DEDICATED_SERVER_STRIPPED_METHODS.items():
        if target not in stubs:
            continue
        for name in expected:
            if not any(key[0] == name for key in stubs[target]["methods"]):
                declare_method(target, name, "()V")
        for key, spec in stubs[target]["methods"].items():
            if key[0] in expected:
                spec["client_only"] = True
    return stubs


def write_jar(path: Path, stubs: dict[str, dict], extra: dict[str, bytes] | None = None) -> None:
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as jar:
        for fqcn, spec in sorted(stubs.items()):
            writer = ClassWriter(fqcn)
            jar.writestr(
                fqcn.replace(".", "/") + ".class",
                writer.build(spec["methods"], spec["fields"]),
            )
        for name, payload in (extra or {}).items():
            jar.writestr(name, payload)


# ---------------------------------------------------------------------------
# Fixture + mixin-contract assertions.
# ---------------------------------------------------------------------------

def check_shield_fixture(tacz_jar: Path, minecraft_jar: Path) -> None:
    """Prove the fixture really simulates a 5-parameter-only vanilla build."""
    jars = audit.JarIndex([tacz_jar, minecraft_jar])
    try:
        living = jars.class_info("net.minecraft.world.entity.LivingEntity")
        blocks = jars.class_info(BLOCKS_ATTACKS)
        if living is None or blocks is None:
            fail("fixture is missing LivingEntity or BlocksAttacks stubs")
            return
        apply = [s for s in living.method_signatures if s[0] == "applyItemBlocking"]
        if len(apply) != 1:
            fail(f"fixture LivingEntity must declare applyItemBlocking once, got {apply}")
            return
        refs = living.method_references[apply[0]]
        five = (
            "net/minecraft/world/item/component/BlocksAttacks",
            "hurtBlockingItem",
            FIVE_PARAM_DESCRIPTOR,
        )
        six = (
            "net/minecraft/world/item/component/BlocksAttacks",
            "hurtBlockingItem",
            SIX_PARAM_DESCRIPTOR,
        )
        resolve = (
            "net/minecraft/world/item/component/BlocksAttacks",
            "resolveBlockedDamage",
            RESOLVE_DESCRIPTOR,
        )
        if resolve not in refs:
            fail("fixture applyItemBlocking is missing the resolveBlockedDamage call-site")
        if five not in refs:
            fail("fixture applyItemBlocking is missing the 5-parameter hurtBlockingItem call-site")
        if six in refs:
            fail("fixture must NOT contain the 6-parameter hurtBlockingItem call-site")
        if ("hurtBlockingItem", FIVE_PARAM_DESCRIPTOR) not in blocks.method_signatures:
            fail("fixture BlocksAttacks is missing the 5-parameter hurtBlockingItem method")
        if ("hurtBlockingItem", SIX_PARAM_DESCRIPTOR) in blocks.method_signatures:
            fail("fixture BlocksAttacks must NOT declare the 6-parameter hurtBlockingItem method")
        if ("resolveBlockedDamage", RESOLVE_DESCRIPTOR) not in blocks.method_signatures:
            fail("fixture BlocksAttacks is missing resolveBlockedDamage")
    finally:
        jars.close()


def check_shield_mixin_contract() -> None:
    """Statically verify the port's mixin-side contract (spec items 1-3, 5)."""
    text = SHIELD_MIXIN.read_text(encoding="utf-8")
    # Collect injector bodies keyed by the hurtBlockingItem shape they wrap,
    # plus the resolveBlockedDamage wrap for the negative remap check.
    bodies: dict[str, str] = {}
    for annotation in re.finditer(rf"@({audit.INJECTOR_ANNOTATIONS})\s*\(", text):
        body = audit._annotation_body(text, text.index("(", annotation.start()))
        if body is None or "hurtBlockingItem" not in body and "resolveBlockedDamage" not in body:
            continue
        if "resolveBlockedDamage" in body:
            bodies["resolve"] = body
        if FIVE_PARAM_DESCRIPTOR in body:
            bodies["five"] = body
        if SIX_PARAM_DESCRIPTOR in body:
            bodies["six"] = body
    for key in ("resolve", "five", "six"):
        if key not in bodies:
            fail(f"shield mixin is missing the {key} wrap")
            return
    for key in ("five", "six"):
        if not re.search(r"\brequire\s*=\s*0\b", bodies[key]):
            fail(f"shield mixin {key}-parameter wrap must declare require = 0")
    if re.search(r"\bremap\s*=\s*false\b", bodies["six"]) is None:
        fail("shield mixin 6-parameter wrap must declare remap = false")
    for key in ("resolve", "five"):
        if re.search(r"\bremap\s*=\s*false\b", bodies[key]):
            fail(f"shield mixin {key} wrap must keep default remap (no remap = false)")

    # original.call arity per handler: receiver + parameters (the
    # IncorrectArgumentCountException mechanism). Mismatches crash the block.
    handlers = [
        (m.group(1), m.start())
        for m in re.finditer(
            r"private (?:float|void) (tacztweaks\$applyItemBlocking\$\w+)\(", text
        )
    ]
    handlers.append(("@end", len(text)))
    expected = {
        "tacztweaks$applyItemBlocking$customDamage": [4],
        "tacztweaks$applyItemBlocking$customDurabilityVanilla": [6],
        "tacztweaks$applyItemBlocking$customDurabilityFixedDamage": [7, 7],
    }
    for (name, start), (_, end) in zip(handlers, handlers[1:]):
        if name not in expected:
            continue
        arities = []
        for call in re.finditer(r"original\.call\(", text[start:end]):
            depth = 0
            commas = 0
            for char in text[start + call.end():end]:
                if char == "(":
                    depth += 1
                elif char == ")":
                    if depth == 0:
                        break
                    depth -= 1
                elif char == "," and depth == 0:
                    commas += 1
            arities.append(commas + 1)
        if sorted(arities) != sorted(expected[name]):
            fail(f"{name} original.call arities {arities} != expected {expected[name]}")

    for identifier in (
        "tacztweaks$shieldDurabilityApplied",
        "tacztweaks$warnedMissingDurabilityHook",
        "tacztweaks$applyShieldDisable",
        "TaCZTweaks.LOGGER.warn",
    ):
        if identifier not in text:
            fail(f"shield mixin is missing {identifier}")


# ---------------------------------------------------------------------------
# Gate runs.
# ---------------------------------------------------------------------------

def run_audit_gate(tacz_jar: Path, minecraft_jar: Path) -> tuple[int, str]:
    proc = subprocess.run(
        [
            sys.executable,
            str(SCRIPTS_DIR / "audit_port.py"),
            "--strict",
            "--tacz-jar",
            str(tacz_jar),
            "--minecraft-jar",
            str(minecraft_jar),
        ],
        cwd=ROOT,
        capture_output=True,
        text=True,
        timeout=300,
    )
    return proc.returncode, proc.stdout + proc.stderr


def check_gate_output(label: str, returncode: int, output: str) -> None:
    if returncode != 0:
        fail(f"{label}: audit gate exited {returncode}\n{output}")
        return
    if "AUDIT: 0 error(s), 1 warning(s)" not in output:
        fail(f"{label}: expected 'AUDIT: 0 error(s), 1 warning(s)'\n{output}")
        return
    warnings = [line for line in output.splitlines() if line.startswith("WARN:")]
    if len(warnings) != 1:
        fail(f"{label}: expected exactly 1 warning, got {len(warnings)}\n{output}")
        return
    warning = warnings[0]
    if audit.OPTIONAL_CALL_SITE_MARKER not in warning:
        fail(f"{label}: warning is missing the optional-target marker\n{output}")
    if "hurtBlockingItem" not in warning or "FI)V" not in warning:
        fail(f"{label}: warning does not name the 6-parameter hurtBlockingItem target\n{output}")


def self_contained_run(tmp: Path) -> None:
    print("== self-contained run (synthetic TaCZ + Minecraft jars) ==")
    stubs = plan_stubs(vanilla_only=False)
    print(f"planned {len(stubs)} stub classes")
    tacz_jar = tmp / "TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar"
    minecraft_jar = tmp / "fake-minecraft-26.1.2.jar"
    tacz_stubs = {
        name: spec for name, spec in stubs.items()
        if not name.startswith("net.minecraft.")
    }
    minecraft_stubs = {
        name: spec for name, spec in stubs.items()
        if name.startswith("net.minecraft.")
    }
    fabric_mod = json.dumps(
        {"schemaVersion": 1, "id": "tacz", "version": "1.1.8+fabric.26.1.2.R2"}
    ).encode("utf-8")
    write_jar(tacz_jar, tacz_stubs, {"fabric.mod.json": fabric_mod})
    write_jar(minecraft_jar, minecraft_stubs)
    check_shield_fixture(tacz_jar, minecraft_jar)
    returncode, output = run_audit_gate(tacz_jar, minecraft_jar)
    print(output.strip().splitlines()[0] if output.strip() else "(no output)")
    check_gate_output("self-contained run", returncode, output)


def real_jar_run(tmp: Path) -> None:
    print("== real-jar run (real TaCZ R2 + synthetic Minecraft jar) ==")
    real_tacz = audit.TA_CZ_JAR
    if not real_tacz.is_file() or audit.audit_tacz_artifact(real_tacz):
        print(f"SKIP: exact TaCZ R2 artifact is absent from {real_tacz}")
        return
    stubs = plan_stubs(vanilla_only=True)
    minecraft_jar = tmp / "fake-minecraft-26.1.2.jar"
    write_jar(minecraft_jar, stubs)
    check_shield_fixture(real_tacz, minecraft_jar)
    returncode, output = run_audit_gate(real_tacz, minecraft_jar)
    print(output.strip().splitlines()[0] if output.strip() else "(no output)")
    check_gate_output("real-jar run", returncode, output)


def main() -> int:
    check_shield_mixin_contract()
    with tempfile.TemporaryDirectory(prefix="tacztweaks-optional-targets-") as raw:
        tmp = Path(raw)
        self_contained_run(tmp)
        real_jar_run(tmp)
    if FAILURES:
        print(f"RESULT: {len(FAILURES)} failure(s)")
        return 1
    print("RESULT: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
