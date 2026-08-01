from __future__ import annotations

import json
import os
from pathlib import Path
import sys
from typing import Any

from .models import BnfoNode


def _append_default_bnfo_path() -> None:
    candidates = []

    custom_src = os.environ.get("BNFO_SRC")
    if custom_src:
        candidates.append(Path(custom_src).expanduser())

    home_root = Path.home() / "Dev" / "BNF_Obj"
    candidates.append(home_root / "src")
    candidates.append(home_root)

    # Allow sibling checkout layouts.
    here = Path(__file__).resolve()
    candidates.append(here.parents[4] / "BNF_Obj" / "src")
    candidates.append(here.parents[4] / "BNF_Obj")

    for candidate in candidates:
        if candidate.exists() and str(candidate) not in sys.path:
            sys.path.append(str(candidate))


def _load_bridge_symbol(module_name: str, symbol_name: str) -> Any:
    _append_default_bnfo_path()

    try:
        module = __import__(module_name, fromlist=[symbol_name])
        return getattr(module, symbol_name)
    except (ImportError, AttributeError):
        # Fallback to legacy non-package module names from BNF_Obj root.
        legacy_modules = {
            "bnfo_bridges.xml_bridge": "bnfo_xml_bridge",
            "bnfo_bridges.json_bridge": "bnfo_json_bridge",
            "bnfo_bridges.tui_bridge": "bnfo_tui_bridge",
        }
        legacy_module_name = legacy_modules.get(module_name)
        if not legacy_module_name:
            raise

        module = __import__(legacy_module_name, fromlist=[symbol_name])
        return getattr(module, symbol_name)


def load_bnfo_from_path(path: Path) -> BnfoNode:
    if path.is_dir():
        return _load_from_directory(path)

    lower = path.suffix.lower()
    if lower == ".xml":
        return _load_from_xml(path)
    if lower == ".json":
        return _load_from_json(path)

    raise ValueError(f"Unsupported input type: {path}")


def _load_from_xml(path: Path) -> BnfoNode:
    xml_text_to_bnfo = _load_bridge_symbol("bnfo_bridges.xml_bridge", "xml_text_to_bnfo")
    xml_text = path.read_text(encoding="utf-8")
    return xml_text_to_bnfo(xml_text)


def _load_from_json(path: Path) -> BnfoNode:
    json_node_to_bnfo = _load_bridge_symbol("bnfo_bridges.json_bridge", "json_node_to_bnfo")
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("JSON input must be an object.")
    return json_node_to_bnfo(payload)


def _load_from_directory(path: Path) -> BnfoNode:
    load_tree_ui = _load_bridge_symbol("bnfo_bridges.tui_bridge", "load_tree_ui")
    import_tui_tree_to_bnfo = _load_bridge_symbol("bnfo_bridges.tui_bridge", "import_tui_tree_to_bnfo")

    tree_ui_env = os.environ.get("TREE_UI_FILE")
    if not tree_ui_env:
        raise ValueError(
            "Directory import requires TREE_UI_FILE env var pointing to tree_ui.py for bnfo_bridges.tui_bridge.load_tree_ui()."
        )

    tree_ui = load_tree_ui(Path(tree_ui_env).expanduser())
    bnfo_root, _diagnostics = import_tui_tree_to_bnfo(tree_ui, path, max_depth=7)
    return bnfo_root
