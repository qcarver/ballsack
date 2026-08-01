from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(slots=True)
class RenderCircle:
    name: str
    x: float
    y: float
    radius: float
    depth: int
    fields: list[tuple[str, str, object]] = field(default_factory=list)
    children: list["RenderCircle"] = field(default_factory=list)


BnfoNode = dict[str, object]
