# Third-party notices

## Original TaCZ Tweaks example-pack audio

- Project: <https://github.com/MUKSC/TaCZTweaks>
- Version: v2.14.2 (`74ba2412`)
- Author: MUKSC
- License: GPL-3.0
- Use here: metal-impact and whizz `.ogg` fixtures under `tacz-tweaks-example-pack/assets/`.

The full GPL-3.0 text is included as [`LICENSE`](LICENSE).

## MixinExtras 0.5.4

- Project: <https://github.com/LlamaLad7/MixinExtras>
- Version/tag commit: `a6a2a42611341b1976b8b33b9a927ff5c84424ec`
- Author: LlamaLad7
- License: MIT
- Use here: `mixinextras-fabric` is embedded in the published mod jar.

Copyright (c) 2022-present LlamaLad7

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.

## First Aid New 1.3.x shader compatibility copies

- Project: <https://github.com/maoruiQa/FIrst-Aid-New>
- Source snapshot: `8fc4dd579c02ba3d3b29b96b2d22a2e12c48a5c5`
- License: GPL-3.0 (compatible with this project's GPL-3.0 distribution)
- Modified files:
  - `assets/firstaid/shaders/post/pain_pulse_blur.fsh`
  - `assets/firstaid/shaders/post/saturation_boost.fsh`
- Modification: removed the unused `minecraft:dynamictransforms.glsl` import that refers to
  a uniform block absent from First Aid's 26.2 post chains.

The full GPL-3.0 license is included in this repository as [`LICENSE`](LICENSE). These
compatibility copies are constrained to First Aid `>=1.3.0,<1.4.0`; they should be removed
once a supported upstream release contains the equivalent correction.
