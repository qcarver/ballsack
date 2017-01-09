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
import org.jbox2d.dynamics.Body;

/**
 * Representation of one part (line) of circumscribed circle.
 * 
 * @author Jan Moulis and Kamil Rendl
 * 
 */
public class CircumscribedCirclePart {
	/** JBox2D body for physical simulation */
	public Body body;
	/** begin, end and center position (coordinates) of line */
	public Vec2 beginPos, endPos, centerPos;

	/**
	 * Constructor of part of circumscribed circle.
	 * 
	 * @param beginPos
	 *            coordinates of begin vertex
	 * @param endPos
	 *            coordinates of end vertex
	 */
	public CircumscribedCirclePart(Vec2 beginPos, Vec2 endPos) {
		this.beginPos = beginPos;
		this.endPos = endPos;
		// computes center position for this part
		centerPos = new Vec2((beginPos.x + endPos.x) / 2, (beginPos.y + endPos.y) / 2);

		// creates JBox2D body for physical simulation
		body = Utils.createCircumscribedCirclePart(beginPos, endPos, centerPos);
	}

	/**
	 * Constructor of part of area of simulation.
	 * 
	 * @param beginPos
	 *            coordinates of begin vertex
	 * @param endPos
	 *            coordinates of end vertex
	 * @param simulationArea
	 *            this part is component of area of simulation
	 */
	public CircumscribedCirclePart(Vec2 beginPos, Vec2 endPos, boolean simulationArea) {
		this.beginPos = beginPos;
		this.endPos = endPos;
		// computes center position for this part
		centerPos = new Vec2((beginPos.x + endPos.x) / 2, (beginPos.y + endPos.y) / 2);

		// creates JBox2D body for physical simulation
		body = Utils.createSimulationAreaPart(beginPos, endPos, centerPos);
	}
}
