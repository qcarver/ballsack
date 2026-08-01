#!/usr/bin/env python3
"""Interactive balls-and-sacks visualizer.

Features:
- Drag out circles with left mouse button.
- Draw around circles to condense them into a transparent sack.
- Drag and drop circles/sacks to reposition them.
- Drop an XML file onto the window to visualize its structure.
- Press Backspace to clear.
"""

from __future__ import annotations

import hashlib
import math
import os
import sys
import traceback
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import List, Optional, Set, Tuple
from urllib.parse import unquote, urlparse

import pygame

WINDOW_SIZE = (1000, 700)
FPS = 60

BG_TOP = (18, 24, 36)
BG_BOTTOM = (10, 14, 20)
TEXT_COLOR = (240, 246, 255)
GUIDE_COLOR = (255, 255, 255, 80)

# Geometric clearances in world units.
STROKE_WIDTH_UNITS = 2.0
PARENT_CHILD_GAP = STROKE_WIDTH_UNITS
SIBLING_GAP = 0.8
ZOOM_TWEEN_SECONDS = 0.10
MAX_DRAW_RADIUS_PX = 20000
MAX_DRAW_RADIUS_VIEWPORT_MULTIPLIER = 4.0
MIN_RECURSE_DIAMETER_PX = 2.0 * STROKE_WIDTH_UNITS


PALETTE = [
    (246, 92, 139),
    (84, 212, 228),
    (255, 196, 61),
    (125, 216, 91),
    (255, 137, 64),
    (156, 118, 255),
]


def stable_hash_int(text: str) -> int:
    digest = hashlib.blake2b(text.encode("utf-8", errors="ignore"), digest_size=8).digest()
    return int.from_bytes(digest, byteorder="big", signed=False)


def stable_unit(text: str) -> float:
    return stable_hash_int(text) / float(2**64 - 1)


def deterministic_color(name: str) -> Tuple[int, int, int]:
    base = PALETTE[stable_hash_int(name) % len(PALETTE)]
    tint = int(stable_unit("tint|" + name) * 48.0) - 24
    return (
        max(20, min(255, base[0] + tint)),
        max(20, min(255, base[1] + tint)),
        max(20, min(255, base[2] + tint)),
    )


def cli_logging_enabled(argv: List[str]) -> bool:
    for arg in argv[1:]:
        normalized = arg.strip().lower().lstrip("-")
        if normalized == "log":
            return True
    return False


@dataclass
class Circle:
    cx: float
    cy: float
    radius: float
    name: str = "circle"
    seed_key: str = ""
    color: Optional[Tuple[int, int, int]] = None
    alpha: int = 170
    children: List["Circle"] = field(default_factory=list)

    def __post_init__(self) -> None:
        if not self.seed_key:
            self.seed_key = self.name
        if self.color is None:
            self.color = deterministic_color(self.seed_key)

    def draw(
        self,
        surface: pygame.Surface,
        selected: bool = False,
        scale: float = 1.0,
        offset_x: float = 0.0,
        offset_y: float = 0.0,
        alpha_override: Optional[int] = None,
    ) -> None:
        sx = self.cx * scale + offset_x
        sy = self.cy * scale + offset_y
        if not (math.isfinite(sx) and math.isfinite(sy) and math.isfinite(self.radius) and math.isfinite(scale)):
            return

        draw_radius_f = max(0.0, self.radius * scale)
        if not math.isfinite(draw_radius_f):
            return

        viewport_w, viewport_h = surface.get_size()
        viewport_cap = int(max(viewport_w, viewport_h) * MAX_DRAW_RADIUS_VIEWPORT_MULTIPLIER)
        safe_cap = max(1, min(MAX_DRAW_RADIUS_PX, viewport_cap))
        draw_radius = max(1, int(min(draw_radius_f, safe_cap)))

        # Fast reject circles fully outside viewport.
        if sx + draw_radius < 0 or sy + draw_radius < 0 or sx - draw_radius > viewport_w or sy - draw_radius > viewport_h:
            return

        effective_alpha = self.alpha if alpha_override is None else alpha_override
        fill = (*self.color, max(0, min(255, effective_alpha)))
        border_color = (255, 255, 255, 240) if selected else (20, 24, 32, 230)
        border_width = 3 if selected else 2

        # Draw directly to avoid temporary surface allocation churn/OOM under deep zoom.
        # Display surfaces don't support per-pixel alpha fills natively, so use RGB.
        pygame.draw.circle(surface, fill[:3], (int(sx), int(sy)), draw_radius)
        pygame.draw.circle(surface, border_color[:3], (int(sx), int(sy)), draw_radius, border_width)

    def contains_point(self, x: float, y: float) -> bool:
        return math.dist((self.cx, self.cy), (x, y)) <= self.radius

    def move_by(self, dx: float, dy: float) -> None:
        self.cx += dx
        self.cy += dy
        for child in self.children:
            child.move_by(dx, dy)

    def set_center(self, x: float, y: float) -> None:
        self.move_by(x - self.cx, y - self.cy)


