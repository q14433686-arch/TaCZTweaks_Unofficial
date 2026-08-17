#!/usr/bin/env bash
# Downloads the unofficial TaCZ Refabricated (26.2) jar from GitHub Releases into ./libs
# so the 26.2-fabric build can resolve it (it is NOT published to Modrinth maven).
#
# Usage: bash scripts/fetch-tacz-26.2.sh
set -euo pipefail

REPO="q14433686-arch/TaCZ_Refabricated_Unofficial"
TAG="26.2_R2"
ASSET="TACZ-Refabricated-26.2-1.1.8+fabric.26.2.R2.jar"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/libs"
mkdir -p "$DEST"

URL="https://github.com/${REPO}/releases/download/${TAG}/${ASSET}"
echo "Downloading ${URL}"
curl -fL --retry 3 -o "$DEST/$ASSET" "$URL"
echo "Saved to $DEST/$ASSET"
