# Ballsack Java GUI

Thin Java desktop frontend over the existing Python implementation.

This app does not reimplement layout logic. It invokes:

- `python -m ballsack.cli --input <path> --output <svg>`

and renders the returned SVG in a Java window.

## Features

- Open XML/JSON file
- Open folder input
- Drag/drop file or folder onto the canvas
- Reload last source
- Save SVG As...
- Pan/zoom interactions provided by Batik `JSVGCanvas`

## Requirements

- Java 17+
- Maven 3.8+
- Python environment capable of running `ballsack.cli`

## Run

From the repository root:

```bash
cd java_gui
mvn compile exec:java
```

Optional input path argument:

```bash
mvn compile exec:java -Dexec.args="/absolute/path/to/input.xml"
```

## Troubleshooting

If you see a stack trace containing:

`Provider org.apache.xerces.jaxp.SAXParserFactoryImpl not found`

the runtime was missing an explicit Xerces provider on the classpath. This project includes `xerces:xercesImpl` in `pom.xml` to address that issue.

## Python Selection

Runtime Python is selected in this order:

1. `BALLSACK_PYTHON` environment variable
2. `../.venv/bin/python` (relative to repo root)
3. `python3`

The launcher sets `PYTHONPATH` to include:

- `<repo>/src`
- `~/Dev/BNF_Obj`
- `~/Dev/BNF_Obj/src`

so current bridge adapter behavior remains compatible.

For directory imports (`Open Folder`), the launcher sets `TREE_UI_FILE` as follows:

1. Use existing `TREE_UI_FILE` from your environment if present.
2. Otherwise use bundled `java_gui/tree_ui.py`.
