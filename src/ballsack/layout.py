from __future__ import annotations

from .circlepack_port import Circle, compute_positions_circle_close_to_center
from .models import BnfoNode, RenderCircle


CHILD_PADDING = 8.0


def _field_value_size(fields: list[tuple[str, str, object]]) -> int:
    total = 0
    for _moniker, _type_name, value in fields:
        total += len(str(value))
    return total


def estimate_radius(node: BnfoNode) -> float:
    name = str(node.get("name", ""))
    fields = node.get("fields", [])
    children = node.get("children", [])

    field_bonus = min(80.0, _field_value_size(fields) * 0.12)
    return max(12.0, 16.0 + len(name) * 0.6 + len(fields) * 2.0 + len(children) * 3.0 + field_bonus)


def build_render_tree(root: BnfoNode) -> RenderCircle:
    return _build_node(root, x=0.0, y=0.0, depth=0)


def _offset_tree(node: RenderCircle, dx: float, dy: float) -> None:
    node.x += dx
    node.y += dy
    for child in node.children:
        _offset_tree(child, dx, dy)


def _build_node(node: BnfoNode, x: float, y: float, depth: int) -> RenderCircle:
    name = str(node.get("name", ""))
    fields = list(node.get("fields", []))
    children_nodes = list(node.get("children", []))

    render = RenderCircle(name=name, x=x, y=y, radius=estimate_radius(node), depth=depth, fields=fields)

    if not children_nodes:
        return render

    packed_children = [_build_node(child, x=0.0, y=0.0, depth=depth + 1) for child in children_nodes]
    packed_children.sort(key=lambda child: child.radius, reverse=True)

    circles = [Circle(radius=child.radius) for child in packed_children]
    circum = compute_positions_circle_close_to_center(circles)

    required_radius = circum.radius + CHILD_PADDING
    if required_radius > render.radius:
        render.radius = required_radius

    for child, placed in zip(packed_children, circles):
        _offset_tree(child, x + placed.x, y + placed.y)
        render.children.append(child)

    return render
