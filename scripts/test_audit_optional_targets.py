"""Simulated --minecraft-jar gate check for the optional remap=false @At target.

Run: python3 scripts/test_audit_optional_targets.py

Builds a stub JarIndex that models the unobfuscated vanilla 26.2 classpath:
BlocksAttacks has ONLY the 5-arg hurtBlockingItem + resolveBlockedDamage, and
LivingEntity#applyItemBlocking references only those two invocations. Then runs
audit_mixins(strict=True) over the real source tree and asserts that the new
6-arg (remap=false) target degrades to a warning, not an error.
"""
import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

spec = importlib.util.spec_from_file_location("audit_port", ROOT / "scripts/audit_port.py")
mod = importlib.util.module_from_spec(spec)
sys.modules["audit_port"] = mod
spec.loader.exec_module(mod)

BA = "net.minecraft.world.item.component.BlocksAttacks"
LE = "net.minecraft.world.entity.LivingEntity"
HURT5 = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;F)V"
HURT6 = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;FI)V"
RESOLVE = "(Lnet/minecraft/world/damagesource/DamageSource;FD)F"
APPLY = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)F"


def ci(**kw):
    base = dict(
        methods=frozenset(),
        fields=frozenset(),
        method_signatures=frozenset(),
        field_signatures=frozenset(),
        method_references={},
        client_only_methods=frozenset(),
        client_only_class=False,
        super_name=None,
        interfaces=(),
    )
    base.update(kw)
    return mod.ClassInfo(**base)


living = ci(
    methods=frozenset({"applyItemBlocking"}),
    method_signatures=frozenset({("applyItemBlocking", APPLY)}),
    method_references={
        ("applyItemBlocking", APPLY): frozenset(
            {
                (BA.replace(".", "/"), "resolveBlockedDamage", RESOLVE),
                (BA.replace(".", "/"), "hurtBlockingItem", HURT5),
            }
        )
    },
)
attacks = ci(
    method_signatures=frozenset(
        {
            ("hurtBlockingItem", HURT5),
            ("resolveBlockedDamage", RESOLVE),
        }
    )
)


class StubJars:
    def __init__(self, paths):
        self.paths = paths

    def close(self):
        pass

    def class_info(self, fqcn):
        if fqcn == LE:
            return living
        if fqcn == BA:
            return attacks
        return None

    def has_method(self, fqcn, signature, seen=None):
        info = self.class_info(fqcn)
        return info is not None and signature in info.method_signatures

    def has_field(self, fqcn, signature, seen=None):
        info = self.class_info(fqcn)
        return info is not None and signature in info.field_signatures


config = mod.json.loads(mod.MIXIN_JSON.read_text(encoding="utf-8"))
jars = StubJars([Path("fake/minecraft-26.2.jar")])
errors, warnings = mod.audit_mixins(config, jars, strict=True)

shield_errors = [e for e in errors if "BlocksAttacks" in e or "bullet_interactions" in e]
shield_warnings = [w for w in warnings if "BlocksAttacks" in w or "bullet_interactions" in w]

print("--- shield-related strict errors ---")
for e in shield_errors:
    print("  ", e)
print("--- shield-related warnings ---")
for w in shield_warnings:
    print("  ", w)

# Also confirm the optional-target classification directly on the mixin source.
text = (
    ROOT / "src/main/java/me/muksc/tacztweaks/mixin/features/bullet_interactions/LivingEntityMixin.java"
).read_text(encoding="utf-8")
import re

optional_targets = set()
for annotation in re.finditer(rf"@(?:{mod.INJECTOR_ANNOTATIONS})\s*\(", text):
    body = mod._annotation_body(text, text.index("(", annotation.start()))
    if body is None or not re.search(r"\bremap\s*=\s*false", body):
        continue
    for owner, name, descriptor in re.findall(
        r'target\s*=\s*"L([^;]+);([^(:"]+)(\([^\"]+)', body
    ):
        optional_targets.add((owner, name, descriptor))
print("--- remap=false optional targets detected ---")
for t in sorted(optional_targets):
    print("  ", "/".join(t))

ok = True
if shield_errors:
    ok = False
    print("FAIL: expected no strict errors for the shield mixin, got %d" % len(shield_errors))
expected_warn = (
    f"@At method reference is absent: {BA}#hurtBlockingItem{HURT6} "
    "(src/main/java/me/muksc/tacztweaks/mixin/features/bullet_interactions/LivingEntityMixin.java)"
    " [remap=false: optional call-site variant]"
)
if expected_warn not in warnings:
    ok = False
    print("FAIL: expected the 6-arg optional target warning, not found in warnings")
BA_SLASH = "net/minecraft/world/item/component/BlocksAttacks"
if (BA_SLASH, "hurtBlockingItem", HURT6) not in optional_targets:
    ok = False
    print("FAIL: 6-arg target not classified as remap=false optional")
if (BA_SLASH, "hurtBlockingItem", HURT5) in optional_targets:
    ok = False
    print("FAIL: 5-arg vanilla target must NOT be optional")
if (BA_SLASH, "resolveBlockedDamage", RESOLVE) in optional_targets:
    ok = False
    print("FAIL: resolveBlockedDamage target must NOT be optional")
if len(optional_targets) != 1:
    ok = False
    print("FAIL: expected exactly one optional target, got %r" % (optional_targets,))

print("RESULT:", "PASS" if ok else "FAIL")
sys.exit(0 if ok else 1)
