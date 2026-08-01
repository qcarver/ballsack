# Ballsack Revision (BNFO + SVG)

This revision establishes a Python baseline for Ballsack with:

- Circle packing layout ported from `portme/circlepack/ComputePositions.java`
- BNFO-only data ingestion through `bnfo_bridges` adapter APIs
- Basic SVG rendering (no pygame)

## Run

Install UI dependency:

```bash
pip install PySide6
```

From this repository root:

```bash
PYTHONPATH=src python -m ballsack.cli --input test-drop.xml --output out/test-drop.svg
```

In-window drag/drop viewer:

```bash
PYTHONPATH=src python -m ballsack.ui_app
```

or with script entrypoint:

```bash
ballsack-ui
```

Viewer controls:

- Drop local file/folder directly into the window
- `Ctrl+Wheel`: zoom centered on cursor
- `Wheel`: vertical pan
- `Shift+Wheel`: horizontal pan
- Toolbar: Open..., Open Folder..., Reload, Save SVG As...

If `bnfo_bridges` is not installed in your interpreter, this project auto-adds:

- `~/Dev/BNF_Obj/src`

For directory import, set:

```bash
export TREE_UI_FILE=/absolute/path/to/tree_ui.py
```

Then run:

```bash
PYTHONPATH=src python -m ballsack.cli --input /some/directory --output out/tree.svg
```

For in-window folder dropping, the same `TREE_UI_FILE` environment variable is used.

## Notes

- Internal structure remains BNFO-shaped dictionaries:
  - `name: str`
  - `fields: list[(moniker, typeName, value)]`
  - `children: list[BNFO]`
- Geometry layout uses the Java salvage strategy: collision-free tangent candidates sampled at 360 degrees and minimum-distance selection to weighted center.
