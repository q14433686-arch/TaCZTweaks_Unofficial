#!/usr/bin/env python3
"""Static audit for the Fabric 1.21.11 TaCZ Tweaks port.

The audit is intentionally stdlib-only so it can run before Gradle/CI. It focuses on
mixed named/intermediary remap safety and the easiest-to-miss regressions:

* mixin source <-> JSON registration drift;
* client/common misclassification, including @Environment(CLIENT) stripped targets;
* missing target methods and missing @At owner/name/descriptor references;
* disallowed lambda$... mixin targets on vanilla classes in the obfuscated branch;
* bundled MixinExtras lower than the mixin JSON minimum;
* README/BUILD release drift after version bumps;
* required support/publication files and version-free long-lived release copy;
* mod-icon content, dimensions, metadata path, and license-provenance drift;
* upstream source omissions without an allowlisted explanation.
"""

from __future__ import annotations

import argparse
import fnmatch
import json
import re
import struct
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from check_mod_icon import validate_icon

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src/main"
MIXIN_JSON = SOURCE_ROOT / "resources/tacztweaks.mixins.json"
MIXIN_DIR = SOURCE_ROOT / "java/me/muksc/tacztweaks/mixin"
DEFAULT_TACZ_JAR = ROOT / "libs/TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R2.jar"
ALLOWLIST = ROOT / "scripts/upstream_omissions.json"

OPTIONAL_DEPENDENCIES: dict[str, dict[str, object]] = {
    "sound_physics_remastered": {
        "display": "Sound Physics Remastered",
        "range": ">=1.5.1 <1.6.0",
        "breaks": ["<1.5.1", ">=1.6.0"],
        "readme_markers": [">=1.5.1 <1.6.0", "fabric-1.21.11-1.5.1"],
        "build_markers": ["fabric-1.21.11-1.5.1"],
    },
    "firstaid": {
        "display": "First Aid New",
        "range": ">=1.2.5 <1.3.0",
        "breaks": ["<1.2.5", ">=1.3.0"],
        "readme_markers": [">=1.2.5 <1.3.0", "firstaid-1.2.5+fabric1.21.11-legacy.jar"],
        "build_markers": ["firstaid-1.2.5+fabric1.21.11-legacy.jar"],
    },
    "pillagers_gun": {
        "display": "Pillager’s Gun (Unofficial Port)",
        "range": ">=3.2.2 <3.3.0",
        "breaks": ["<3.2.2", ">=3.3.0"],
        "readme_markers": [">=3.2.2 <3.3.0", "pillagers_gun-3.2.2 fabric 1.21.11.jar"],
        "build_markers": ["pillagers_gun-3.2.2 fabric 1.21.11.jar"],
    },
}

KNOWN_DORMANT_OPTIONS: dict[str, str] = {
    "Compat.lsoCompat": "No verified 1.21.11 Fabric target is wired yet.",
    "Compat.mtsFix": "No verified 1.21.11 Fabric target is wired yet.",
    "Compat.vsCollisionCompat": "No verified 1.21.11 Fabric target is wired yet.",
    "Compat.vsExplosionCompat": "No verified 1.21.11 Fabric target is wired yet.",
}

KNOWN_SYNTHETIC_TARGET_EXCEPTIONS: dict[str, str] = {
    "me.muksc.tacztweaks.mixin.tweaks.SoundBufferLibraryMixin": (
        "1.21.11 source verification: SoundBufferLibrary decodes complete buffers inside "
        "lambda$getCompleteBuffer$1(Identifier) before constructing SoundBuffer. Keep this "
        "exception narrow and rely on strict jar verification for final confirmation."
    ),
}

INJECTOR_ANNOTATIONS = (
    "Inject|ModifyArg|ModifyArgs|ModifyVariable|Redirect|ModifyConstant|"
    "ModifyExpressionValue|ModifyReturnValue|WrapOperation|WrapMethod|WrapWithCondition"
)


@dataclass(frozen=True)
class ClassInfo:
    methods: frozenset[str]
    fields: frozenset[str]
    method_signatures: frozenset[tuple[str, str]]
    field_signatures: frozenset[tuple[str, str]]
    method_references: dict[tuple[str, str], frozenset[tuple[str, str, str]]]
    client_only_methods: frozenset[tuple[str, str]]
    client_only_class: bool
    super_name: str | None
    interfaces: tuple[str, ...]


@dataclass(frozen=True)
class SourceMixin:
    fqcn: str
    path: Path
    text: str
    target: str | None
    section: str | None