@dataclass
class Sack(Circle):
    def __post_init__(self) -> None:
        super().__post_init__()
        self.alpha = 40

    def draw(
        self,
        surface: pygame.Surface,
        selected: bool = False,
        scale: float = 1.0,
        offset_x: float = 0.0,
        offset_y: float = 0.0,
        alpha_override: Optional[int] = None,
    ) -> None:
        super().draw(
            surface,
            selected=selected,
            scale=scale,
            offset_x=offset_x,
            offset_y=offset_y,
            alpha_override=alpha_override,
        )

    def contains_point(self, x: float, y: float) -> bool:
        return super().contains_point(x, y)

    @staticmethod
    def _collides(
        x: float,
        y: float,
        radius: float,
        others: List[Circle],
        ignore: Optional[Circle] = None,
        padding: float = SIBLING_GAP,
    ) -> bool:
        for other in others:
            if other is ignore:
                continue
            min_dist = radius + other.radius + padding
            if math.dist((x, y), (other.cx, other.cy)) < min_dist:
                return True
        return False

    def _tighten_positions(self, circles: List[Circle], iterations: int = 80) -> None:
        for _ in range(iterations):
            improved = False
            for idx, circle in enumerate(circles):
                if idx == 0:
                    continue
                dx = self.cx - circle.cx
                dy = self.cy - circle.cy
                dist = math.hypot(dx, dy)
                if dist < 1e-6:
                    continue

                ux = dx / dist
                uy = dy / dist
                step = min(3.0, dist * 0.3)

                while step > 0.05:
                    nx = circle.cx + ux * step
                    ny = circle.cy + uy * step
                    if not self._collides(nx, ny, circle.radius, circles, ignore=circle):
                        circle.move_by(nx - circle.cx, ny - circle.cy)
                        improved = True
                        break
                    step *= 0.5

            if not improved:
                break

    @staticmethod
    def _tangent_positions(a: Circle, b: Circle, radius: float, padding: float) -> List[Tuple[float, float]]:
        ra = a.radius + radius + padding
        rb = b.radius + radius + padding
        dx = b.cx - a.cx
        dy = b.cy - a.cy
        d = math.hypot(dx, dy)

        if d < 1e-9:
            return []
        if d > ra + rb:
            return []
        if d < abs(ra - rb):
            return []

        ex = dx / d
        ey = dy / d
        x = (ra * ra - rb * rb + d * d) / (2.0 * d)
        y_sq = max(0.0, ra * ra - x * x)
        y = math.sqrt(y_sq)

        px = a.cx + ex * x
        py = a.cy + ey * x
        perp_x = -ey
        perp_y = ex

        return [
            (px + perp_x * y, py + perp_y * y),
            (px - perp_x * y, py - perp_y * y),
        ]

    def _enclosing_extent(self, circles: List[Circle]) -> float:
        max_extent = 0.0
        for child in circles:
            max_extent = max(max_extent, math.dist((self.cx, self.cy), (child.cx, child.cy)) + child.radius)
        return max_extent

    def _enclosing_extent_with_candidate(self, circles: List[Circle], cx: float, cy: float, radius: float) -> float:
        max_extent = self._enclosing_extent(circles)
        max_extent = max(max_extent, math.dist((self.cx, self.cy), (cx, cy)) + radius)
        return max_extent

    def _resolve_overlaps_hard(self, circles: List[Circle], padding: float, iterations: int = 400) -> None:
        for _ in range(iterations):
            moved = False
            for i in range(len(circles)):
                for j in range(i + 1, len(circles)):
                    a = circles[i]
                    b = circles[j]
                    dx = b.cx - a.cx
                    dy = b.cy - a.cy
                    d = math.hypot(dx, dy)
                    min_d = a.radius + b.radius + padding

                    if d >= min_d:
                        continue

                    if d < 1e-6:
                        angle = stable_unit(f"pair|{a.seed_key}|{b.seed_key}|{i}|{j}") * math.tau
                        dx = math.cos(angle)
                        dy = math.sin(angle)
                        d = 1.0

                    ux = dx / d
                    uy = dy / d
                    shift = (min_d - d) * 0.501
                    a.move_by(-ux * shift, -uy * shift)
                    b.move_by(ux * shift, uy * shift)
                    moved = True

            if not moved:
                break

    def _attract_without_overlap(self, circles: List[Circle], padding: float) -> bool:
        improved = False
        for idx, circle in enumerate(circles):
            if idx == 0:
                continue

            dx = self.cx - circle.cx
            dy = self.cy - circle.cy
            dist = math.hypot(dx, dy)
            if dist < 1e-6:
                continue

            ux = dx / dist
            uy = dy / dist
            step = min(2.0, dist * 0.2)

            while step > 0.04:
                nx = circle.cx + ux * step
                ny = circle.cy + uy * step
                if not self._collides(nx, ny, circle.radius, circles, ignore=circle, padding=padding):
                    circle.move_by(nx - circle.cx, ny - circle.cy)
                    improved = True
                    break
                step *= 0.5

        return improved

    @staticmethod
    def _stagger_pattern(index: int) -> int:
        if index == 0:
            return 0
        step = (index + 1) // 2
        return step if index % 2 == 1 else -step

    def _stagger_family_y(self, circles: List[Circle], padding: float) -> None:
        if len(circles) < 3:
            return

        # Stagger siblings by x-order so labels avoid flat notebook-like rows.
        by_x = sorted(circles, key=lambda c: c.cx)
        avg_radius = sum(c.radius for c in by_x) / len(by_x)
        unit = max(1.5, min(10.0, avg_radius * 0.20))

        for idx, circle in enumerate(by_x):
            pattern = self._stagger_pattern(idx)
            if pattern == 0:
                continue

            desired_shift = pattern * unit
            step = desired_shift

            while abs(step) > 0.05:
                ny = circle.cy + step
                if not self._collides(circle.cx, ny, circle.radius, circles, ignore=circle, padding=padding):
                    circle.move_by(0.0, ny - circle.cy)
                    break
                step *= 0.5

    def _rotation_seed_angle(self) -> float:
        key = self.seed_key + "|arity=" + str(len(self.children)) + "|" + "|".join(
            child.seed_key for child in self.children
        )
        return stable_unit("rotation|" + key) * math.tau

    def _rotate_children_around_center(self, angle: float) -> None:
        if abs(angle) < 1e-8:
            return
        c = math.cos(angle)
        s = math.sin(angle)
        for child in self.children:
            dx = child.cx - self.cx
            dy = child.cy - self.cy
            nx = self.cx + (dx * c - dy * s)
            ny = self.cy + (dx * s + dy * c)
            child.move_by(nx - child.cx, ny - child.cy)

    def pack_children(self) -> None:
        if not self.children:
            self.radius = max(10.0, self.radius)
            return

        padding = SIBLING_GAP
        children = sorted(self.children, key=lambda c: (-c.radius, stable_hash_int(c.seed_key)))
        if len(children) == 1:
            children[0].set_center(self.cx, self.cy)
        else:
            a = children[0]
            b = children[1]
            a.set_center(self.cx - (b.radius + padding * 0.5), self.cy)
            b.set_center(self.cx + (a.radius + padding * 0.5), self.cy)

            placed: List[Circle] = [a, b]
            boundary: List[int] = [0, 1]

            for idx in range(2, len(children)):
                child = children[idx]
                best: Optional[Tuple[float, float, float, int]] = None
                best_pair_slot = 0

                # Front-chain style: place tangent to adjacent boundary circles.
                for slot in range(len(boundary)):
                    i = boundary[slot]
                    j = boundary[(slot + 1) % len(boundary)]
                    for x, y in self._tangent_positions(children[i], children[j], child.radius, padding):
                        if self._collides(x, y, child.radius, placed, padding=padding):
                            continue

                        candidate_extent = self._enclosing_extent_with_candidate(placed, x, y, child.radius)

                        # Prefer tighter bounds and positions that touch boundary circles closely.
                        edge_fit = abs(math.dist((x, y), (children[i].cx, children[i].cy)) - (child.radius + children[i].radius + padding))
                        edge_fit += abs(math.dist((x, y), (children[j].cx, children[j].cy)) - (child.radius + children[j].radius + padding))
                        score = candidate_extent * 10.0 + edge_fit

                        if best is None or score < best[0]:
                            best = (score, x, y, slot)

                if best is None:
                    # Fallback if boundary-adjacent placements fail: try any boundary pair.
                    for i_pos in range(len(boundary)):
                        for j_pos in range(i_pos + 1, len(boundary)):
                            i = boundary[i_pos]
                            j = boundary[j_pos]
                            for x, y in self._tangent_positions(children[i], children[j], child.radius, padding):
                                if self._collides(x, y, child.radius, placed, padding=padding):
                                    continue
                                score = self._enclosing_extent_with_candidate(placed, x, y, child.radius)
                                if best is None or score < best[0]:
                                    best = (score, x, y, i_pos)

                if best is None:
                    # Last-resort search around current envelope.
                    radius_hint = self._enclosing_extent(placed)
                    placed_ok = False
                    for ring in range(1, 100):
                        d = radius_hint + ring * max(1.2, child.radius * 0.2)
                        samples = max(36, ring * 6)
                        for n in range(samples):
                            theta = (n / samples) * math.tau
                            x = self.cx + math.cos(theta) * d
                            y = self.cy + math.sin(theta) * d
                            if self._collides(x, y, child.radius, placed, padding=padding):
                                continue
                            child.set_center(x, y)
                            placed_ok = True
                            break
                        if placed_ok:
                            break
                    if not placed_ok:
                        # Guaranteed non-overlapping fallback by expanding until clear.
                        d = radius_hint + child.radius + 4.0
                        theta = stable_unit(f"fallback|{self.seed_key}|{child.seed_key}|{idx}") * math.tau
                        while True:
                            x = self.cx + math.cos(theta) * d
                            y = self.cy + math.sin(theta) * d
                            if not self._collides(x, y, child.radius, placed, padding=padding):
                                child.set_center(x, y)
                                break
                            d += max(1.0, child.radius * 0.12)
                else:
                    _, x, y, slot = best
                    child.set_center(x, y)
                    best_pair_slot = slot

                placed.append(child)
                insert_at = (best_pair_slot + 1) if best is not None else len(boundary)
                boundary.insert(insert_at, idx)

        # Resolve residual overlaps and tighten layout without ever introducing overlap.
        for _ in range(160):
            self._resolve_overlaps_hard(children, padding, iterations=1)
            if not self._attract_without_overlap(children, padding):
                break

        # Final hard pass guarantees strict non-overlap even in dense XML documents.
        self._resolve_overlaps_hard(children, padding, iterations=500)

        self._rotate_children_around_center(self._rotation_seed_angle())
        self._tighten_positions(children)
        self._stagger_family_y(children, padding)
        self._resolve_overlaps_hard(children, padding, iterations=120)

        # Finish with the same shell-minimization logic used by Space to avoid
        # having two noticeably different "tightness" outcomes.
        self.tighten_envelope_to_children(passes=24)

    def _radius_for_center(self, cx: float, cy: float, margin: float = PARENT_CHILD_GAP) -> float:
        if not self.children:
            return max(10.0, self.radius)
        worst = 0.0
        for child in self.children:
            worst = max(worst, math.dist((cx, cy), (child.cx, child.cy)) + child.radius + margin)
        return worst

    def tighten_envelope_to_children(self, passes: int = 32) -> None:
        """Shrink this sack around current child placements without moving children."""
        if not self.children:
            self.radius = max(10.0, self.radius)
            return

        # Good initial guess: average of child centers.
        avg_x = sum(child.cx for child in self.children) / len(self.children)
        avg_y = sum(child.cy for child in self.children) / len(self.children)

        best_x = avg_x
        best_y = avg_y
        best_r = self._radius_for_center(best_x, best_y)

        # Include current center as candidate in case it is already near-optimal.
        curr_r = self._radius_for_center(self.cx, self.cy)
        if curr_r < best_r:
            best_x, best_y, best_r = self.cx, self.cy, curr_r

        # Hill-climb with shrinking step to minimize enclosing radius.
        step = max(1.0, best_r * 0.35)
        for _ in range(passes):
            improved = False
            for dx, dy in (
                (step, 0.0),
                (-step, 0.0),
                (0.0, step),
                (0.0, -step),
                (step, step),
                (step, -step),
                (-step, step),
                (-step, -step),
            ):
                cx = best_x + dx
                cy = best_y + dy
                r = self._radius_for_center(cx, cy)
                if r < best_r:
                    best_x, best_y, best_r = cx, cy, r
                    improved = True
            if not improved:
                step *= 0.55
                if step < 0.02:
                    break

        # Update sack shell only; children stay exactly where user placed them.
        self.cx = best_x
        self.cy = best_y
        self.radius = max(16.0, best_r)


