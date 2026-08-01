from __future__ import annotations

from dataclasses import dataclass
import math


PI180 = math.pi / 180.0


@dataclass(slots=True)
class Circle:
    x: float = 0.0
    y: float = 0.0
    radius: float = 0.0
    computed: bool = False


@dataclass(slots=True)
class CircumscribedCircle:
    x: float = 0.0
    y: float = 0.0
    radius: float = 0.0
    original_radius: float = 0.0
    area_of_circumscribed_circle: float = 0.0
    minimum: float = 0.0
    maximum: float = 0.0


def dist(x1: float, y1: float, x2: float, y2: float) -> float:
    temp_x = x2 - x1
    temp_y = y2 - y1
    return math.sqrt(temp_x * temp_x + temp_y * temp_y)


def _dist_from_center(x: float, y: float) -> float:
    return math.sqrt(x * x + y * y)


def compute_positions_circle_close_to_center(circles: list[Circle]) -> CircumscribedCircle:
    if not circles:
        return CircumscribedCircle()

    sinus = [math.sin(i * PI180) for i in range(360)]
    cosinus = [math.cos(i * PI180) for i in range(360)]

    circumscribed_circle = CircumscribedCircle(x=0.0, y=0.0)

    count = len(circles)
    circles[0].x = circumscribed_circle.x
    circles[0].y = circumscribed_circle.y
    circles[0].computed = True
    circumscribed_circle.radius = circles[0].radius

    for c in range(1, count):
        circle = circles[c]
        if circle.computed:
            return circumscribed_circle

        open_points: list[tuple[float, float]] = []

        for i in range(count):
            if not circles[i].computed:
                continue

            sum_of_radii = circle.radius + circles[i].radius
            for ang in range(360):
                pnt_x = circles[i].x + (cosinus[ang] * sum_of_radii)
                pnt_y = circles[i].y + (sinus[ang] * sum_of_radii)

                collision = False
                for j in range(count):
                    if circles[j].computed and dist(pnt_x, pnt_y, circles[j].x, circles[j].y) < (circle.radius + circles[j].radius):
                        collision = True
                        break
                if not collision:
                    open_points.append((pnt_x, pnt_y))

        if open_points:
            best_point = 0
            minimal_distance = dist(circumscribed_circle.x, circumscribed_circle.y, open_points[0][0], open_points[0][1])
            for i, point in enumerate(open_points):
                distance = dist(circumscribed_circle.x, circumscribed_circle.y, point[0], point[1])
                if distance < minimal_distance:
                    minimal_distance = distance
                    best_point = i

            circle.x = open_points[best_point][0]
            circle.y = open_points[best_point][1]

        circle.computed = True

        circumscribed_circle.x = 0.0
        circumscribed_circle.y = 0.0
        weights = 0.0
        for i in range(c + 1):
            weight = circles[i].radius * circles[i].radius
            weights += weight
            circumscribed_circle.x += circles[i].x * weight
            circumscribed_circle.y += circles[i].y * weight

        if weights != 0.0:
            circumscribed_circle.x /= weights
            circumscribed_circle.y /= weights

    move_x = circumscribed_circle.x
    move_y = circumscribed_circle.y
    circumscribed_circle.x = 0.0
    circumscribed_circle.y = 0.0
    circumscribed_circle.radius = 0.0

    for circle in circles:
        circle.x -= move_x
        circle.y -= move_y
        distance = _dist_from_center(circle.x, circle.y) + circle.radius
        if distance > circumscribed_circle.radius:
            circumscribed_circle.radius = distance

    circumscribed_circle.original_radius = circumscribed_circle.radius
    circumscribed_circle.area_of_circumscribed_circle = math.pi * circumscribed_circle.radius * circumscribed_circle.radius
    circumscribed_circle.minimum = circles[-1].radius
    circumscribed_circle.maximum = circumscribed_circle.radius * 2.0

    return circumscribed_circle
