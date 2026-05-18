#!/bin/sh

set -e

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
WRAPPER_CMD="$SCRIPT_DIR/mvnw.cmd"

if command -v cmd.exe >/dev/null 2>&1; then
  exec cmd.exe /C "\"$WRAPPER_CMD\"" "$@"
fi

echo "This wrapper script is intended for Windows shells." >&2
echo "Use mvnw.cmd from PowerShell or Command Prompt." >&2
exit 1