#!/usr/bin/env bash
set -euo pipefail

# Determine the workspace directory (repository root) and ensure we run from there.
WORKSPACE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$WORKSPACE_DIR"

# Run the baseline verification task used by agents.
exec ./gradlew --no-daemon checkAgentsEnvironment --parallel --console=plain
