# Security policy

## Supported versions

Only the currently maintained NeoForge 26.2 beta line declared by `gradle.properties` and
`src/main/templates/META-INF/neoforge.mods.toml` is in scope. Older experimental builds may be used
for diagnosis but are not guaranteed fixes. Until the build/client/server gates documented in
`docs/KNOWN_ISSUES.md` pass, this line remains pre-release rather than production-validated.

## Reporting a vulnerability

Do not post private server addresses, access tokens, crash reports containing secrets, or exploit
steps against public servers in an issue. If private GitHub reporting is unavailable, open a minimal
public issue requesting a private channel and omit sensitive details.

Security-relevant examples include:

- payloads that bypass server authority, validation or permissions;
- arbitrary file access/path traversal through data or configuration loading;
- remotely triggerable dedicated-server crashes;
- privilege bypass around unload, block breaking, shield damage or config sync;
- resource/shader handling that exposes local information.

Include affected versions, a minimal reproduction, expected impact, and all required optional mods.
