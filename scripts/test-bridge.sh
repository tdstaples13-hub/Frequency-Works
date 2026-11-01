#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT
javac -d "$tmpdir" src/main/java/dev/frequencyworks/core/*.java \
    src/main/java/dev/frequencyworks/world/PortTransferService.java \
    $(find src/testHarness/java -name '*.java')
java -ea -cp "$tmpdir" dev.frequencyworks.tests.PortTransferChecks
