# Local compile dependencies

These two `SimpleBedrockModel` Fabric jars are the LGPL-3.0 compile dependencies
used by the corresponding branches of
[Sh1roCu/TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated):

- `simplebedrockmodel-fabric-2.3.0.1+mc1.20.1.jar`
- `simplebedrockmodel-fabric-2.3.0.1+mc1.21.1.jar`

TaCZ Tweaks v3 already declared them as flat-directory dependencies, but the
upstream repository did not include the files, which made a clean Stonecutter
checkout fail while configuring the older Fabric nodes. Each jar contains its
own `LICENSE_simplebedrockmodel-fabric` and embedded library metadata.

The Minecraft 1.21.11 target does not consume these jars; they are retained only
so Gradle can configure/build the existing 1.20.1 and 1.21.1 targets from a clean
checkout.
