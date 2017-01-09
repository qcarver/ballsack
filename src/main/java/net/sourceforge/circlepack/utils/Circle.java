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

import org.jbox2d.dynamics.Body;

/**
 * Representation of one circle in program.
 * It contains coordinates of center, radius of circle and body for physical simulation.
 * 
 * @author Jan Moulis and Kamil Rendl
 * 
 */

public class Circle {
	/** x coordinate of center */
	public double x;
	/** y coordinate of center */
	public double y;
	/** radius of circle */
	public double radius;
	/** state, whether coordinates of center of cirlce are computed */
	public boolean computed;
	/** JBox2D body for physical simulation */
	public Body body;

	/**
	 * Constructor for class Circle.
	 */
	public Circle() {
		computed = false;
	}

	/**
	 * Constructor for class Circle.
	 * 
	 * @param radius
	 *            radius of created circle
	 */
	public Circle(double radius) {
		this.radius = radius;
		computed = false;
	}

	/**
	 * Constructor for class Circle.
	 * 
	 * @param x
	 *            x coordinate of center of circle
	 * @param y
	 *            y coordinate of center of circle
	 * @param radius
	 *            radius of created circle
	 */
	public Circle(double x, double y, double radius) {
		this.x = x;
		this.y = y;
		this.radius = radius;
		computed = false;
	}

	/**
	 * Method for creating JBox2D body for simulation.
	 */
	public void createObjectForSimulation() {
		body = Utils.createCircleBody(x, y, radius);
	}
}
