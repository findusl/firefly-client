#!/bin/bash
set -o pipefail

start=$(date +%s)

./gradlew checkAgentsEnvironment
status=$?

end=$(date +%s)
duration=$(( end - start ))

printf 'checkAgentsEnvironment duration: %s seconds\n' "$duration"

exit $status
