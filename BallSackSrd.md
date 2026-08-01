# BallSack Software Requirements Document

## 1. Purpose
This document defines requirements for the BallSack interactive visualizer using a format-agnostic recursive payload called a BNF-Object.

The application supports:
1. Interactive authoring and manipulation of circles and sacks.
2. Import and visualization of recursively structured data represented as BNF-Object trees.

## 2. Scope
In scope:
1. Desktop GUI behavior and controls.
2. Geometry, packing, and containment behavior.
3. Rendering, visibility culling, and camera behavior.
4. Import of BNF-Object trees into visual scene structures.
5. Optional diagnostic logging.

Out of scope:
1. Concrete parser/serializer logic for specific external formats.
2. Web/mobile deployment.
3. Collaborative editing.

## 3. Product Overview
### 3.1 Runtime Platform
1. Python runtime.
2. Pygame rendering and event loop.
3. Desktop drag/drop and keyboard/mouse control model.

### 3.2 Core Domain Model
1. Circle: a visual node with center, radius, style, name, and optional children.
2. Sack: a container-style Circle with lower-alpha shell behavior.
3. Scene graph: recursive top-level list of Circle/Sack nodes.
4. Data interchange object: BNF-Object tree supplied by format adapters.

## 4. Functional Requirements
### FR-1 Startup and Loop
1. The system shall open a window and start an interactive render loop.
2. The system shall display control hints and status text.

### FR-2 Rendering
1. The system shall draw circles/sacks each frame.
2. The system shall apply world-to-screen transform using zoom and offsets.
3. The system shall visually differentiate selected nodes.

### FR-3 Node Creation
1. Left-drag on empty space shall preview/create a root node.
2. Left-drag inside a node shall preview/create a child node.
3. Preview commit shall require a minimum usable radius.

### FR-4 Condensing and Containers
1. User-defined containment gestures around nodes shall condense those nodes into a Sack.
2. Condense shall preserve children and replace root-level entries accordingly.
3. Resulting container shall be packed/tightened with non-overlap safeguards.

### FR-5 Move, Copy, Select, Pan
1. Right-drag on empty space shall pan camera.
2. Right-drag on node shall move node or selection group.
3. Ctrl+Right-drag shall duplicate subtree and drag the copy.
4. Shift+Left-click shall toggle selection membership.

### FR-6 Zoom and Framing
1. Wheel zoom shall preserve the world position under cursor.
2. Fit-all and smallest-visible framing actions shall be provided.
3. Zoom transitions shall support smooth tweening.

### FR-7 Labels
1. Modifier-assisted label display shall render node names.
2. Labels shall be width-fitted with middle truncation when needed.
3. Hovering a truncation marker shall reveal full name.

### FR-8 BNF-Object Import
1. The system shall accept recursively structured data as BNF-Object.
2. A root BNF-Object shall map to one visual subtree.
3. Dropping/importing on empty space shall place subtree at drop target.
4. Dropping/importing over a host node shall attempt nested insertion.
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
1. Window close shall terminate loop cleanly.
2. Scene-clear shortcut shall remove current scene nodes.

## 5. Non-Functional Requirements
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

## 6. External Interfaces
1. GUI input: mouse and keyboard controls.
2. File-system input/output: ingest adapted BNF-Object sources; write diagnostic log artifacts.
3. Desktop drag/drop event interface for import placement.

## 7. Constraints
1. Immediate-mode rendering primitives.
2. Single-process event loop.
3. Format conversion responsibilities delegated to adapters that produce/consume BNF-Object.

## 8. Assumptions
1. Runtime dependencies are installed and display/input are available.
2. Imported recursive structures are bounded enough for interactive inspection.

## 9. Acceptance Criteria
1. Running the app launches interactive editing and view controls.
2. User can create, move, copy, group, and relabel structures with constraints enforced.
3. Imported BNF-Object trees are visualized as nested scene graphs with context-aware placement.
4. Logging mode writes persistent per-run logs into logs directory.
5. Deep zoom and dense recursion paths remain render-safe through culling and radius limits.
