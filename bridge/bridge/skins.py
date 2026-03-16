"""Community skin storage and retrieval."""
from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any

logger = logging.getLogger(__name__)

SKINS_DIR = Path.home() / ".claude" / "ccsaver-skins"


def ensure_dir() -> Path:
    SKINS_DIR.mkdir(parents=True, exist_ok=True)
    return SKINS_DIR


def list_skins() -> list[dict[str, Any]]:
    """List all installed community skins (metadata only)."""
    skins = []
    for f in ensure_dir().glob("*.json"):
        try:
            data = json.loads(f.read_text())
            skins.append({
                "id": data.get("id", f.stem),
                "name": data.get("name", f.stem),
                "description": data.get("description", ""),
                "author": data.get("author", "unknown"),
                "is_premium": data.get("is_premium", False),
            })
        except (json.JSONDecodeError, KeyError):
            continue
    return skins


def get_skin(skin_id: str) -> dict[str, Any] | None:
    """Get full skin data by ID."""
    path = ensure_dir() / f"{skin_id}.json"
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text())
    except (json.JSONDecodeError, KeyError):
        return None


def save_skin(skin_data: dict[str, Any]) -> str:
    """Save a skin, returns its ID."""
    skin_id = skin_data.get("id", "unnamed")
    path = ensure_dir() / f"{skin_id}.json"
    path.write_text(json.dumps(skin_data, indent=2))
    logger.info("Saved skin '%s' to %s", skin_id, path)
    return skin_id


def delete_skin(skin_id: str) -> bool:
    """Delete a skin by ID. Returns True if deleted."""
    if skin_id == "ghost":
        return False  # can't delete built-in
    path = ensure_dir() / f"{skin_id}.json"
    if path.exists():
        path.unlink()
        return True
    return False
