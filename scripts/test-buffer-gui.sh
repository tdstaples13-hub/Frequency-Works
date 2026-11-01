#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
output="$(mktemp -d)"; trap 'rm -rf "$output"' EXIT
mapfile -t src < <(find src/test/buffer-gui-stubs -name '*.java')
javac -encoding UTF-8 -d "$output" "${src[@]}" src/main/java/dev/frequencyworks/world/BufferInventoryActions.java
java -cp "$output" dev.frequencyworks.tests.BufferGuiChecks