@dataclass
class HitInfo:
    node: Circle
    parent: Optional[Circle]
    depth: int


class BallsackApp:
    def __init__(self, enable_log: bool = False) -> None:
        self.enable_log = enable_log
        self.log_file_path: Optional[Path] = None
        self.log_root: Optional[ET.Element] = None
        self.log_tree: Optional[ET.ElementTree] = None
        self.start_timestamp_utc = datetime.utcnow()
        self._init_debug_log()

        pygame.init()
        pygame.display.set_caption("Ballsack Python Port")
        self.screen = pygame.display.set_mode(WINDOW_SIZE)
        self.clock = pygame.time.Clock()
        self.font = pygame.font.SysFont("dejavusans", 18)
        self.big_font = pygame.font.SysFont("dejavusans", 24, bold=True)

        self.circles: List[Circle] = []
        self.preview: Optional[Circle] = None
        self.preview_host: Optional[Circle] = None
        self.preview_host_parent: Optional[Circle] = None

        self.selected_nodes: Set[int] = set()
        self.right_drag_mode: Optional[str] = None
        self.grabbed_node: Optional[Circle] = None
        self.grabbed_parent: Optional[Circle] = None
        self.grab_group: List[Circle] = []
        self.grab_offset = (0.0, 0.0)
        self.pan_last_screen: Optional[Tuple[int, int]] = None

        self.status_text = "Ready"
        self.status_ticks = 0

        self.zoom = 1.0
        self.offset_x = 0.0
        self.offset_y = 0.0
        self.zoom_tween: Optional[dict] = None

        self.label_fonts: dict[int, pygame.font.Font] = {}
        self.hover_label_font = pygame.font.SysFont("arial", 20)

        self.set_cursor("arrow")
        self.log_event(
            "app_initialized",
            window_width=WINDOW_SIZE[0],
            window_height=WINDOW_SIZE[1],
            fps=FPS,
            python=sys.version.split()[0],
            pygame=pygame.version.ver,
        )

    def _init_debug_log(self) -> None:
        if not self.enable_log:
            return

        project_root = Path(__file__).resolve().parent
        logs_dir = project_root / "logs"
        logs_dir.mkdir(parents=True, exist_ok=True)

        timestamp = datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
        filename = f"debug_{timestamp}_pid{os.getpid()}.xml"
        self.log_file_path = logs_dir / filename

        self.log_root = ET.Element(
            "debugLog",
            {
                "createdUtc": self.start_timestamp_utc.isoformat(timespec="seconds") + "Z",
                "pid": str(os.getpid()),
                "cwd": str(Path.cwd()),
                "argv": " ".join(sys.argv),
            },
        )
        self.log_tree = ET.ElementTree(self.log_root)
        self._flush_log()

    def _flush_log(self) -> None:
        if not self.enable_log or self.log_tree is None or self.log_file_path is None:
            return
        self.log_tree.write(self.log_file_path, encoding="utf-8", xml_declaration=True)

    def log_event(self, event_type: str, **fields: object) -> None:
        if not self.enable_log or self.log_root is None:
            return

        event = ET.SubElement(self.log_root, "event")
        event.set("type", event_type)
        event.set("utc", datetime.utcnow().isoformat(timespec="milliseconds") + "Z")

        if pygame.get_init():
            event.set("ticks", str(pygame.time.get_ticks()))

        for key, value in fields.items():
            if value is None:
                continue
            event.set(str(key), str(value))

        self._flush_log()

    def run(self) -> None:
        running = True
        self.log_event("run_start")
        while running:
            dt_seconds = self.clock.get_time() / 1000.0
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    self.log_event("quit_event")
                    running = False
                elif event.type == pygame.KEYDOWN:
                    self.log_event("key_down", key=event.key, mods=event.mod)
                    self.handle_key(event)
                elif event.type == pygame.MOUSEBUTTONDOWN and event.button == 1:
                    self.handle_left_down(*event.pos)
                elif event.type == pygame.MOUSEBUTTONDOWN and event.button == 3:
                    self.handle_right_down(*event.pos)
                elif event.type == pygame.MOUSEBUTTONDOWN and event.button in (4, 5):
                    zoom_factor = 1.1 if event.button == 4 else (1.0 / 1.1)
                    self.zoom_at(event.pos[0], event.pos[1], zoom_factor)
                elif event.type == pygame.MOUSEMOTION:
                    self.handle_mouse_motion(*event.pos)
                elif event.type == pygame.MOUSEBUTTONUP and event.button == 1:
                    self.handle_left_up(*event.pos)
                elif event.type == pygame.MOUSEBUTTONUP and event.button == 3:
                    self.handle_right_up(*event.pos)
                elif event.type == pygame.DROPFILE:
                    if hasattr(event, "x") and hasattr(event, "y"):
                        drop_pos = (int(event.x), int(event.y))
                    else:
                        drop_pos = pygame.mouse.get_pos()
                    self.load_xml(event.file, source="drop", drop_screen_pos=drop_pos)
                elif event.type == pygame.MOUSEWHEEL:
                    mx, my = pygame.mouse.get_pos()
                    self.zoom_at(mx, my, 1.1 ** event.y)

            self.update_zoom_tween(dt_seconds)

            self.draw()
            self.clock.tick(FPS)

        self.log_event("run_end")
        pygame.quit()

    def handle_key(self, event: pygame.event.Event) -> None:
        mods = pygame.key.get_mods()

        if (mods & pygame.KMOD_SHIFT) and event.key in (pygame.K_EQUALS, pygame.K_PLUS, pygame.K_KP_PLUS):
            self.zoom_fit_all_data()
            self.set_status("Zoomed to fit all data", seconds=1.8)
            return

        if (mods & pygame.KMOD_SHIFT) and event.key in (pygame.K_MINUS, pygame.K_KP_MINUS):
            self.zoom_smallest_to_fill_view()
            self.set_status("Zoomed to smallest element", seconds=1.8)
            return

        if event.key == pygame.K_BACKSPACE:
            self.circles.clear()
            self.set_status("Cleared all circles/sacks", seconds=2.0)
            self.selected_nodes.clear()
            return

        if event.key == pygame.K_SPACE:
            self.tighten_all_sacks_inside_out()
            self.set_status("Tightened sacks from inner to outer", seconds=2.0)
            return

        if event.key == pygame.K_o and (event.mod & pygame.KMOD_CTRL):
            self.open_xml_dialog()

    def tighten_all_sacks_inside_out(self) -> None:
        def tighten_node(node: Circle) -> None:
            for child in node.children:
                tighten_node(child)
            if isinstance(node, Sack):
                node.tighten_envelope_to_children()

        for root in self.circles:
            tighten_node(root)

    def world_to_screen(self, x: float, y: float) -> Tuple[float, float]:
        return (x * self.zoom + self.offset_x, y * self.zoom + self.offset_y)

    def screen_to_world(self, x: float, y: float) -> Tuple[float, float]:
        return ((x - self.offset_x) / self.zoom, (y - self.offset_y) / self.zoom)

    def start_zoom_tween(self, target_zoom: float, target_offset_x: float, target_offset_y: float) -> None:
        safe_zoom = max(1e-7, target_zoom)
        self.zoom_tween = {
            "start_zoom": self.zoom,
            "start_offset_x": self.offset_x,
            "start_offset_y": self.offset_y,
            "target_zoom": safe_zoom,
            "target_offset_x": target_offset_x,
            "target_offset_y": target_offset_y,
            "elapsed": 0.0,
            "duration": ZOOM_TWEEN_SECONDS,
        }

    def update_zoom_tween(self, dt_seconds: float) -> None:
        if not self.zoom_tween:
            return

        tween = self.zoom_tween
        tween["elapsed"] += max(0.0, dt_seconds)
        t = min(1.0, tween["elapsed"] / tween["duration"])
        # Smoothstep easing.
        e = t * t * (3.0 - 2.0 * t)

        self.zoom = tween["start_zoom"] + (tween["target_zoom"] - tween["start_zoom"]) * e
        self.offset_x = tween["start_offset_x"] + (tween["target_offset_x"] - tween["start_offset_x"]) * e
        self.offset_y = tween["start_offset_y"] + (tween["target_offset_y"] - tween["start_offset_y"]) * e

        if t >= 1.0:
            self.zoom = tween["target_zoom"]
            self.offset_x = tween["target_offset_x"]
            self.offset_y = tween["target_offset_y"]
            self.zoom_tween = None

    def zoom_at(self, sx: float, sy: float, factor: float) -> None:
        old_zoom = self.zoom
        new_zoom = max(1e-7, old_zoom * factor)
        if abs(new_zoom - old_zoom) < 1e-12:
            return

        # Keep the world point under the mouse fixed while zooming.
        wx, wy = self.screen_to_world(sx, sy)
        target_offset_x = sx - wx * new_zoom
        target_offset_y = sy - wy * new_zoom
        self.start_zoom_tween(new_zoom, target_offset_x, target_offset_y)
        self.set_status(f"Zoom: {self.zoom:.2f}x", seconds=1.5)
        self.log_event(
            "zoom",
            screen_x=f"{sx:.2f}",
            screen_y=f"{sy:.2f}",
            factor=f"{factor:.6f}",
            old_zoom=f"{old_zoom:.8f}",
            target_zoom=f"{new_zoom:.8f}",
        )

    def all_nodes(self) -> List[Circle]:
        return [info.node for info in self.iter_node_infos()]

    def zoom_fit_all_data(self) -> None:
        nodes = self.all_nodes()
        if not nodes:
            return

        min_x = min(node.cx - node.radius for node in nodes)
        max_x = max(node.cx + node.radius for node in nodes)
        min_y = min(node.cy - node.radius for node in nodes)
        max_y = max(node.cy + node.radius for node in nodes)

        width, height = self.screen.get_size()
        world_w = max(1e-7, max_x - min_x)
        world_h = max(1e-7, max_y - min_y)
        target_zoom = min((width * 0.92) / world_w, (height * 0.92) / world_h)

        center_x = (min_x + max_x) * 0.5
        center_y = (min_y + max_y) * 0.5
        target_offset_x = (width / 2.0) - center_x * target_zoom
        target_offset_y = (height / 2.0) - center_y * target_zoom
        self.start_zoom_tween(target_zoom, target_offset_x, target_offset_y)

    def zoom_smallest_to_fill_view(self) -> None:
        nodes = [node for node in self.all_nodes() if node.radius > 0]
        if not nodes:
            return

        # "Smallest element shown": prefer currently visible nodes.
        width, height = self.screen.get_size()
        shown_nodes = []
        for node in nodes:
            sx, sy = self.world_to_screen(node.cx, node.cy)
            rpx = node.radius * self.zoom
            if rpx <= 0:
                continue
            if sx + rpx < 0 or sy + rpx < 0 or sx - rpx > width or sy - rpx > height:
                continue
            shown_nodes.append(node)

        pool = shown_nodes if shown_nodes else nodes

        # Ignore numerically tiny circles that are effectively not visible; they can
        # drive astronomical zoom targets and destabilize rendering.
        visible_floor = 0.25 / max(self.zoom, 1e-12)
        viable = [n for n in pool if n.radius >= visible_floor]
        if not viable:
            return

        smallest = min(viable, key=lambda n: n.radius)
        target_zoom = (min(width, height) * 0.92) / max(1e-7, 2.0 * smallest.radius)
        target_offset_x = (width / 2.0) - smallest.cx * target_zoom
        target_offset_y = (height / 2.0) - smallest.cy * target_zoom
        self.start_zoom_tween(target_zoom, target_offset_x, target_offset_y)

    def set_cursor(self, style: str) -> None:
        cursor_map = {
            "arrow": pygame.SYSTEM_CURSOR_ARROW,
            "open_hand": pygame.SYSTEM_CURSOR_HAND,
            "grabbing": pygame.SYSTEM_CURSOR_SIZEALL,
        }
        try:
            pygame.mouse.set_cursor(pygame.cursors.Cursor(cursor_map.get(style, pygame.SYSTEM_CURSOR_ARROW)))
        except Exception:
            pass

    def set_status(self, message: str, seconds: float = 4.0) -> None:
        self.status_text = message
        self.status_ticks = int(FPS * max(0.5, seconds))
        self.log_event("status", message=message, seconds=f"{seconds:.2f}")

    def tick_status(self) -> None:
        if self.status_ticks > 0:
            self.status_ticks -= 1

    def open_xml_dialog(self) -> None:
        try:
            import tkinter as tk
            from tkinter import filedialog
        except Exception:
            self.set_status("File picker unavailable. Try drag/drop or install tkinter.", seconds=5.0)
            return

        root = tk.Tk()
        root.withdraw()
        root.attributes("-topmost", True)

        selected = filedialog.askopenfilename(
            title="Select XML File",
            filetypes=[("XML files", "*.xml"), ("All files", "*.*")],
        )
        root.destroy()

        if not selected:
            self.set_status("XML load canceled", seconds=2.0)
            return

        self.load_xml(selected, source="picker")

    def iter_node_infos(self):
        def walk(node: Circle, parent: Optional[Circle], depth: int):
            yield HitInfo(node=node, parent=parent, depth=depth)
            for child in node.children:
                yield from walk(child, node, depth + 1)

        for root in self.circles:
            yield from walk(root, None, 0)

    def iter_visible_node_infos(self):
        viewport_w, viewport_h = self.screen.get_size()

        def walk(node: Circle, parent: Optional[Circle], depth: int):
            sx, sy = self.world_to_screen(node.cx, node.cy)
            if not (math.isfinite(sx) and math.isfinite(sy) and math.isfinite(node.radius) and math.isfinite(self.zoom)):
                return

            rpx = max(0.0, node.radius * self.zoom)
            if rpx <= 0.0:
                return

            if sx + rpx < 0 or sy + rpx < 0 or sx - rpx > viewport_w or sy - rpx > viewport_h:
                return

            yield HitInfo(node=node, parent=parent, depth=depth)

            # Stop descending once the rendered diameter is below visual line fidelity.
            if (2.0 * rpx) < MIN_RECURSE_DIAMETER_PX:
                return

            for child in node.children:
                yield from walk(child, node, depth + 1)

        for root in self.circles:
            yield from walk(root, None, 0)

    def deepest_hit(self, wx: float, wy: float) -> Optional[HitInfo]:
        infos = list(self.iter_node_infos())
        for info in reversed(infos):
            if info.node.contains_point(wx, wy):
                return info
        return None

    def container_of(self, parent: Optional[Circle]) -> List[Circle]:
        return self.circles if parent is None else parent.children

    def would_overlap_top_level(self, candidate: Circle, ignore: Optional[Circle] = None) -> bool:
        for shape in self.circles:
            if shape is ignore:
                continue
            min_dist = candidate.radius + shape.radius + SIBLING_GAP
            if math.dist((candidate.cx, candidate.cy), (shape.cx, shape.cy)) < min_dist:
                return True
        return False

    def overlaps_in_container(
        self,
        candidate: Circle,
        parent: Optional[Circle],
        ignore_ids: Optional[Set[int]] = None,
        cx: Optional[float] = None,
        cy: Optional[float] = None,
    ) -> bool:
        x = candidate.cx if cx is None else cx
        y = candidate.cy if cy is None else cy
        ignore_ids = ignore_ids or set()
        for other in self.container_of(parent):
            if id(other) == id(candidate) or id(other) in ignore_ids:
                continue
            min_dist = candidate.radius + other.radius + SIBLING_GAP
            if math.dist((x, y), (other.cx, other.cy)) < min_dist:
                return True
        return False

    def inside_parent_bounds(self, node: Circle, parent: Optional[Circle], cx: float, cy: float) -> bool:
        if parent is None:
            return True
        return math.dist((cx, cy), (parent.cx, parent.cy)) + node.radius <= (parent.radius - PARENT_CHILD_GAP)

    def _has_overlap_anywhere(self, circles: List[Circle], padding: float = SIBLING_GAP) -> bool:
        for i in range(len(circles)):
            for j in range(i + 1, len(circles)):
                a = circles[i]
                b = circles[j]
                if math.dist((a.cx, a.cy), (b.cx, b.cy)) < (a.radius + b.radius + padding):
                    return True
        return False

    def handle_left_down(self, x: float, y: float) -> None:
        wx, wy = self.screen_to_world(x, y)
        hit = self.deepest_hit(wx, wy)
        mods = pygame.key.get_mods()

        if (mods & pygame.KMOD_SHIFT) and hit is not None:
            node_id = id(hit.node)
            if node_id in self.selected_nodes:
                self.selected_nodes.remove(node_id)
            else:
                self.selected_nodes.add(node_id)
            return

        if hit is not None:
            self.preview_host = hit.node
            self.preview_host_parent = hit.parent
            idx = len(hit.node.children)
            seed_key = f"drawn|parent={hit.node.seed_key}|idx={idx}|x={int(wx*10)}|y={int(wy*10)}"
            self.preview = Circle(float(wx), float(wy), 1.0, name=f"child-of-{hit.node.name}", seed_key=seed_key)
        else:
            self.preview_host = None
            self.preview_host_parent = None
            idx = len(self.circles)
            seed_key = f"drawn|parent=universe|idx={idx}|x={int(wx*10)}|y={int(wy*10)}"
            self.preview = Circle(float(wx), float(wy), 1.0, name="child-of-universe", seed_key=seed_key)

    def clone_subtree(self, node: Circle) -> Circle:
        if isinstance(node, Sack):
            clone: Circle = Sack(
                node.cx,
                node.cy,
                node.radius,
                name=node.name,
                seed_key=node.seed_key,
                color=node.color,
                alpha=node.alpha,
            )
        else:
            clone = Circle(
                node.cx,
                node.cy,
                node.radius,
                name=node.name,
                seed_key=node.seed_key,
                color=node.color,
                alpha=node.alpha,
            )

        clone.children = [self.clone_subtree(child) for child in node.children]
        return clone

    def handle_right_down(self, x: float, y: float) -> None:
        wx, wy = self.screen_to_world(x, y)
        hit = self.deepest_hit(wx, wy)
        mods = pygame.key.get_mods()

        if hit is None:
            self.right_drag_mode = "pan"
            self.pan_last_screen = (int(x), int(y))
            self.set_cursor("open_hand")
            return

        if mods & pygame.KMOD_CTRL:
            clone = self.clone_subtree(hit.node)
            self.container_of(hit.parent).append(clone)
            self.grabbed_node = clone
            self.grabbed_parent = hit.parent
            self.grab_group = [clone]
            self.grab_offset = (wx - clone.cx, wy - clone.cy)
            self.right_drag_mode = "copy"
            self.set_cursor("grabbing")
            return

        group = [hit.node]
        if id(hit.node) in self.selected_nodes:
            group = [node for node in self.container_of(hit.parent) if id(node) in self.selected_nodes]
            if not group:
                group = [hit.node]

        self.grabbed_node = hit.node
        self.grabbed_parent = hit.parent
        self.grab_group = group
        self.grab_offset = (wx - hit.node.cx, wy - hit.node.cy)
        self.right_drag_mode = "move"
        self.set_cursor("grabbing")

    def apply_group_delta(self, dx: float, dy: float) -> bool:
        if not self.grab_group:
            return False

        group_ids = {id(node) for node in self.grab_group}
        parent = self.grabbed_parent

        for factor in (1.0, 0.6, 0.35, 0.2, 0.1, 0.05):
            sdx = dx * factor
            sdy = dy * factor
            ok = True
            for node in self.grab_group:
                nx = node.cx + sdx
                ny = node.cy + sdy
                if not self.inside_parent_bounds(node, parent, nx, ny):
                    ok = False
                    break
                if self.overlaps_in_container(node, parent, ignore_ids=group_ids, cx=nx, cy=ny):
                    ok = False
                    break

            if ok:
                for node in self.grab_group:
                    node.move_by(sdx, sdy)
                return True

        return False

    def handle_mouse_motion(self, x: float, y: float) -> None:
        wx, wy = self.screen_to_world(x, y)

        if self.right_drag_mode == "pan" and self.pan_last_screen is not None:
            dxs = x - self.pan_last_screen[0]
            dys = y - self.pan_last_screen[1]
            self.offset_x += dxs
            self.offset_y += dys
            self.pan_last_screen = (int(x), int(y))
            return

        if self.right_drag_mode in {"move", "copy"} and self.grabbed_node is not None:
            target_x = wx - self.grab_offset[0]
            target_y = wy - self.grab_offset[1]
            dx = target_x - self.grabbed_node.cx
            dy = target_y - self.grabbed_node.cy
            self.apply_group_delta(dx, dy)
            return

        if self.preview is not None:
            raw_radius = max(1.0, math.dist((self.preview.cx, self.preview.cy), (wx, wy)))
            if self.preview_host is not None:
                max_radius = self.preview_host.radius - math.dist(
                    (self.preview.cx, self.preview.cy),
                    (self.preview_host.cx, self.preview_host.cy),
                ) - PARENT_CHILD_GAP
                for sibling in self.preview_host.children:
                    max_radius = min(
                        max_radius,
                        math.dist((self.preview.cx, self.preview.cy), (sibling.cx, sibling.cy))
                        - sibling.radius
                        - SIBLING_GAP,
                    )
                self.preview.radius = max(1.0, min(raw_radius, max_radius))
            else:
                self.preview.radius = raw_radius

    def handle_left_up(self, x: float, y: float) -> None:
        if self.preview is None:
            return

        if self.preview.radius > 4.0:
            if self.preview_host is None:
                if not self.condense(self.preview):
                    if self.would_overlap_top_level(self.preview):
                        self.set_status("Circle overlaps another perimeter; draw it elsewhere.", seconds=3.5)
                    else:
                        self.circles.append(self.preview)
            else:
                if self.add_child_circle(self.preview_host, self.preview_host_parent, self.preview):
                    self.set_status("Added child circle", seconds=1.6)

        self.preview = None
        self.preview_host = None
        self.preview_host_parent = None

    def handle_right_up(self, x: float, y: float) -> None:
        if self.right_drag_mode is None:
            return

        self.right_drag_mode = None
        self.grabbed_node = None
        self.grabbed_parent = None
        self.grab_group = []
        self.pan_last_screen = None
        self.set_cursor("arrow")

    def promote_to_sack(self, node: Circle, parent: Optional[Circle]) -> Sack:
        if isinstance(node, Sack):
            return node

        promoted = Sack(
            node.cx,
            node.cy,
            node.radius,
            name=node.name,
            seed_key=node.seed_key,
            color=node.color,
            alpha=max(40, node.alpha),
        )
        promoted.children = node.children

        container = self.container_of(parent)
        for i, item in enumerate(container):
            if item is node:
                container[i] = promoted
                break

        if self.preview_host is node:
            self.preview_host = promoted
        if self.grabbed_node is node:
            self.grabbed_node = promoted

        return promoted

    def add_child_circle(self, host: Circle, parent: Optional[Circle], child: Circle) -> bool:
        host = self.promote_to_sack(host, parent)

        if not self.inside_parent_bounds(child, host, child.cx, child.cy):
            self.set_status("Child circle must stay inside host circle.", seconds=3.0)
            return False

        for sibling in host.children:
            if math.dist((child.cx, child.cy), (sibling.cx, sibling.cy)) < (
                child.radius + sibling.radius + SIBLING_GAP
            ):
                self.set_status("Child overlaps an existing sibling.", seconds=3.0)
                return False

        host.children.append(child)
        return True

    def condense(self, container: Circle) -> bool:
        contained = [
            c for c in self.circles if math.dist((container.cx, container.cy), (c.cx, c.cy)) <= container.radius
        ]

        if not contained:
            return False

        avg_x = sum(c.cx for c in contained) / len(contained)
        avg_y = sum(c.cy for c in contained) / len(contained)

        for c in contained:
            self.circles.remove(c)

        child_sig = "|".join(sorted(c.seed_key for c in contained))
        sack_seed = f"condense|{child_sig}|count={len(contained)}"
        sack = Sack(avg_x, avg_y, container.radius, name="sack", seed_key=sack_seed, children=contained)
        sack.pack_children()
        if isinstance(sack, Sack) and sack.children and self._has_overlap_anywhere(sack.children):
            self.set_status("Warning: overlap detected after packing; auto-correct attempted.", seconds=5.0)
        if self.would_overlap_top_level(sack):
            # Put contained circles back if resulting sack would overlap unrelated shapes.
            self.circles.extend(contained)
            self.set_status("Sack would overlap another shape; try a different boundary.", seconds=4.0)
            return True
        self.circles.append(sack)
        return True

    def draw_background(self) -> None:
        width, height = self.screen.get_size()

        for y in range(height):
            t = y / max(1, height - 1)
            r = int(BG_TOP[0] * (1 - t) + BG_BOTTOM[0] * t)
            g = int(BG_TOP[1] * (1 - t) + BG_BOTTOM[1] * t)
            b = int(BG_TOP[2] * (1 - t) + BG_BOTTOM[2] * t)
            pygame.draw.line(self.screen, (r, g, b), (0, y), (width, y))

        for i in range(8):
            radius = 120 + i * 40
            alpha = max(0, 50 - i * 6)
            if alpha <= 0:
                break
            glow = pygame.Surface((radius * 2, radius * 2), pygame.SRCALPHA)
            pygame.draw.circle(glow, (60, 100, 180, alpha), (radius, radius), radius)
            self.screen.blit(glow, (width - radius - 70, 80 - radius // 2))

    def iter_nodes(self, shape: Circle):
        yield shape
        for child in shape.children:
            yield from self.iter_nodes(child)

    def depth_alpha(self, depth: int) -> int:
        # Deeper nodes become more opaque.
        return max(35, min(255, 70 + depth * 30))

    def get_label_font(self, px_size: int) -> pygame.font.Font:
        clamped = max(1, min(18, px_size))
        font = self.label_fonts.get(clamped)
        if font is None:
            font = pygame.font.SysFont("arial", clamped)
            self.label_fonts[clamped] = font
        return font

    @staticmethod
    def _middle_ellipsis(text: str, keep: int) -> str:
        if keep <= 0:
            return "…"
        left = (keep + 1) // 2
        right = keep // 2
        if right == 0:
            return text[:left] + "…"
        return text[:left] + "…" + text[-right:]

    def fit_name_to_width(self, name: str, font: pygame.font.Font, max_width: float) -> Tuple[str, bool]:
        if max_width <= 0:
            return "", False

        if font.size(name)[0] <= max_width:
            return name, False

        # Remove letters from the middle until the name with an ellipsis fits.
        for keep in range(len(name) - 1, -1, -1):
            candidate = self._middle_ellipsis(name, keep)
            if font.size(candidate)[0] <= max_width:
                return candidate, True

        # If even a standalone ellipsis does not fit, render nothing.
        if font.size("…")[0] <= max_width:
            return "…", True
        return "", False

    def draw_node_labels(self) -> None:
        mods = pygame.key.get_mods()
        if not (mods & pygame.KMOD_ALT):
            return

        mouse_x, mouse_y = pygame.mouse.get_pos()
        hovered_full_name: Optional[str] = None

        for info in self.iter_visible_node_infos():
            node = info.node
            if not node.name:
                continue

            screen_radius = node.radius * self.zoom
            if screen_radius < 2.0:
                continue

            sx, sy = self.world_to_screen(node.cx, node.cy)
            font_size = int(screen_radius * 0.42)
            font = self.get_label_font(font_size)
            max_width = max(0.0, (screen_radius * 2.0) - 6.0)
            fitted_name, was_truncated = self.fit_name_to_width(node.name, font, max_width)
            if not fitted_name:
                continue

            text = font.render(fitted_name, True, (248, 251, 255))
            text_rect = text.get_rect(center=(int(sx), int(sy)))

            # Soft shadow keeps labels readable over bright fills.
            shadow = font.render(fitted_name, True, (12, 16, 22))
            shadow_rect = shadow.get_rect(center=(int(sx + 1), int(sy + 1)))
            self.screen.blit(shadow, shadow_rect)
            self.screen.blit(text, text_rect)

            if was_truncated and "…" in fitted_name:
                ellipsis_index = fitted_name.find("…")
                prefix = fitted_name[:ellipsis_index]
                prefix_w = font.size(prefix)[0]
                ellipsis_w = font.size("…")[0]
                ellipsis_rect = pygame.Rect(
                    text_rect.left + prefix_w,
                    text_rect.top,
                    ellipsis_w,
                    text_rect.height,
                )
                if ellipsis_rect.collidepoint(mouse_x, mouse_y):
                    hovered_full_name = node.name

        if hovered_full_name:
            full_text = self.hover_label_font.render(hovered_full_name, True, (255, 255, 255))
            shadow = self.hover_label_font.render(hovered_full_name, True, (14, 18, 24))
            pad = 10
            box_w = full_text.get_width() + pad * 2
            box_h = full_text.get_height() + pad * 2
            box_x = min(max(8, mouse_x + 16), self.screen.get_width() - box_w - 8)
            box_y = min(max(8, mouse_y - box_h - 12), self.screen.get_height() - box_h - 8)

            backdrop = pygame.Surface((box_w, box_h), pygame.SRCALPHA)
            pygame.draw.rect(backdrop, (18, 24, 34, 220), backdrop.get_rect(), border_radius=8)
            pygame.draw.rect(backdrop, (220, 232, 245, 180), backdrop.get_rect(), width=1, border_radius=8)
            self.screen.blit(backdrop, (box_x, box_y))

            self.screen.blit(shadow, (box_x + pad + 1, box_y + pad + 1))
            self.screen.blit(full_text, (box_x + pad, box_y + pad))

    def draw(self) -> None:
        self.draw_background()

        grabbed_ids = {id(n) for n in self.grab_group}
        for info in self.iter_visible_node_infos():
            info.node.draw(
                self.screen,
                selected=(id(info.node) in self.selected_nodes or id(info.node) in grabbed_ids),
                scale=self.zoom,
                offset_x=self.offset_x,
                offset_y=self.offset_y,
                alpha_override=self.depth_alpha(info.depth),
            )

        if self.preview is not None:
            self.preview.draw(self.screen, scale=self.zoom, offset_x=self.offset_x, offset_y=self.offset_y)
            guide = pygame.Surface(self.screen.get_size(), pygame.SRCALPHA)
            pcx, pcy = self.world_to_screen(self.preview.cx, self.preview.cy)
            pygame.draw.circle(
                guide,
                GUIDE_COLOR,
                (int(pcx), int(pcy)),
                int(self.preview.radius * self.zoom),
                1,
            )
            self.screen.blit(guide, (0, 0))

        self.draw_node_labels()

        title = self.big_font.render("Ballsack: Python Port", True, TEXT_COLOR)
        tip = self.font.render(
            "L-drag create child/root | R-drag pan/move | Ctrl+R-drag copy | Shift+L click select | Alt names",
            True,
            (220, 228, 240),
        )
        status = self.font.render(f"Status: {self.status_text}", True, (200, 235, 255))
        self.screen.blit(title, (18, 14))
        self.screen.blit(tip, (18, 46))
        self.screen.blit(status, (18, self.screen.get_height() - 32))

        self.tick_status()

        pygame.display.flip()

    def normalize_input_path(self, path_str: str) -> Path:
        text = path_str.strip()
        if text.startswith("file://"):
            parsed = urlparse(text)
            if parsed.netloc and parsed.netloc != "localhost":
                return Path(unquote(parsed.path))
            return Path(unquote(parsed.path))
        return Path(unquote(text))

    def scale_subtree_about(self, node: Circle, anchor_x: float, anchor_y: float, factor: float) -> None:
        node.cx = anchor_x + (node.cx - anchor_x) * factor
        node.cy = anchor_y + (node.cy - anchor_y) * factor
        node.radius *= factor
        for child in node.children:
            self.scale_subtree_about(child, anchor_x, anchor_y, factor)

    def place_within_host(self, host: Circle, child: Circle, desired_x: float, desired_y: float) -> bool:
        # Try desired spot first.
        if self.inside_parent_bounds(child, host, desired_x, desired_y):
            collision = any(
                math.dist((desired_x, desired_y), (s.cx, s.cy)) < (child.radius + s.radius + SIBLING_GAP)
                for s in host.children
            )
            if not collision:
                child.set_center(desired_x, desired_y)
                return True

        # Search for nearest valid placement around desired point.
        max_d = max(1.0, host.radius - PARENT_CHILD_GAP - child.radius)
        for ring in range(0, 80):
            d = min(max_d, ring * max(1.2, child.radius * 0.15))
            samples = max(18, 24 + ring * 3)
            phase = stable_unit(f"place|{host.seed_key}|{child.seed_key}|{ring}") * math.tau
            for n in range(samples):
                theta = phase + (n / samples) * math.tau
                x = desired_x + math.cos(theta) * d
                y = desired_y + math.sin(theta) * d
                if not self.inside_parent_bounds(child, host, x, y):
                    continue
                collision = any(
                    math.dist((x, y), (s.cx, s.cy)) < (child.radius + s.radius + SIBLING_GAP)
                    for s in host.children
                )
                if collision:
                    continue
                child.set_center(x, y)
                return True

        return False

    def auto_zoom_into_host(self, host: Circle) -> None:
        width, height = self.screen.get_size()
        target_radius_px = min(width, height) * 0.34
        desired_zoom = target_radius_px / max(1e-7, host.radius)
        target_offset_x = (width / 2.0) - host.cx * desired_zoom
        target_offset_y = (height / 2.0) - host.cy * desired_zoom
        self.start_zoom_tween(desired_zoom, target_offset_x, target_offset_y)

    def load_xml(
        self,
        path_str: str,
        source: str = "unknown",
        drop_screen_pos: Optional[Tuple[int, int]] = None,
    ) -> None:
        self.log_event("load_xml_start", source=source, raw_path=path_str)
        path = self.normalize_input_path(path_str)

        if path.is_dir():
            self.set_status(f"Dropped folder ignored: {path}", seconds=4.0)
            self.log_event("load_xml_ignored_dir", path=path)
            return

        if path.suffix.lower() != ".xml":
            self.set_status(f"Not an XML file: {path.name}", seconds=4.0)
            self.log_event("load_xml_ignored_extension", path=path)
            return

        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as exc:
            self.set_status(f"XML parse error: {exc}", seconds=5.0)
            self.log_event("load_xml_parse_error", path=path, error=exc)
            return
        except OSError as exc:
            self.set_status(f"File error: {exc}", seconds=5.0)
            self.log_event("load_xml_os_error", path=path, error=exc)
            return

        shape = self.build_shape(root)
        if drop_screen_pos is None:
            drop_screen_pos = (self.screen.get_width() // 2, self.screen.get_height() // 2)

        drop_wx, drop_wy = self.screen_to_world(float(drop_screen_pos[0]), float(drop_screen_pos[1]))
        hit = self.deepest_hit(drop_wx, drop_wy)

        # Universe drop: instantiate at drop location.
        if hit is None:
            shape.set_center(drop_wx, drop_wy)
            self.circles.append(shape)
            self.set_status(f"Loaded XML at drop point: {path.name}", seconds=3.5)
            self.log_event("load_xml_universe_drop", path=path, source=source, world_x=drop_wx, world_y=drop_wy)
            return

        # Innerspace drop: persist as child content of target host.
        host = self.promote_to_sack(hit.node, hit.parent)

        # Anchor the dropped graph at cursor world location before fitting.
        shape.set_center(drop_wx, drop_wy)

        # If too large for host, scale subtree down to fit host innerspace.
        max_host_radius = host.radius - PARENT_CHILD_GAP
        if shape.radius > max_host_radius and shape.radius > 0:
            factor = max(0.02, max_host_radius / shape.radius)
            self.scale_subtree_about(shape, shape.cx, shape.cy, factor)

        placed = self.place_within_host(host, shape, drop_wx, drop_wy)
        if not placed:
            # Last resort: scale further and retry placement.
            if shape.radius > 0:
                shrink = 0.75
                self.scale_subtree_about(shape, shape.cx, shape.cy, shrink)
            placed = self.place_within_host(host, shape, host.cx, host.cy)

        if not placed:
            self.set_status(f"Drop could not fit inside {host.name}", seconds=4.0)
            self.log_event("load_xml_fit_failed", path=path, host=host.name, host_seed=host.seed_key)
            return

        host.children.append(shape)
        self.auto_zoom_into_host(host)
        self.set_status(
            f"Dropped inside {host.name}; auto-zoomed in. Use wheel to zoom back out.",
            seconds=4.5,
        )
        self.log_event("load_xml_nested_drop", path=path, source=source, host=host.name, host_seed=host.seed_key)

    def build_shape(self, element: ET.Element, path: str = "") -> Circle:
        tag = element.tag
        node_path = f"{path}/{tag}" if path else tag
        child_elements = list(element)
        children = [
            self.build_shape(child, f"{node_path}[{idx}]")
            for idx, child in enumerate(child_elements)
        ]
        if children:
            seed_key = f"xml|path={node_path}|arity={len(children)}"
            sack = Sack(0.0, 0.0, 20.0, name=element.tag, seed_key=seed_key, children=children)
            sack.pack_children()
            return sack

        text_len = len((element.text or "").strip())
        radius = max(8.0, min(80.0, float(text_len + 10)))
        seed_key = f"xml|path={node_path}|textlen={text_len}|arity=0"
        return Circle(0.0, 0.0, radius, name=element.tag, seed_key=seed_key)


if __name__ == "__main__":
    enable_log = cli_logging_enabled(sys.argv)
    app = BallsackApp(enable_log=enable_log)
    try:
        app.run()
    except Exception as exc:
        app.log_event("unhandled_exception", error=exc, traceback=traceback.format_exc())
        raise
