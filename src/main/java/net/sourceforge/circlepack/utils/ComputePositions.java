// <editor-fold defaultstate="collapsed" desc="License">
/*
 * The MIT License
 *
 * Copyright 2012 Jan Moulis and Kamil Rendl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
// </editor-fold>

package net.sourceforge.circlepack.utils;

import java.util.ArrayList;

/**
 * Computing position for circles. Placing circles closest to the center from the largest to smallest.
 * 
 * @author Jan Moulis and Kamil Rendl
 * 
 */

public class ComputePositions {
	/** value of PI/180 */
	private static final double PI180 = Math.PI / 180d;

	/**
	 * Computing position for given circles.
	 * 
	 * @param circles
	 *            field of circles
	 * @return circumscribed circle
	 */
	public static CircumscribedCircle computePositionsCicleCloseToCenter(Circle[] circles) {
		// field of sinus values
		double[] sinus = new double[360];
		// field of cosinus values
		double[] cosinus = new double[360];
		// computing sinus and cosinus values
		for (int i = 0; i < 360; i++) {
			sinus[i] = Math.sin(i * PI180);
			cosinus[i] = Math.cos(i * PI180);
		}

		// creating circumscribed circle
		CircumscribedCircle circumscribedCircle = new CircumscribedCircle();
		// sets center of circumscribed circle to [0;0]
		circumscribedCircle.x = 0d;
		circumscribedCircle.y = 0d;

		// sets count of circles to length of the field
		int countOfCircles = circles.length;
		// computed circle
		Circle circle;
		// array of points, where can be placed circle
		ArrayList<Point> openPoints;

		// indexes
		int c, i, j;
		// state, whether collision occurs
		boolean collision;
		// angle
		int ang;
		// computed point
		Point pnt;
		// minimal distance
		double minimalDistance;
		// best point for placing circle
		int bestPoint;
		double sumOfRadii;
		double distance;

		// sets coordinates of first circle to center
		circles[0].x = circumscribedCircle.x;
		circles[0].y = circumscribedCircle.y;
		// circle is computed
		circles[0].computed = true;
		// sets radius of circumscribed circle to radius of first circle
		circumscribedCircle.radius = circles[0].radius;

		// go through all circles and compute all possible position for them
		for (c = 1; c < countOfCircles; c++) {
			circle = circles[c];
			collision = false;
			openPoints = new ArrayList<Point>();

			if (circle.computed)
				return circumscribedCircle;

			// go through all computed circle and compute possible possition for new circle
			for (i = 0; i < countOfCircles; i++) {
				if (circles[i].computed) {
					ang = 0;
					sumOfRadii = circle.radius + circles[i].radius;

					// computes 360 positions around computed circle
					for (ang = 0; ang < 360; ang++) {
						collision = false;
						pnt = new Point(circles[i].x + (cosinus[ang] * sumOfRadii), circles[i].y + (sinus[ang] * sumOfRadii));

						// checks collision
						for (j = 0; j < countOfCircles; j++) {
							if (circles[j].computed && !collision)
								if (Double.compare(dist(pnt.x, pnt.y, circles[j].x, circles[j].y), circle.radius + circles[j].radius) < 0)
									collision = true;
						}

						// adds point to open points, when is not collision
						if (!collision)
							openPoints.add(pnt);
					}
				}
			}

			// finds best positon for new circle
			minimalDistance = dist(circumscribedCircle.x, circumscribedCircle.y, openPoints.get(0).x, openPoints.get(0).y);
			bestPoint = 0;
			for (i = 0; i < openPoints.size(); i++) {
				distance = dist(circumscribedCircle.x, circumscribedCircle.y, openPoints.get(i).x, openPoints.get(i).y);

				if (Double.compare(distance, minimalDistance) < 0) {
					minimalDistance = distance;
					bestPoint = i;
				}
			}
			// sets coordinates of circle only when there are some points
			if (openPoints.size() != 0) {
				circle.x = openPoints.get(bestPoint).x;
				circle.y = openPoints.get(bestPoint).y;
			}
			// sets circle as computed
			circle.computed = true;

			// computes new center for circumscribed circle
			circumscribedCircle.x = 0d;
			circumscribedCircle.y = 0d;
			double weights = 0d;
			double weight = 0d;
			for (i = 0; i <= c; i++) {
				weight = circles[i].radius * circles[i].radius;

				weights += weight;
				circumscribedCircle.x += circles[i].x * weight;
				circumscribedCircle.y += circles[i].y * weight;
			}
			circumscribedCircle.x /= weights;
			circumscribedCircle.y /= weights;
		}

		// moves circumscribed circle to origin [0;0] and moves also all circles
		distance = 0d;
		double moveX = circumscribedCircle.x;
		double moveY = circumscribedCircle.y;
		circumscribedCircle.x = 0d;
		circumscribedCircle.y = 0d;
		circumscribedCircle.radius = 0d;
		for (c = 0; c < countOfCircles; c++) {
			circles[c].x -= moveX;
			circles[c].y -= moveY;

			distance = distFromCenter(circles[c].x, circles[c].y) + circles[c].radius;

			if (Double.compare(distance, circumscribedCircle.radius) > 0)
				circumscribedCircle.radius = distance;
		}

		// sets parameters of circumscribed circle
		circumscribedCircle.originalRadius = circumscribedCircle.radius;
		circumscribedCircle.countAreaOfCircumscribedCircle();
		circumscribedCircle.minimum = circles[countOfCircles - 1].radius;
		circumscribedCircle.maximum = circumscribedCircle.radius * 2;

		return circumscribedCircle;
	}

	/**
	 * Computes distance between two points given by coordinates.
	 * 
	 * @param x1
	 *            x coordinate of first point
	 * @param y1
	 *            y coordinate of first point
	 * @param x2
	 *            x coordinate of other point
	 * @param y2
	 *            y coordinate of other point
	 * @return distance
	 */
	public static double dist(double x1, double y1, double x2, double y2) {
		double tempX = x2 - x1;
		double tempY = y2 - y1;

		return Math.sqrt(tempX * tempX + tempY * tempY);
	}

	/**
	 * Computes distance between point and origin [0;0].
	 * 
	 * @param x
	 *            x coordinate of point
	 * @param y
	 *            y coordinate of point
	 * @return distance
	 */
	private static double distFromCenter(double x, double y) {
		return Math.sqrt(x * x + y * y);
	}
}
