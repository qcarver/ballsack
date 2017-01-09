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

import org.jbox2d.common.Vec2;

/**
 * Representation of circumscribed circle in program. It containts coordinates of center, radius and area of circumcircle and parts of circumscribed circle, that are used in simulation. Because JBox2D
 * has not simulation area as a cirlce, this circumscribed circle is made of lines.
 * 
 * @author Jan Moulis and Kamil Rendl
 * 
 */

public class CircumscribedCircle {
	/** enlargement of circumcircle to area of simulation */
	private final float ENLARGEMENT = 2f;
	/** number of parts of circumscribed circle */
	private final int COUNTOFLINES = 32;
	/** sinus value in radians for one part of circumscribed circle */
	private final double SINA = Math.sin((2 * Math.PI) / COUNTOFLINES);
	/** cosinus value in radians for one part of circumscribed circle */
	private final double COSA = Math.cos((2 * Math.PI) / COUNTOFLINES);
	/** acting force to circumscribed circle while diminution and augmentation */
	private final float FORCE = 300f;

	/** area of simulation */
	private CircumscribedCircle simulationArea;

	/** x coordinates of center */
	public double x;
	/** y coordinates of center */
	public double y;
	/** radius of circumscribed circle */
	public double radius;
	/** original radius of circumscribed circle before simulation */
	public double originalRadius;
	/** area of circumscribed circle */
	public double areaOfCircumscribedCircle;
	/** minimum radius of circumscribed circle (smallest radius or circle in program) */
	public double minimum;
	/** maximum radius of circumscribed circle (2*radius) */
	public double maximum;

	/** parts of circumscribed circle (lines) */
	public CircumscribedCirclePart[] circumscribedCircleParts;

	/**
	 * Constructor of circumscribed circle.
	 */
	public CircumscribedCircle() {
	}

	/**
	 * Constructor of circumscribed circle.
	 * 
	 * @param radius
	 *            radius of circumscribed circle
	 */
	public CircumscribedCircle(double radius) {
		this.radius = radius;
		originalRadius = radius;
	}

	/**
	 * Method for creating objects for simulation. Method computes begin and end vertexes of lines which make together circumscribed circle.
	 */
	public void createObjectForSimulation() {
		// create field of lines
		circumscribedCircleParts = new CircumscribedCirclePart[COUNTOFLINES];

		// begin position of circumscribed circle
		Vec2 beginPos = new Vec2((float) (x + ENLARGEMENT * originalRadius * Utils.worldScale), (float) y);
		Vec2 endPos = new Vec2();

		// computes vertexes of circumscribed circle
		for (int l = 0; l < COUNTOFLINES; l++) {
			endPos.x = (float) (x + COSA * (beginPos.x - x) + SINA * (beginPos.y - y));
			endPos.y = (float) (y - SINA * (beginPos.x - x) + COSA * (beginPos.y - y));

			circumscribedCircleParts[l] = new CircumscribedCirclePart(new Vec2(beginPos), new Vec2(endPos));

			beginPos.x = endPos.x;
			beginPos.y = endPos.y;
		}

		// because circumscribed circle is computed enlarged, this for moves all lines to appropriate distance from center (radius)
		for (int l = 0; l < COUNTOFLINES; l++) {
			circumscribedCircleParts[l].body.setTransform(new Vec2((float) (circumscribedCircleParts[l].centerPos.x - x), (float) (circumscribedCircleParts[l].centerPos.y - y)).mul(1 / ENLARGEMENT),
					circumscribedCircleParts[l].body.getAngle());
		}

		// creating area of simulation
		createSimulationArea();
	}

	/**
	 * Creating area of simulation. Radius of this area is 2*radius of circumscribed circle. Method computes begin and end vertexes of lines which make together area of simulation.
	 */
	private void createSimulationArea() {
		// creating area of simulation
		simulationArea = new CircumscribedCircle(maximum);
		// create field of lines
		simulationArea.circumscribedCircleParts = new CircumscribedCirclePart[COUNTOFLINES];

		// begin position for area of simulation
		Vec2 beginPos = new Vec2((float) (x + simulationArea.radius * Utils.worldScale), (float) y);
		Vec2 endPos = new Vec2();

		// computes vertexes of area of simulation
		for (int l = 0; l < COUNTOFLINES; l++) {
			endPos.x = (float) (x + COSA * (beginPos.x - x) + SINA * (beginPos.y - y));
			endPos.y = (float) (y - SINA * (beginPos.x - x) + COSA * (beginPos.y - y));

			simulationArea.circumscribedCircleParts[l] = new CircumscribedCirclePart(new Vec2(beginPos), new Vec2(endPos), true);

			beginPos.x = endPos.x;
			beginPos.y = endPos.y;
		}
	}