def _u1(data: memoryview, offset: int) -> tuple[int, int]:
    return data[offset], offset + 1


def _u2(data: memoryview, offset: int) -> tuple[int, int]:
    return struct.unpack_from(">H", data, offset)[0], offset + 2


def _u4(data: memoryview, offset: int) -> tuple[int, int]:
    return struct.unpack_from(">I", data, offset)[0], offset + 4


def read_class_info(raw: bytes) -> ClassInfo:
    data = memoryview(raw)
    magic, off = _u4(data, 0)
    if magic != 0xCAFEBABE:
        raise ValueError("not a class file")
    off += 4
    cp_count, off = _u2(data, off)
    utf8: dict[int, str] = {}
    class_name_indices: dict[int, int] = {}
    name_and_type_indices: dict[int, tuple[int, int]] = {}
    member_ref_indices: dict[int, tuple[int, int]] = {}
    index = 1
    while index < cp_count:
        tag, off = _u1(data, off)
        if tag == 1:
            length, off = _u2(data, off)
            utf8[index] = bytes(data[off: off + length]).decode("utf-8", "replace")
            off += length
        elif tag in (3, 4):
            off += 4
        elif tag in (5, 6):
            off += 8
            index += 1
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

    def has_client_environment_annotation(raw_annotations: memoryview) -> bool:
        cursor = 0

        def read_u2_local() -> int:
            nonlocal cursor
            value = struct.unpack_from(">H", raw_annotations, cursor)[0]
            cursor += 2
            return value

        def read_element_value() -> list[tuple[str, str]]:
            nonlocal cursor
            tag = chr(raw_annotations[cursor])
            cursor += 1
            if tag in "BCDFIJSZsc":
                cursor += 2
                return []
            if tag == "e":
                enum_type = utf8.get(read_u2_local(), "")
                enum_value = utf8.get(read_u2_local(), "")
                return [(enum_type, enum_value)]
            if tag == "@":
                return read_annotation()[1]
            if tag == "[":
                values: list[tuple[str, str]] = []
                for _ in range(read_u2_local()):
                    values.extend(read_element_value())
                return values
            raise ValueError(f"unknown annotation element tag {tag!r}")

        def read_annotation() -> tuple[str, list[tuple[str, str]]]:
            annotation_type = utf8.get(read_u2_local(), "")
            values: list[tuple[str, str]] = []
            for _ in range(read_u2_local()):
                read_u2_local()
                values.extend(read_element_value())
            return annotation_type, values

        try:
            for _ in range(read_u2_local()):
                annotation_type, values = read_annotation()
                if annotation_type == "Lnet/fabricmc/api/Environment;" and (
                    "Lnet/fabricmc/api/EnvType;",
                    "CLIENT",
                ) in values:
                    return True
        except (IndexError, struct.error, ValueError):
            return False
        return False

    off += 2
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
    client_only_methods: set[tuple[str, str]] = set()

    def members(offset: int, collect_code: bool = False) -> tuple[list[tuple[str, str]], int]:
        count, offset = _u2(data, offset)
        members_found: list[tuple[str, str]] = []
        for _ in range(count):
            offset += 2
            name_index, offset = _u2(data, offset)
            descriptor_index, offset = _u2(data, offset)
            signature = (utf8.get(name_index, ""), utf8.get(descriptor_index, ""))
            members_found.append(signature)
            refs: set[tuple[str, str, str]] = set()
            attribute_count, offset = _u2(data, offset)
            for _ in range(attribute_count):
                attribute_name_index, offset = _u2(data, offset)
                length, offset = _u4(data, offset)
                attribute_name = utf8.get(attribute_name_index)
                if collect_code and attribute_name == "Code" and length >= 8:
                    code_length = struct.unpack_from(">I", data, offset + 4)[0]
                    code = data[offset + 8: offset + 8 + code_length]
                    for pos in range(max(0, len(code) - 2)):
                        if code[pos] not in (0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9):
                            continue
                        cp_index = (code[pos + 1] << 8) | code[pos + 2]
                        resolved = resolve_member_ref(cp_index)
                        if resolved is not None:
                            refs.add(resolved)
                if collect_code and attribute_name in {"RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations"}:
                    if has_client_environment_annotation(data[offset: offset + length]):
                        client_only_methods.add(signature)
                offset += length
            if collect_code:
                method_references[signature] = frozenset(refs)
        return members_found, offset

    fields, off = members(off)
    methods, off = members(off, collect_code=True)
    client_only_class = False
    class_attribute_count, off = _u2(data, off)
    for _ in range(class_attribute_count):
        attribute_name_index, off = _u2(data, off)
        length, off = _u4(data, off)
        if utf8.get(attribute_name_index) in {"RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations"} and has_client_environment_annotation(data[off: off + length]):
            client_only_class = True
        off += length

    return ClassInfo(
        frozenset(name for name, _ in methods),
        frozenset(name for name, _ in fields),
        frozenset(methods),
        frozenset(fields),
        method_references,
        frozenset(client_only_methods),
        client_only_class,
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


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" not in line or line.lstrip().startswith("#"):
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def listed_mixins(config: dict) -> dict[str, str]:
    package = config["package"]
    result: dict[str, str] = {}
    for section in ("mixins", "client", "server"):
        for name in config.get(section, []):
            result[f"{package}.{name}"] = section
    return result


def resolve_target(text: str) -> str | None:
    target_string = re.search(r"@Mixin\s*\([^)]*?targets\s*=\s*\"([^\"]+)\"", text, re.S)
    if target_string:
        return target_string.group(1)
    target_type = re.search(r"@Mixin\s*\(\s*(?:value\s*=\s*)?([A-Za-z_$][\w$]*)\.class", text, re.S)
    if not target_type:
        return None
    simple = target_type.group(1)
    imported = re.search(rf"\bimport\s+([\w.$]+\.{re.escape(simple)})\s*;", text)
    if imported:
        return imported.group(1)
    package_match = re.search(r"\bpackage\s+([\w.]+)\s*;", text)
    return f"{package_match.group(1)}.{simple}" if package_match else simple


def source_mixins(registry: dict[str, str]) -> list[SourceMixin]:
    result: list[SourceMixin] = []
    for path in sorted(MIXIN_DIR.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        if "@Mixin" not in text:
            continue
        package_match = re.search(r"\bpackage\s+([\w.]+)\s*;", text)
        type_match = re.search(r"^(?:public\s+)?(?:abstract\s+)?(?:final\s+)?(?:class|interface)\s+([\w$]+)", text, re.M)
        if not package_match or not type_match:
            continue
        fqcn = f"{package_match.group(1)}.{type_match.group(1)}"
        result.append(SourceMixin(fqcn, path, text, resolve_target(text), registry.get(fqcn)))
    return result


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
                return text[open_paren + 1:index]
    return None


def _string_constants(text: str) -> dict[str, str]:
    return dict(re.findall(r"\b(?:String|private\s+static\s+final\s+String)\s+(\w+)\s*=\s*\"([^\"]+)\"", text))


def mixin_declares_remap_false(text: str) -> bool:
    match = re.search(r"@Mixin\s*\((.*?)\)", text, re.S)
    return bool(match and "remap = false" in match.group(1))


def iterate_annotation_bodies(text: str, names: str) -> list[str]:
    bodies: list[str] = []
    for annotation in re.finditer(rf"@({names})\s*\(", text):
        body = _annotation_body(text, text.index("(", annotation.start()))
        if body is not None:
            bodies.append(body)
    return bodies


def resolve_annotation_value(raw: str, constants: dict[str, str]) -> str | None:
    raw = raw.strip()
    if raw.startswith('"') and raw.endswith('"'):
        return raw[1:-1]
    return constants.get(raw)


def injection_method_targets(text: str) -> set[tuple[str, str | None]]:
    targets: set[tuple[str, str | None]] = set()
    constants = _string_constants(text)
    for annotation in re.finditer(rf"@({INJECTOR_ANNOTATIONS})\s*\(", text):
        body = _annotation_body(text, text.index("(", annotation.start()))
        if body is None:
            continue
        assignment = re.search(r"\bmethod\s*=\s*(\{[^}]*\}|\"[^\"]+\"|\w+)", body, re.S)
        if assignment is None:
            continue
        value = assignment.group(1)
        raw_values = re.findall(r'\"([^\"]+)\"', value)
        if not raw_values and value in constants:
            raw_values = [constants[value]]
        for raw in raw_values:
            name = raw.split("(", 1)[0]
            descriptor = "(" + raw.split("(", 1)[1] if "(" in raw else None
            if name:
                targets.add((name, descriptor))
    return targets


def parse_at_targets(text: str) -> list[str]:
    constants = _string_constants(text)
    result: list[str] = []
    for body in iterate_annotation_bodies(text, INJECTOR_ANNOTATIONS + "|At"):
        for target_match in re.finditer(r"\btarget\s*=\s*(\"[^\"]+\"|\w+)", body):
            resolved = resolve_annotation_value(target_match.group(1), constants)
            if resolved:
                result.append(resolved)
    return result


def audit_vanilla_remap_safety(source: SourceMixin) -> list[str]:
    if not mixin_declares_remap_false(source.text):
        return []
    errors: list[str] = []
    constants = _string_constants(source.text)

    for body in iterate_annotation_bodies(source.text, "At"):
        for target_match in re.finditer(r"\btarget\s*=\s*(\"[^\"]+\"|\w+)", body):
            resolved = resolve_annotation_value(target_match.group(1), constants)
            if resolved and "Lnet/minecraft/" in resolved and "remap = true" not in body and "remap=true" not in body:
                errors.append(
                    f"remap=false mixin references named vanilla member without explicit remap=true: {source.path.relative_to(ROOT)} -> {resolved}"
                )

    for body in iterate_annotation_bodies(source.text, INJECTOR_ANNOTATIONS):
        method_match = re.search(r"\bmethod\s*=\s*(\{[^}]*\}|\"[^\"]+\"|\w+)", body, re.S)
        if method_match is None:
            continue
        raw_values = re.findall(r'\"([^\"]+)\"', method_match.group(1))
        if not raw_values:
            resolved = resolve_annotation_value(method_match.group(1), constants)
            raw_values = [resolved] if resolved else []
        for raw in raw_values:
            if "Lnet/minecraft/" in raw and "class_" not in raw and "remap = true" not in body and "remap=true" not in body:
                errors.append(
                    f"remap=false mixin uses named vanilla descriptor in method=: {source.path.relative_to(ROOT)} -> {raw}"
                )
    return errors


def is_client_target(target: str | None) -> bool:
    return bool(target) and (
        target.startswith("net.minecraft.client.")
        or ".client." in target
    )


def choose_jar(target: str, named: JarIndex, intermediary: JarIndex, tacz: JarIndex) -> JarIndex:
    if target.startswith("com.tacz.") or target.startswith("me.xjqsh."):
        return tacz
    if ".class_" in target or re.search(r"\.method_\d+$", target):
        return intermediary
    return named


def exact_methods(info: ClassInfo, name: str, desc: str | None) -> list[tuple[str, str]]:
    return [
        signature for signature in info.method_signatures
        if signature[0] == name and (desc is None or signature[1] == desc)
    ]


def config_options() -> set[str]:
    text = (SOURCE_ROOT / "kotlin/me/muksc/tacztweaks/config/Config.kt").read_text(encoding="utf-8")
    options: set[str] = set()
    for match in re.finditer(r"^    object (Gun|Crawl|Compat|Tweaks|Debug)\b[^\n]*\{", text, re.M):
        group = match.group(1)
        depth = 1
        cursor = match.end()
        while cursor < len(text) and depth:
            if text[cursor] == "{":
                depth += 1
            elif text[cursor] == "}":
                depth -= 1
            cursor += 1
        block = text[match.end(): cursor - 1]
        for func in re.finditer(r"^        fun\s+(\w+)\(\):", block, re.M):
            options.add(f"{group}.{func.group(1)}")
    return options


def audit_config_usage() -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    texts: list[str] = []
    for path in SOURCE_ROOT.rglob("*"):
        if not path.is_file() or path.suffix not in {".java", ".kt"} or path.name == "Config.kt":
            continue
        texts.append(path.read_text(encoding="utf-8", errors="replace"))
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
            warnings.append(f"dormant config option {option}: {reason}")
        else:
            errors.append(f"config option has no behaviour reader outside Config.kt: {option}")
    return errors, warnings


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
            errors.append(f"{name} has {len(extra)} extra keys absent from en_us: {', '.join(sorted(extra))}")
    return errors


def audit_test_and_fixture_guards() -> list[str]:
    errors: list[str] = []
    required_tests = {
        "src/test/kotlin/me/muksc/tacztweaks/core/SafeMathTest.kt",
        "src/test/kotlin/me/muksc/tacztweaks/core/StackSplitterTest.kt",
        "src/test/kotlin/me/muksc/tacztweaks/core/ProjectileIndexAllocatorTest.kt",
        "src/test/kotlin/me/muksc/tacztweaks/data/CodecSmokeTest.kt",
    }
    for name in sorted(required_tests):
        if not (ROOT / name).is_file():
            errors.append(f"missing regression test: {name}")

    required_fixtures = {
        "src/test/resources/fixtures/bullet_interaction_v2.json",
        "src/test/resources/fixtures/schema_smoke.json",
        "src/test/resources/fixtures/airspace.json",
        "tacz-tweaks-example-pack/data/tacztweaks/bullet_interactions/schema_smoke.json",
        "tacz-tweaks-example-pack/data/tacztweaks/bullet_sounds/airspace.json",
        "THIRD_PARTY_NOTICES.md",
    }
    for name in sorted(required_fixtures):
        if not (ROOT / name).is_file():
            errors.append(f"missing release artifact/fixture: {name}")

    pairs = (
        (
            ROOT / "src/test/resources/fixtures/schema_smoke.json",
            ROOT / "tacz-tweaks-example-pack/data/tacztweaks/bullet_interactions/schema_smoke.json",
        ),
        (
            ROOT / "src/test/resources/fixtures/airspace.json",
            ROOT / "tacz-tweaks-example-pack/data/tacztweaks/bullet_sounds/airspace.json",
        ),
    )
    for left, right in pairs:
        if left.is_file() and right.is_file() and left.read_bytes() != right.read_bytes():
            errors.append(f"fixture drift between {left.relative_to(ROOT)} and {right.relative_to(ROOT)}")
    return errors


def audit_tacz_metadata(path: Path) -> list[str]:
    if not path.is_file():
        return []
    errors: list[str] = []
    try:
        with zipfile.ZipFile(path) as archive:
            metadata = json.loads(archive.read("fabric.mod.json").decode("utf-8"))
    except Exception as exc:
        return [f"failed to read TaCZ jar metadata from {path}: {exc}"]
    if metadata.get("id") != "tacz":
        errors.append(f"TaCZ jar has unexpected mod id: {metadata.get('id')!r}")
    if metadata.get("version") != "1.1.8+fabric.1.21.11.R2":
        errors.append(f"TaCZ jar has unexpected version: {metadata.get('version')!r}")
    return errors


def audit_versions(config: dict) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    properties = read_properties(ROOT / "gradle.properties")
    required = config.get("mixinextras", {}).get("minVersion")
    bundled = properties.get("mixinextras_version")
    if required and bundled and version_tuple(bundled) < version_tuple(required):
        errors.append(f"MixinExtras {bundled} is lower than mixin JSON minimum {required}")

    mod_version = properties.get("mod_version", "")
    if not re.fullmatch(r"\d+\.\d+\.\d+\+fabric\.1\.21\.11\.(?:R\d+|Beta-\d+(?:-hotfix)?)", mod_version):
        errors.append(f"unexpected mod_version format: {mod_version}")

    readme_text = (ROOT / "README.md").read_text(encoding="utf-8")
    build_text = (ROOT / "BUILD.md").read_text(encoding="utf-8")
    for name, text in (("README.md", readme_text), ("BUILD.md", build_text)):
        if mod_version and mod_version not in text:
            errors.append(f"{name} does not mention current version {mod_version}")

    wrapper = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8")
    expected_gradle_sum = "bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f"
    if f"distributionSha256Sum={expected_gradle_sum}" not in wrapper:
        errors.append("Gradle 9.5.1 distribution checksum is missing or incorrect")

    mod_json = json.loads((SOURCE_ROOT / "resources/fabric.mod.json").read_text(encoding="utf-8"))
    if mod_json.get("depends", {}).get("tacz") != "=1.1.8+fabric.1.21.11.R2":
        errors.append(
            "fabric.mod.json must retain the current-branch TaCZ R2 compile/test baseline "
            "(runtime accepts 1.1.8+fabric.1.21.11.R<n>, n >= 2)"
        )

    suggests = mod_json.get("suggests", {})
    breaks = mod_json.get("breaks", {})
    for mod_id, spec in OPTIONAL_DEPENDENCIES.items():
        expected_range = spec["range"]
        expected_breaks = spec["breaks"]
        display = spec["display"]
        if suggests.get(mod_id) != expected_range:
            errors.append(
                f"fabric.mod.json suggests[{mod_id!r}] drifted from verified {display} range {expected_range}"
            )
        if breaks.get(mod_id) != expected_breaks:
            errors.append(
                f"fabric.mod.json breaks[{mod_id!r}] drifted from verified {display} guard rails {expected_breaks}"
            )
        for marker in spec["readme_markers"]:
            if marker not in readme_text:
                errors.append(f"README.md is missing verified {display} marker: {marker}")
        for marker in spec["build_markers"]:
            if marker not in build_text:
                errors.append(f"BUILD.md is missing verified {display} marker: {marker}")

    if "--refmap build/resources/main/tacztweaks.refmap.json" not in readme_text:
        errors.append("README.md strict audit example must mention the generated refmap path")
    if "--refmap build/resources/main/tacztweaks.refmap.json" not in build_text:
        errors.append("BUILD.md strict audit example must mention the generated refmap path")
    if "suggests` / `breaks`" not in readme_text:
        warnings.append("README.md does not explicitly note that optional compat ranges are synced with fabric.mod.json suggests/breaks")
    if not (ROOT / "scripts/check_server_log.py").is_file():
        errors.append("missing dedicated-server log gate")
    return errors, warnings


def audit_release_guards() -> list[str]:
    """Require support workflow files and stable, version-free publication copy."""
    errors: list[str] = []
    required_files = {
        ".github/ISSUE_TEMPLATE/bug_report.yml",
        ".github/ISSUE_TEMPLATE/compat_report.yml",
        ".github/ISSUE_TEMPLATE/config.yml",
        "CHANGELOG.md",
        "CONTRIBUTING.md",
        "LICENSES.md",
        "SECURITY.md",
        "CODE_OF_CONDUCT.md",
        "RESOURCE_IMPORT_MANIFEST.tsv",
        "docs/README.md",
        "docs/SUPPORT.md",
        "docs/CONFIGURATION.md",
        "docs/COMPATIBILITY.md",
        "docs/KNOWN_ISSUES.md",
        "docs/data/README.md",
        "docs/data/SELECTORS.md",
        "docs/data/BULLET_INTERACTIONS.md",
        "docs/data/BULLET_SOUNDS.md",
        "docs/data/BULLET_PARTICLES.md",
        "docs/data/MELEE_INTERACTIONS.md",
        "docs/data/MIGRATION.md",
        "docs/publish/README.md",
        "docs/publish/Modrinth.md",
        "docs/publish/CurseForge.md",
        "docs/maintenance/ci-workflow.yml",
        "scripts/download_dependencies.py",
        "scripts/check_release_consistency.py",
    }
    for name in sorted(required_files):
        if not (ROOT / name).is_file():
            errors.append(f"missing support or publication document: {name}")

    publish_paths = [
        ROOT / name
        for name in sorted(required_files)
        if name.startswith("docs/publish/") and (ROOT / name).is_file()
    ]
    publication_text = "\n".join(
        path.read_text(encoding="utf-8") for path in publish_paths
    )
    properties = read_properties(ROOT / "gradle.properties")
    for key in ("minecraft_version", "mod_version"):
        value = properties.get(key)
        if value and value in publication_text:
            errors.append(f"publication copy must not embed current {key}: {value}")

    numbered_stage = re.compile(
        r"(?ix)\b(?:"
        r"alpha\s*[-_.]?\s*\d+|"
        r"beta\s*[-_.]?\s*\d+|"
        r"release\s*[-_ ]?\s*candidate\s*[-_.]?\s*\d+|"
        r"rc\s*[-_.]?\s*\d+"
        r")\b"
    )
    if numbered_stage.search(publication_text):
        errors.append("publication copy must not embed a numbered release stage")
    return errors


def audit_mixins(
    config: dict,
    named: JarIndex,
    intermediary: JarIndex,
    tacz: JarIndex,
    refmap: dict | None,
    strict: bool,
) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    registry = listed_mixins(config)
    sources = source_mixins(registry)
    source_names = {source.fqcn for source in sources}

    for fqcn in sorted(set(registry) - source_names):
        errors.append(f"mixin JSON lists a source class that does not exist: {fqcn}")
    for fqcn in sorted(source_names - set(registry)):
        errors.append(f"mixin source is not registered in tacztweaks.mixins.json: {fqcn}")

    for source in sources:
        if "require = 0" in source.text or "require=0" in source.text:
            errors.append(f"require=0 is forbidden: {source.path.relative_to(ROOT)}")
        errors.extend(audit_vanilla_remap_safety(source))
        if source.section == "mixins" and is_client_target(source.target):
            errors.append(f"client-only target is listed in common mixins: {source.fqcn} -> {source.target}")
        if source.target is None:
            warnings.append(f"could not resolve @Mixin target for {source.path.relative_to(ROOT)}")
            continue
        synthetic_exception = KNOWN_SYNTHETIC_TARGET_EXCEPTIONS.get(source.fqcn)
        if source.target.startswith("net.minecraft.") and "lambda$" in source.text and synthetic_exception is None:
            errors.append(f"vanilla mixin uses lambda$ synthetic names: {source.path.relative_to(ROOT)}")
        elif synthetic_exception is not None:
            warnings.append(f"synthetic target exception in use: {source.path.relative_to(ROOT)} — {synthetic_exception}")

        jar = choose_jar(source.target, named, intermediary, tacz)
        info = jar.class_info(source.target)
        if info is None:
            if not jar.paths:
                warnings.append(
                    f"target class not checked because its jar was not supplied: {source.target} ({source.path.relative_to(ROOT)})"
                )
            else:
                message = f"target class not found in supplied jars: {source.target} ({source.path.relative_to(ROOT)})"
                (errors if strict else warnings).append(message)
            continue
        if source.section == "mixins" and info.client_only_class:
            errors.append(f"common mixin targets an @Environment(CLIENT) class: {source.fqcn} -> {source.target}")

        for method_name, method_descriptor in sorted(injection_method_targets(source.text)):
            exact = exact_methods(info, method_name, method_descriptor)
            if not exact:
                message = f"target method absent: {source.target}#{method_name}{method_descriptor or ''} ({source.path.relative_to(ROOT)})"
                (errors if strict else warnings).append(message)
                continue
            if source.section == "mixins":
                client_matches = {
                    signature for signature in info.client_only_methods
                    if signature[0] == method_name and (method_descriptor is None or signature[1] == method_descriptor)
                }
                if client_matches:
                    rendered = ", ".join(name + descriptor for name, descriptor in sorted(client_matches))
                    errors.append(
                        f"common mixin injects @Environment(CLIENT) target method(s) {source.target}#{rendered}"
                    )

        for at_target in parse_at_targets(source.text):
            method_match = re.fullmatch(r"L([^;]+);([^(:]+)(\(.*)", at_target)
            if method_match is not None:
                owner, name, descriptor = method_match.groups()
                owner_name = owner.replace("/", ".")
                owner_jar = choose_jar(owner_name, named, intermediary, tacz)
                owner_info = owner_jar.class_info(owner_name)
                if owner_info is None:
                    if not owner_jar.paths:
                        warnings.append(
                            f"@At owner class not checked because its jar was not supplied: {owner_name} ({source.path.relative_to(ROOT)})"
                        )
                    else:
                        message = f"@At owner class absent: {owner_name} ({source.path.relative_to(ROOT)})"
                        (errors if strict else warnings).append(message)
                    continue
                if not owner_jar.has_method(owner_name, (name, descriptor)):
                    message = f"@At method reference absent: {owner_name}#{name}{descriptor} ({source.path.relative_to(ROOT)})"
                    (errors if strict else warnings).append(message)
                if info.method_references and not any((owner, name, descriptor) in refs for refs in info.method_references.values()):
                    message = f"target bytecode does not contain @At reference {at_target} ({source.path.relative_to(ROOT)})"
                    (errors if strict else warnings).append(message)
                continue
            field_match = re.fullmatch(r"L([^;]+);([^:]+):(.+)", at_target)
            if field_match is not None:
                owner, name, descriptor = field_match.groups()
                owner_name = owner.replace("/", ".")
                owner_jar = choose_jar(owner_name, named, intermediary, tacz)
                owner_info = owner_jar.class_info(owner_name)
                if owner_info is None:
                    if not owner_jar.paths:
                        warnings.append(
                            f"@At owner class not checked because its jar was not supplied: {owner_name} ({source.path.relative_to(ROOT)})"
                        )
                    else:
                        message = f"@At owner class absent: {owner_name} ({source.path.relative_to(ROOT)})"
                        (errors if strict else warnings).append(message)
                    continue
                if not owner_jar.has_field(owner_name, (name, descriptor)):
                    message = f"@At field reference absent: {owner_name}#{name}:{descriptor} ({source.path.relative_to(ROOT)})"
                    (errors if strict else warnings).append(message)
                if info.method_references and not any((owner, name, descriptor) in refs for refs in info.method_references.values()):
                    message = f"target bytecode does not contain @At reference {at_target} ({source.path.relative_to(ROOT)})"
                    (errors if strict else warnings).append(message)

        if refmap is not None and source.target.startswith("net.minecraft."):
            mixin_key = source.fqcn.replace(config["package"] + ".", "")
            mappings = refmap.get("mappings", {}).get(source.fqcn) or refmap.get("data", {}).get(mixin_key)
            if not mappings:
                warnings.append(f"refmap has no explicit entry for vanilla mixin {source.fqcn}")
    return errors, warnings


def compare_upstream(path: Path) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    upstream_root = path / "src/main"
    if not upstream_root.is_dir():
        return [f"{upstream_root} is not an upstream source tree"], warnings
    upstream = {
        str(p.relative_to(upstream_root)) for p in upstream_root.rglob("*")
        if p.is_file() and p.suffix in {".java", ".kt"}
    }
    current = {
        str(p.relative_to(SOURCE_ROOT)) for p in SOURCE_ROOT.rglob("*")
        if p.is_file() and p.suffix in {".java", ".kt"}
    }
    omitted = sorted(upstream - current)
    rules = json.loads(ALLOWLIST.read_text(encoding="utf-8")) if ALLOWLIST.is_file() else []
    matched_rules: set[int] = set()
    print(f"UPSTREAM: {len(upstream)} code files; port: {len(current)} code files; direct-path omissions: {len(omitted)}")
    for name in omitted:
        matches = [
            (index, rule) for index, rule in enumerate(rules)
            if fnmatch.fnmatchcase(name, rule.get("glob", ""))
        ]
        if not matches:
            errors.append(f"unexplained upstream source omission: {name}")
            continue
        matched_rules.update(index for index, _ in matches)
    for index, rule in enumerate(rules):
        if index not in matched_rules:
            warnings.append(f"stale upstream omission rule matches nothing: {rule.get('glob')}")
    return errors, warnings


def discover_refmap() -> Path | None:
    candidates = [
        ROOT / "build/resources/main/tacztweaks.refmap.json",
        ROOT / "build/classes/java/main/tacztweaks.refmap.json",
        ROOT / "build/classes/kotlin/main/tacztweaks.refmap.json",
    ]
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    return None


def load_refmap(path: Path | None) -> dict | None:
    if path is None or not path.is_file():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--strict", action="store_true", help="fail on target-class/method mismatches")
    parser.add_argument("--tacz-jar", type=Path, default=DEFAULT_TACZ_JAR)
    parser.add_argument("--minecraft-named-jar", type=Path)
    parser.add_argument("--minecraft-intermediary-jar", type=Path)
    parser.add_argument("--refmap", type=Path, help="generated tacztweaks.refmap.json")
    parser.add_argument("--upstream-root", type=Path, help="optional TaCZTweaks v2.14.2 checkout")
    args = parser.parse_args()

    config = json.loads(MIXIN_JSON.read_text(encoding="utf-8"))
    refmap_path = args.refmap if args.refmap is not None else discover_refmap()
    refmap = load_refmap(refmap_path)
    errors: list[str] = []
    warnings: list[str] = []

    if not args.tacz_jar.is_file():
        errors.append(f"missing TaCZ jar: {args.tacz_jar}")
    if args.strict and args.minecraft_named_jar is None:
        errors.append("strict audit requires --minecraft-named-jar for vanilla named target verification")
    if args.strict and args.minecraft_intermediary_jar is None:
        errors.append("strict audit requires --minecraft-intermediary-jar for obfuscated runtime verification")
    if args.strict and refmap_path is None:
        errors.append("strict audit requires a generated tacztweaks.refmap.json (pass --refmap or build first)")

    named = JarIndex([args.minecraft_named_jar] if args.minecraft_named_jar else [])
    intermediary = JarIndex([args.minecraft_intermediary_jar] if args.minecraft_intermediary_jar else [])
    tacz = JarIndex([args.tacz_jar])
    try:
        mixin_errors, mixin_warnings = audit_mixins(config, named, intermediary, tacz, refmap, args.strict)
    finally:
        named.close()
        intermediary.close()
        tacz.close()
    errors.extend(mixin_errors)
    warnings.extend(mixin_warnings)

    config_errors, config_warnings = audit_config_usage()
    errors.extend(config_errors)
    warnings.extend(config_warnings)

    errors.extend(audit_languages())
    errors.extend(audit_test_and_fixture_guards())
    errors.extend(audit_tacz_metadata(args.tacz_jar))

    version_errors, version_warnings = audit_versions(config)
    errors.extend(version_errors)
    warnings.extend(version_warnings)

    errors.extend(audit_release_guards())
    errors.extend(validate_icon())

    if args.upstream_root:
        upstream_errors, upstream_warnings = compare_upstream(args.upstream_root)
        errors.extend(upstream_errors)
        warnings.extend(upstream_warnings)

    print(f"AUDIT: {len(errors)} error(s), {len(warnings)} warning(s)")
    for message in errors:
        print(f"ERROR: {message}")
    for message in warnings:
        print(f"WARN:  {message}")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
