#!/usr/bin/env bash
# statusline-bridge.sh — Pipe CC statusline metrics to the bridge server.
# Reads JSON from stdin, POSTs metrics to bridge, passes through to ccstatusline.
#
# Usage in ~/.claude/settings.json:
#   "statusLine": {
#     "type": "command",
#     "command": "bash /home/semmy/codeprojects/CCReStatus/tools/statusline-bridge.sh",
#     "padding": 0
#   }
set -uo pipefail

BRIDGE_URL="${CCSAVER_BRIDGE_URL:-http://localhost:4001}"
INPUT=$(cat)

# Extract fields with jq (fast, single parse)
METRICS=$(echo "$INPUT" | jq -c '{
  session_id: .session_id,
  context_percent: (.context_window.used_percentage // null),
  cost_usd: (.cost.total_cost_usd // null),
  model: (.model.display_name // null),
  cwd: (.workspace.current_dir // null),
  lines_added: (.cost.total_lines_added // null),
  lines_removed: (.cost.total_lines_removed // null),
  duration_ms: (.cost.total_duration_ms // null),
  api_duration_ms: (.cost.total_api_duration_ms // null)
}' 2>/dev/null)

SESSION_ID=$(echo "$METRICS" | jq -r '.session_id // empty' 2>/dev/null)

# POST to bridge (fire-and-forget, don't block statusline rendering)
if [ -n "$SESSION_ID" ]; then
  curl -s -X POST \
    "$BRIDGE_URL/session/$SESSION_ID/metrics" \
    -H "Content-Type: application/json" \
    -d "$METRICS" \
    --connect-timeout 1 \
    --max-time 2 \
    >/dev/null 2>&1 &
fi

# Pass through to ccstatusline for terminal display.
# Use installed binary if available (avoids npm registry check on every tick).
if command -v ccstatusline >/dev/null 2>&1; then
  echo "$INPUT" | ccstatusline
else
  echo "$INPUT" | npx -y ccstatusline@latest
fi