	/**
	 * Application force to all parts of circumscribed circle with direction from line to center of circumscribed circle. Result is, that circumscribed circle gets smaller.
	 */
	public void smaller() {
		// apply force to all parts of circumscribed circle
		for (int l = 0; l < COUNTOFLINES; l++)
			circumscribedCircleParts[l].body.applyForce(new Vec2((float) (x - circumscribedCircleParts[l].centerPos.x), (float) (y - circumscribedCircleParts[l].centerPos.y)).mul(FORCE),
					circumscribedCircleParts[l].body.getPosition());

		// when radius is smaller than minimum, stop changing size of circumscribed circle
		if (Double.compare(radius, minimum) < 0) {
			stopChangingSize();
		}
	}

	/**
	 * Application force to all parts of circumscribed circle with direction from center of circumscribed cirlce to line. Result is, that circumscribed circle gets bigger.
	 */
	public void bigger() {
		// apply forcle only when radius is smaller than maximum
		if (Double.compare(radius, maximum) < 0) {
			// apply force to all parts of circumscribed circle
			for (int l = 0; l < COUNTOFLINES; l++)
				circumscribedCircleParts[l].body.applyForce(new Vec2((float) (circumscribedCircleParts[l].centerPos.x - x), (float) (circumscribedCircleParts[l].centerPos.y - y)).mul(FORCE),
						circumscribedCircleParts[l].body.getPosition());
		}
	}

	/**
	 * Stops changing size of circumscribed circle.
	 */
	public void stopChangingSize() {
		// sets velocity to all parts to zero
		for (int l = 0; l < COUNTOFLINES; l++)
			circumscribedCircleParts[l].body.setLinearVelocity(new Vec2(0f, 0f));
	}

	/**
	 * Change shape of circumscribed circle to circle (some parts can move and some can not).
	 */
	public void setAproprieteRadius() {
		double lengthX;
		double lengthY;
		double length;
		double actualRadius;
		double maxRadius = 0d;

		// set maximal lenght (radius) from line to center
		for (int l = 0; l < COUNTOFLINES; l++) {
			lengthX = circumscribedCircleParts[l].body.getPosition().x - x;
			lengthY = circumscribedCircleParts[l].body.getPosition().y - y;
			length = Math.sqrt(lengthX * lengthX + lengthY * lengthY);
			actualRadius = length / Math.cos(Math.PI / COUNTOFLINES);

			if (Double.compare(actualRadius, maxRadius) > 0)
				maxRadius = actualRadius;
		}

		// when radius is bigger than maximal radius, set radius to max value
		if (Double.compare(maxRadius / Utils.worldScale, maximum) > 0)
			maxRadius = maximum * Utils.worldScale;

		// recomputes center coordinates of lines
		Vec2 beginPos = new Vec2((float) (x + maxRadius), (float) y);
		Vec2 endPos = new Vec2((float) (x + COSA * (beginPos.x - x) + SINA * (beginPos.y - y)), (float) (y - SINA * (beginPos.x - x) + COSA * (beginPos.y - y)));
		Vec2 centerPos;

		for (int l = 0; l < COUNTOFLINES; l++) {
			endPos.x = (float) (x + COSA * (beginPos.x - x) + SINA * (beginPos.y - y));
			endPos.y = (float) (y - SINA * (beginPos.x - x) + COSA * (beginPos.y - y));

			centerPos = new Vec2((beginPos.x + endPos.x) / 2, (beginPos.y + endPos.y) / 2);
			Vec2 direction = new Vec2(circumscribedCircleParts[l].body.getPosition().add(centerPos.sub(circumscribedCircleParts[l].body.getPosition())));
			circumscribedCircleParts[l].body.setTransform(direction, circumscribedCircleParts[l].body.getAngle());

			beginPos.x = endPos.x;
			beginPos.y = endPos.y;
		}
	}

	/**
	 * Computes radius and area of circumscribed circle.
	 */
	public void countRadiusAndAreaOfCircumscribedCircle() {
		double lengthX = circumscribedCircleParts[0].body.getPosition().x / Utils.worldScale - x;
		double lengthY = circumscribedCircleParts[0].body.getPosition().y / Utils.worldScale - y;
		double length = Math.sqrt(lengthX * lengthX + lengthY * lengthY);

		radius = length / Math.cos(Math.PI / COUNTOFLINES);

		countAreaOfCircumscribedCircle();
	}

	/**
	 * Computes area of circumscribed circle.
	 */
	public void countAreaOfCircumscribedCircle() {
		areaOfCircumscribedCircle = Math.PI * radius * radius;
	}
}
