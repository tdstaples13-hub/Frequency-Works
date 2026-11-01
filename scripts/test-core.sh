#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/core-test
javac -d build/core-test src/main/java/dev/frequencyworks/core/*.java src/test/java/dev/frequencyworks/core/CoreChecks.java
java -ea -cp build/core-test dev.frequencyworks.core.CoreChecks
