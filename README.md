# Ballsack Requirements and CLI Specification

This document is the authoritative specification for this repository.
It supersedes `SRD.md` for active development.

## Purpose

Ballsack defines a format-agnostic recursive visual transformation pipeline using a BNF-Object payload.

The system supports:

1. Authoring and manipulation of circles and sacks as scene-graph operations.
2. Import and visualization of recursively structured data represented as BNF-Object trees.
3. Deterministic, scriptable execution through a shell-first interface.

## Scope

In scope:

1. Shell command behavior and action API contracts.
2. Geometry, packing, and containment behavior.
3. Rendering output behavior and camera/framing semantics.
4. Import of BNF-Object trees into visual scene structures.
5. Optional diagnostic logging.

Out of scope:

1. Concrete parser/serializer logic for specific external formats.
2. Web/mobile deployment.
3. Collaborative editing.

## Product Overview

### Runtime Platform

1. Python runtime.
2. SVG rendering output for visualization and export.
3. Source payload adaptation through BNFO bridge adapters.

### Core Domain Model

1. Circle: a visual node with center, radius, style, name, and optional children.
2. Sack: a container-style Circle with shell behavior.
3. Scene graph: recursive top-level list of Circle/Sack nodes.
4. Data interchange object: BNF-Object tree supplied by format adapters.

## Current Command Entry Points

Install project dependencies with your selected Python environment.

Generate SVG from an input source:

```bash
PYTHONPATH=src python -m ballsack.cli --input test-drop.xml --output out/test-drop.svg
```

Directory-source import requires a `TREE_UI_FILE` bridge path:

```bash
export TREE_UI_FILE=/absolute/path/to/tree_ui.py
PYTHONPATH=src python -m ballsack.cli --input /some/directory --output out/tree.svg
```

If `bnfo_bridges` is not installed in your interpreter, this project attempts to resolve from:

1. `~/Dev/BNF_Obj/src`
2. `~/Dev/BNF_Obj`

## Java GUI Frontend

An optional Java desktop frontend now exists at `java_gui/`.

It is intentionally thin and invokes the existing Python CLI implementation (`ballsack.cli`) for all compute/layout work, then renders the generated SVG.

See `java_gui/README.md` for build/run instructions.

## Planned Shell Action Surface

This section defines the concrete shell surface that maps one-to-one with GUI-intent actions.
These commands are the target contract for FR-3 through FR-15.

### Command Form

```bash
ballsack action <name> [options]
```

Shared options for action commands:

1. `--scene <path>`: input scene document.
2. `--state-in <path>`: load mutable scene state.
3. `--state-out <path>`: persist mutable scene state.
4. `--output <path>`: write SVG output artifact.
5. `--result-json <path>`: write machine-readable action result.
6. `--stdin-json`: accept action payload from stdin JSON.
7. `--pretty`: pretty-print JSON result.
8. `--dry-run`: validate and report without mutating state.

### Action to Requirement Mapping

1. `create-root`: FR-3 root creation.
2. `create-child`: FR-3 child creation.
3. `condense`: FR-4 container creation and tightening.
4. `move`: FR-5 node/group translation.
5. `copy`: FR-5 subtree duplication.
6. `select`: FR-5 selection set mutation.
7. `pan`: FR-5 camera pan.
8. `zoom`: FR-6 anchored zoom with clamp.
9. `frame`: FR-6 fit-all / fit-node / fit-smallest-visible.
10. `labels`: FR-7 label mode and truncation policy.
11. `import`: FR-8 import subtree onto empty target.
12. `insert-import`: FR-8 import and nested insertion by host node id.
13. `render`: FR-2/FR-11 render current scene with visibility culling.
14. `reload`: FR-14 source reload.
15. `export`: FR-14 artifact export.
16. `clear`: FR-13 scene-clear behavior.

### Result Contract

Each action writes a JSON result envelope:

```json
{
   "ok": true,
   "action": "move",
   "sceneRevision": "r42",
   "changedNodeIds": ["n12", "n17"],
   "camera": {
      "zoom": 1.2,
      "panX": 48.0,
      "panY": -21.0
   },
   "bounds": {
      "minX": -210.3,
      "minY": -180.2,
      "maxX": 315.7,
      "maxY": 289.4
   },
   "messages": ["Moved selection by (+10,-5)."]
}
```

On failure, `ok` is `false` and `error` contains a stable code and message.

## Command Surface Test Layout

Deterministic test artifacts live under `tests/command_surface` and mirror BNFO smoke-test style across three concrete manifestations.

1. `tests/command_surface/json_manifest`: JSON input, run script, SVG output.
2. `tests/command_surface/xml_manifest`: XML input, run script, SVG output.
3. `tests/command_surface/tui_manifest`: directory tree input, run script, SVG output.

Generate or refresh deterministic fixtures:

```bash
PYTHONPATH=src .venv/bin/python tests/command_surface/scripts/generate_manifests.py
```

Execute each command-under-test script:

```bash
tests/command_surface/json_manifest/run.sh
tests/command_surface/xml_manifest/run.sh
tests/command_surface/tui_manifest/run.sh
```

Run the full command-surface suite and persist verbose results:

```bash
tests/command_surface/run_all.sh --no-cleanup --verbose
```

The suite runs fixture generation with `--print-pretty` by default to keep XML easy to inspect.

## Functional Requirements

### FR-1 Session and Loop

1. The system shall start an execution session from shell invocation.
2. The system shall emit actionable status text and errors.

### FR-2 Rendering

