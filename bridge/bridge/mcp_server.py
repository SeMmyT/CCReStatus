"""MCP server for orchestrating Claude Code sessions via the bridge.

Exposes tools to list, inspect, message, and broadcast to all running
Claude Code sessions. Works from claude.ai, Claude Desktop, or Claude Code.

Backend: connects to either:
  - RE:Tabby's built-in API (tabby-cc-orchestrator plugin, port 4002)
    Real PTY stdin injection — input goes directly to the terminal.
  - Python bridge (bridge/server.py, port 4001/4002)
    Queue-based — input is polled by CC hooks.

Usage:
    uv run python -m bridge.mcp_server [--bridge-url http://localhost:4002]
"""

from __future__ import annotations

import json
import os
import urllib.request
from typing import Any

from mcp.server.fastmcp import FastMCP

BRIDGE_URL = os.environ.get("CCSAVER_BRIDGE_URL", "http://localhost:4002")

mcp = FastMCP(
    "Claude Code Orchestrator",
    instructions=(
        "You are an orchestrator for multiple Claude Code sessions. "
        "Use list_sessions to see what's running, send_message to talk to one session, "
        "and broadcast to talk to all sessions at once."
    ),
)


def _bridge_get(path: str) -> Any:
    """GET request to bridge, return parsed JSON."""
    url = f"{BRIDGE_URL}{path}"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=5) as resp:
        return json.loads(resp.read())


def _bridge_post(path: str, data: dict) -> Any:
    """POST JSON to bridge, return parsed JSON."""
    url = f"{BRIDGE_URL}{path}"
    body = json.dumps(data).encode()
    req = urllib.request.Request(
        url, data=body, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=5) as resp:
        return json.loads(resp.read())


@mcp.tool()
def list_sessions() -> str:
    """List all active Claude Code sessions with their status, working directory, model, and cost.

    Returns a summary of every running session so you can decide which to interact with.
    """
    sessions = _bridge_get("/status")
    if not sessions:
        return "No active sessions."

    lines = []
    for sid, s in sessions.items():
        state = s.get("state", "unknown")
        cwd = s.get("cwd") or "unknown"
        # Strip to last 2 path segments
        parts = cwd.rstrip("/").split("/")
        short_cwd = "/".join(parts[-2:]) if len(parts) >= 2 else cwd
        model = s.get("model") or ""
        cost = s.get("cost_usd")
        cost_str = f"${cost:.2f}" if cost else ""
        tool = s.get("tool") or ""
        tool_input = s.get("tool_input_summary") or ""
        context = s.get("context_percent")
        ctx_str = f"{context:.0f}%" if context else ""

        status_parts = [state]
        if tool:
            status_parts.append(f"tool={tool}")
        if tool_input:
            status_parts.append(tool_input[:60])

        meta = " | ".join(filter(None, [model, cost_str, ctx_str]))
        lines.append(f"• [{sid[:8]}] {short_cwd} — {' '.join(status_parts)}")
        if meta:
            lines.append(f"  {meta}")

    return f"{len(sessions)} active session(s):\n\n" + "\n".join(lines)


@mcp.tool()
def get_session(session_id: str) -> str:
    """Get detailed status of a specific session.

    Args:
        session_id: Full or partial (8-char) session ID.
    """
    sessions = _bridge_get("/status")
    # Match by prefix
    matches = {k: v for k, v in sessions.items() if k.startswith(session_id)}
    if not matches:
        return f"No session found matching '{session_id}'."
    if len(matches) > 1:
        return f"Ambiguous: {len(matches)} sessions match. Be more specific."

    sid, s = next(iter(matches.items()))
    return json.dumps({"session_id": sid, **s}, indent=2)


@mcp.tool()
def send_message(session_id: str, text: str) -> str:
    """Send a message/instruction to a specific Claude Code session.

    The message will be delivered as user input to that session.
    Use this to give instructions, answer questions, or steer a specific session.

    Args:
        session_id: Full or partial (8-char) session ID.
        text: The message to send.
    """
    sessions = _bridge_get("/status")
    matches = [k for k in sessions if k.startswith(session_id)]
    if not matches:
        return f"No session found matching '{session_id}'."
    if len(matches) > 1:
        return f"Ambiguous: {len(matches)} sessions match."

    result = _bridge_post(f"/session/{matches[0]}/input", {"text": text})
    return f"Sent to {matches[0][:8]}: {text[:100]}"


@mcp.tool()
def broadcast(text: str) -> str:
    """Send a message to ALL active Claude Code sessions at once.

    Use this to give the same instruction to every running session,
    e.g. "commit your work", "stop what you're doing", "focus on tests".

    Args:
        text: The message to broadcast to all sessions.
    """
    result = _bridge_post("/broadcast", {"text": text})
    sent_to = result.get("sent_to", [])
    short_ids = [s[:8] for s in sent_to]
    return f"Broadcast to {len(sent_to)} session(s): {', '.join(short_ids)}\nMessage: {text}"


@mcp.tool()
def spawn_session(cwd: str = "", prompt: str = "") -> str:
    """Spawn a new Claude Code session in a new RE:Tabby tab.

    Requires RE:Tabby with the cc-orchestrator plugin as the backend.
    Opens a new terminal tab, optionally cd's to a directory, and launches claude.

    Args:
        cwd: Working directory for the new session (optional).
        prompt: Initial prompt to send to claude (optional, uses interactive mode if empty).
    """
    data: dict = {}
    if cwd:
        data["cwd"] = cwd
    if prompt:
        data["prompt"] = prompt

    try:
        result = _bridge_post("/spawn", data)
        tab_id = result.get("tab_id", "unknown")
        return f"Spawned new session in tab {tab_id}. It will appear in list_sessions shortly."
    except Exception as e:
        return f"Failed to spawn session (is RE:Tabby running?): {e}"


def main():
    """Run the MCP server via stdio transport."""
    mcp.run(transport="stdio")


if __name__ == "__main__":
    main()
