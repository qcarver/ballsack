from dataclasses import dataclass
from enum import Enum
from pathlib import Path


class NodeForm(Enum):
    DIRECTORY = "directory"
    FILE = "file"


@dataclass
class Modifier:
    canonical_token: str


@dataclass
class Parsed:
    raw_name: str
    label: str
    form: NodeForm
    ordinal: int | None
    modifiers: list[Modifier]
    extension: str | None


@dataclass
class Node:
    parsed: Parsed
    children: list["Node"]


@dataclass
class Tree:
    root: Node
    diagnostics: tuple[object, ...]


SOUNDSCAPE_DIALECT = object()


def _parse_name(path: Path) -> Parsed:
    raw_name = path.name
    if path.is_dir():
        return Parsed(
            raw_name=raw_name,
            label=raw_name,
            form=NodeForm.DIRECTORY,
            ordinal=None,
            modifiers=[],
            extension=None,
        )

    if "." in raw_name:
        label, ext = raw_name.rsplit(".", 1)
    else:
        label, ext = raw_name, None
    return Parsed(
        raw_name=raw_name,
        label=label,
        form=NodeForm.FILE,
        ordinal=None,
        modifiers=[],
        extension=ext,
    )


def _scan(path: Path) -> Node:
    parsed = _parse_name(path)
    children = []
    if path.is_dir():
        entries = sorted(path.iterdir(), key=lambda p: p.name)
        for child in entries:
            children.append(_scan(child))
    return Node(parsed=parsed, children=children)


def scan_tree(root_path: Path, _dialect) -> Tree:
    return Tree(root=_scan(root_path), diagnostics=())
