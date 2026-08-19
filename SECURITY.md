# Security policy

## Supported versions

Only the currently maintained Fabric 1.21.11 beta line documented in `gradle.properties` and
`fabric.mod.json` is supported by this branch. Older experimental builds and the separate 26.x
branches may be referenced for diagnosis but are not guaranteed to receive 1.21.11 fixes.

## Reporting a vulnerability

Do not post private server addresses, access tokens, crash reports containing secrets, or exploit
steps against public servers in an issue. If private reporting is not enabled on GitHub, open a
minimal public issue that says a private security report is needed and avoid sensitive details until
a maintainer provides a private channel.

Security-relevant examples include:

- network payloads that bypass server authority or permissions;
- arbitrary file read/write or path traversal through data/config loading;
- crashes that can be triggered remotely by normal clients on a dedicated server;
- privilege bypass around unload, block breaking, shield damage or config sync;
- resource-pack or shader handling that exposes local/private information.

Please include affected versions, a minimal reproduction, expected impact, and whether the issue
requires optional compatibility mods.