1. The system shall render circles/sacks into SVG output.
2. The system shall support world-to-screen transforms through camera parameters in action contracts.
3. The system shall support selected-node differentiation in rendered output metadata and style controls.

### FR-3 Node Creation

1. The action API shall support creating a root node on empty-space targets.
2. The action API shall support creating a child node inside a host node.
3. Create preview commit shall require a minimum usable radius.

### FR-4 Condensing and Containers

1. User-defined containment actions around nodes shall condense those nodes into a Sack.
2. Condense shall preserve children and replace root-level entries accordingly.
3. Resulting containers shall be packed/tightened with non-overlap safeguards.

### FR-5 Move, Copy, Select, Pan

1. The action API shall support camera pan.
2. The action API shall support moving a node or selection group.
3. The action API shall support subtree duplication and reposition.
4. The action API shall support additive/toggle selection membership.
5. The action API shall support directional pan controls when zoom-modifier is not active.

### FR-6 Zoom and Framing

1. Zoom actions shall preserve the world position under cursor/anchor when requested.
2. Fit-all and smallest-visible framing actions shall be provided.
3. Zoom transitions shall support smooth tweening semantics in API contracts.
4. Zoom level shall be clamped between configured min/max bounds.

### FR-7 Labels

1. Label display actions shall render node names.
2. Labels shall be width-fitted with middle truncation when needed.
3. Truncation expansion shall expose full names via action/query pathways.

### FR-8 BNF-Object Import

1. The system shall accept recursively structured data as BNF-Object.
2. A root BNF-Object shall map to one visual subtree.
3. Import on empty space shall place subtree at target position.
4. Import over a host node shall attempt nested insertion.
5. Nested insertion shall enforce containment and sibling non-overlap.
6. On insertion failure, the system shall report status and avoid partial commit.

### FR-9 Deterministic Identity

1. Nodes shall include deterministic seed keys.
2. Deterministic seeds shall drive stable visual identity across runs.

### FR-10 Geometry and Constraint Safety

1. Child nodes shall remain inside parent bounds minus configured gap.
2. Sibling overlap checks shall enforce configured separation.
3. Motion updates shall degrade step size to find valid placement before aborting.

### FR-11 Visibility Culling

1. Rendering shall skip off-screen nodes.
2. Recursive descent shall stop below configured projected-diameter threshold.
3. Label traversal shall use the same visibility-aware recursion policy.

### FR-12 Diagnostic Logging

1. If log CLI argument is passed, logging mode shall be enabled.
2. The system shall create a logs directory if absent.
3. The system shall write one persistent timestamped log document per run.
4. The log shall include lifecycle, status, camera, and import events.
5. Without log argument, log creation shall be disabled.

### FR-13 Exit Behavior

1. Session termination shall complete cleanly.
2. Scene-clear action shall remove current scene nodes.

### FR-14 Import and Export UX in Shell

1. The shell shall provide command forms for opening/importing xml/json sources.
2. The shell shall provide command forms for directory-source visualization.
3. The shell shall provide command forms for saving/exporting the current rendered scene.
4. The shell shall provide command forms for reloading the current source path.
5. Status messaging shall report last successful load path and failures.
6. Import failures shall return actionable details.

### FR-15 Action Parity and Shell Authority

1. Every user-visible mutation and camera action shall be invokable from shell.
2. The shell interface shall provide command forms for create, condense, move, copy, select, pan, zoom, framing, import, nested insertion, reload, and export actions.
3. All invocations of the same action shall execute through one shared application action API and produce equivalent scene outcomes.
4. Shell commands shall support deterministic addressing of nodes and selections.
5. Action results and failures shall be machine-readable with stable exit codes.
6. Unsupported actions shall return explicit capability errors rather than silent fallback.

## Non-Functional Requirements

### NFR-1 Responsiveness

1. Interaction should remain fluid under typical scene sizes.
2. Pan/zoom response should remain visually continuous.

### NFR-2 Stability

1. Rendering shall avoid unbounded allocation patterns.
2. Draw radius shall be capped for pathological zoom states.
3. Numeric guardrails shall handle invalid coordinate/radius values.

### NFR-3 Determinism

1. Deterministic seeding shall yield reproducible visual differentiation.

### NFR-4 Observability

1. Optional logging shall provide enough event detail for debugging.
2. Observability overhead shall be opt-in.

## External Interfaces

1. Shell command input for action execution.
2. File-system input/output for adapted BNF-Object sources and output artifacts.
3. Action API input/output for scene mutations, camera operations, and status reporting.

## Constraints

1. Immediate-mode rendering primitives for produced artifacts.
2. Single-process execution model for core action handling.
3. Format conversion responsibilities are delegated to adapters that produce/consume BNF-Object.

## Assumptions

1. Runtime dependencies are installed and filesystem access is available.
2. Imported recursive structures are bounded enough for interactive inspection and scripted processing.

## Acceptance Criteria

1. Running the system from shell supports authoring and view-control actions through the action API.
2. User can create, move, copy, group, and relabel structures with constraints enforced.
3. Imported BNF-Object trees are visualized as nested scene graphs with context-aware placement.
4. Logging mode writes persistent per-run logs into logs directory.
5. Deep zoom and dense recursion paths remain render-safe through culling and radius limits.

## Implementation Notes

1. Internal structure remains BNFO-shaped dictionaries:
   1. `name: str`
   2. `fields: list[(moniker, typeName, value)]`
   3. `children: list[BNFO]`
2. Geometry layout uses the Java-derived strategy of collision-free tangent candidates sampled at 360 degrees and minimum-distance selection to weighted center.
