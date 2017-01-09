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

import java.awt.Color;

/**
 * Color schema for coloring circles with different radius. Color schema is computed from HSB color space (variation of HSV). BLUE-CYAN-GREEN-YELLOW-RED
 * 
 * @author Jan Moulis and Kamil Rendl
 * 
 */

public class ColorSchema {
	/** count of colors in schema */
	private static final int COUNTOFCOLORS = 128;
	/** field of computed colors */
	private Color[] colors;
	/** minimal value for index 0 in field of colors */
	private double min;
	/** maximal value for last index of field of colors */
	private double max;

	/**
	 * Constructor of color schema.
	 * 
	 * @param min
	 *            minimal value
	 * @param max
	 *            maximal value
	 */
	public ColorSchema(double min, double max) {
		this.min = min;
		this.max = max;

		// creating field of colors
		colors = new Color[COUNTOFCOLORS];
		// computing colors
		for (int i = 0; i < COUNTOFCOLORS; i++) {
			colors[COUNTOFCOLORS - 1 - i] = Color.getHSBColor(270f / 360f * (float) i / (float) COUNTOFCOLORS, 1f, 1f);
		}
	}

	/**
	 * Get color from field of colors for given radius.
	 * 
	 * @param radius
	 *            radius of circle
	 * @return color for given radius
	 */
	public Color getColor(double radius) {
		int index = (int) (COUNTOFCOLORS * (radius - min) / max);

		// checks index
		if (index < 0)
			index = 0;
		if (index > COUNTOFCOLORS - 1)
			index = COUNTOFCOLORS - 1;

		return colors[index];
	}
}
