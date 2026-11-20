#!/bin/sh
set -eu
project=$(cd "$(dirname "$0")/../.." && pwd)
install=$1
game=$2
player=$3
mode=$4
cd "$project"
assets_cache=${PS_QA_ASSETS:-$HOME/.gradle/caches/fabric-loom/assets}
if [ ! -d "$assets_cache" ]; then assets_cache="$project/$install/asset-cache"; fi
mkdir -p "$assets_cache"
mkdir -p "$game/mods"
cp build/libs/psychedelicraft-1.20.1-port.2.jar "$game/mods/"
printf 'soundCategory_master:0.0\npauseOnLostFocus:false\nfullscreen:false\nrenderDistance:4\nmaxFps:30\n' > "$game/options.txt"
exec docker run --rm --platform linux/amd64 --init \
    --mount "type=bind,src=$project,dst=/task" \
    --mount "type=bind,src=$assets_cache,dst=/assets,readonly" \
    psychedelicraft-forge-qa:java17 \
    sh /task/tools/qa/virtual-display.sh \
    --install "/task/$install" --game "/task/$game" --assets /assets \
    --player "$player" --mode "$mode"
