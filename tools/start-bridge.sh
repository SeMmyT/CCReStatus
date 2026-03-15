#!/usr/bin/env bash
# Start the Agent ScreenSaver bridge server + Windows port forwarder
# Run this once — both services run in background
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BRIDGE_DIR="$SCRIPT_DIR/bridge"
WSL_IP=$(ip addr show eth0 | grep "inet " | awk '{print $2}' | cut -d/ -f1)

echo "=== Agent ScreenSaver Bridge ==="
echo "WSL2 IP: $WSL_IP"

# 1. Start bridge server (if not already running)
if curl -s http://localhost:4001/health >/dev/null 2>&1; then
    echo "Bridge already running on :4001"
else
    echo "Starting bridge server..."
    cd "$BRIDGE_DIR"
    nohup uv run claude-bridge --port 4001 --no-mdns >/tmp/agent-bridge.log 2>&1 &
    sleep 2
    if curl -s http://localhost:4001/health >/dev/null 2>&1; then
        echo "Bridge started on :4001"
    else
        echo "ERROR: Bridge failed to start. Check /tmp/agent-bridge.log"
        exit 1
    fi
fi

# 2. Update port forwarder with current WSL IP and start on Windows
cat > /mnt/c/Users/Daniel/Downloads/bridge_fwd.py << PYEOF
"""Port forwarder: 0.0.0.0:4001 (Windows/Tailscale) -> $WSL_IP:4001 (WSL2 bridge)"""
import socket, threading, sys

WSL_IP = "$WSL_IP"
PORT = 4001

def relay(src, dst):
    try:
        while True:
            data = src.recv(8192)
            if not data:
                break
            dst.sendall(data)
    except:
        pass
    src.close()
    dst.close()

def handle(client):
    try:
        remote = socket.socket()
        remote.connect((WSL_IP, PORT))
        threading.Thread(target=relay, args=(client, remote), daemon=True).start()
        relay(remote, client)
    except Exception as e:
        print(f"Connection failed: {e}")
        client.close()

server = socket.socket()
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(("0.0.0.0", PORT))
server.listen(5)
print(f"Forwarding 0.0.0.0:{PORT} -> {WSL_IP}:{PORT}")

while True:
    client, addr = server.accept()
    print(f"Connection from {addr}")
    threading.Thread(target=handle, args=(client,), daemon=True).start()
PYEOF

# Kill existing forwarder on Windows
/init /mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe -Command \
    "Get-Process python -ErrorAction SilentlyContinue | Where-Object { \$_.CommandLine -like '*bridge_fwd*' } | Stop-Process -Force -ErrorAction SilentlyContinue" 2>/dev/null || true

# Start forwarder on Windows
/init /mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe -Command \
    "Start-Process -WindowStyle Hidden python -ArgumentList 'C:\Users\Daniel\Downloads\bridge_fwd.py'" 2>/dev/null

sleep 2
if /init /mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe -Command \
    "Invoke-WebRequest -Uri http://100.82.136.105:4001/health -UseBasicParsing -TimeoutSec 3" >/dev/null 2>&1; then
    echo "Port forwarder running (Tailscale :4001 -> WSL2 :4001)"
else
    echo "WARNING: Port forwarder may not be reachable via Tailscale"
fi

echo ""
echo "Phone connects to: http://100.82.136.105:4001/events"
echo "=== Ready ==="
