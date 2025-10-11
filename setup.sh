#!/usr/bin/env bash
set -euo pipefail

# Determine the workspace directory (repository root) and ensure we run from there.
WORKSPACE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$WORKSPACE_DIR"

# Ensure Android SDK environment variables are not set so the build runs in JVM-only mode.
unset ANDROID_HOME || true
unset ANDROID_SDK_ROOT || true

# Run the JVM-only verification task used by agents.
./gradlew --no-daemon checkAgentsEnvironment --console=plain
