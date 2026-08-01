from __future__ import annotations

from html import escape

from .models import RenderCircle


PALETTE = [
    "#264653",
    "#2a9d8f",
    "#e9c46a",
    "#f4a261",
    "#e76f51",
]


def render_svg(root: RenderCircle, width: int = 1200, height: int = 900, show_labels: bool = True) -> str:
    min_x, min_y, max_x, max_y = _bounds(root)

    content_w = max_x - min_x
    content_h = max_y - min_y
    if content_w <= 0:
        content_w = 1.0
    if content_h <= 0:
        content_h = 1.0

    margin = 40.0
    scale = min((width - 2 * margin) / content_w, (height - 2 * margin) / content_h)

    tx = margin - min_x * scale
    ty = margin - min_y * scale

    lines: list[str] = []
    lines.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">')
    lines.append("  <rect width=\"100%\" height=\"100%\" fill=\"#f8f9fa\"/>")
    lines.extend(_draw_circle(root, scale, tx, ty, show_labels))
    lines.append("</svg>")
    return "\n".join(lines)


def _bounds(node: RenderCircle) -> tuple[float, float, float, float]:
    min_x = node.x - node.radius
    min_y = node.y - node.radius
    max_x = node.x + node.radius
    max_y = node.y + node.radius

    for child in node.children:
        cmin_x, cmin_y, cmax_x, cmax_y = _bounds(child)
        min_x = min(min_x, cmin_x)
        min_y = min(min_y, cmin_y)
        max_x = max(max_x, cmax_x)
        max_y = max(max_y, cmax_y)

    return min_x, min_y, max_x, max_y


def _draw_circle(node: RenderCircle, scale: float, tx: float, ty: float, show_labels: bool) -> list[str]:
    cx = node.x * scale + tx
    cy = node.y * scale + ty
    rr = node.radius * scale

    color = PALETTE[node.depth % len(PALETTE)]
    alpha = max(0.12, 0.28 - node.depth * 0.02)

    lines = [
        f'  <circle cx="{cx:.2f}" cy="{cy:.2f}" r="{rr:.2f}" fill="{color}" fill-opacity="{alpha:.3f}" stroke="#1f2937" stroke-width="1"/>',
    ]

    if show_labels and rr > 20:
        text = escape(node.name)
        lines.append(
            f'  <text x="{cx:.2f}" y="{cy:.2f}" text-anchor="middle" dominant-baseline="middle" '
            f'font-family="monospace" font-size="{max(10, min(16, rr * 0.2)):.0f}" fill="#111827">{text}</text>'
        )

    for child in node.children:
        lines.extend(_draw_circle(child, scale, tx, ty, show_labels))

    return lines
