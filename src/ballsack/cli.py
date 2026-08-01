from __future__ import annotations

import argparse
from pathlib import Path

from .bnfo_adapter import load_bnfo_from_path
from .layout import build_render_tree
from .svg_render import render_svg


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Ballsack SVG generator from BNFO bridge inputs.")
    parser.add_argument("--input", required=True, type=Path, help="Input xml/json file or directory")
    parser.add_argument("--output", required=True, type=Path, help="Output SVG path")
    parser.add_argument("--width", type=int, default=1200)
    parser.add_argument("--height", type=int, default=900)
    parser.add_argument("--hide-labels", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    bnfo = load_bnfo_from_path(args.input.expanduser().resolve())
    render_tree = build_render_tree(bnfo)

    svg_text = render_svg(
        render_tree,
        width=args.width,
        height=args.height,
        show_labels=not args.hide_labels,
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(svg_text, encoding="utf-8")
    print(f"Wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
