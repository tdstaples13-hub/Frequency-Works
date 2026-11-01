#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT
javac -encoding UTF-8 -d "$BUILD_DIR" \
  src/main/java/dev/frequencyworks/core/BufferTier.java \
  src/main/java/dev/frequencyworks/core/FrequencyId.java \
  src/main/java/dev/frequencyworks/security/BufferSecurityPolicy.java \
  src/test/java/dev/frequencyworks/core/BufferSecurityChecks.java
java -cp "$BUILD_DIR" dev.frequencyworks.core.BufferSecurityChecks
