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

/**
 * Representation of point, that is used for computing position for circles.
 * 
 * @author Jan Moulis and Kamil Rendl
 * 
 */

public class Point {
	/** x coordinates */
	double x;
	/** y coordinates */
	double y;

	/**
	 * Constructor of point.
	 */
	public Point() {
	}

	/**
	 * Constructor of point.
	 * 
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 */
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
}
