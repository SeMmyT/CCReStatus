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

# Extract metrics and POST to bridge using Python (jq not available on all systems).
# Fire-and-forget in background so statusline rendering isn't blocked.
python3 -c "
import json, sys, urllib.request

data = json.loads(sys.argv[1])
session_id = data.get('session_id', '')
if not session_id:
    sys.exit(0)

metrics = {
    'session_id': session_id,
    'context_percent': (data.get('context_window') or {}).get('used_percentage'),
    'cost_usd': (data.get('cost') or {}).get('total_cost_usd'),
    'model': (data.get('model') or {}).get('display_name'),
    'cwd': (data.get('workspace') or {}).get('current_dir'),
    'lines_added': (data.get('cost') or {}).get('total_lines_added'),
    'lines_removed': (data.get('cost') or {}).get('total_lines_removed'),
    'duration_ms': (data.get('cost') or {}).get('total_duration_ms'),
    'api_duration_ms': (data.get('cost') or {}).get('total_api_duration_ms'),
}

url = sys.argv[2] + '/session/' + session_id + '/metrics'
req = urllib.request.Request(url, data=json.dumps(metrics).encode(), headers={'Content-Type': 'application/json'}, method='POST')
try:
    urllib.request.urlopen(req, timeout=2)
except Exception:
    pass
" "$INPUT" "$BRIDGE_URL" >/dev/null 2>&1 &

# Pass through to ccstatusline for terminal display.
# Use installed binary if available (avoids npm registry check on every tick).
if command -v ccstatusline >/dev/null 2>&1; then
  echo "$INPUT" | ccstatusline
else
  echo "$INPUT" | npx -y ccstatusline@latest
fi
