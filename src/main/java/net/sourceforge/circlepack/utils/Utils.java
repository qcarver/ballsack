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

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

/**
 * Utils for JBox2D physical simulation.
 * 
 * @author Jan Moulis and Kamil Rendl
 * 
 */

public class Utils {
	// JBox2D categories for collision filtering
	/** category for area of simulation */
	private static final short simulationAreaCategory = 0x0001;
	/** category for parts of circumscribed circle */
	private static final short circumscribedCirclePartsCategory = 0x0002;
	/** category for circles */
	private static final short circleCategory = 0x0004;

	// JBox2D masks for collision filtering
	/** mask for area of simulation */
	private static final short simulationAreaMask = circleCategory;
	/** mask for patrs of circumscribed circle */
	private static final short cicrumscribedCirclePartsMask = circleCategory;
	/** mask for circles */
	private static final short circleMask = simulationAreaCategory | circumscribedCirclePartsCategory | circleCategory;

	/** JBox2D simulation world */
	public static World world;
	/** value of world scale */
	public static float worldScale = 1f;

	/**
	 * Inicialization of JBox2d. Creating JBox2D world and computing world scale.
	 * 
	 * @param maximumRadius
	 *            radius of biggest cirlce
	 */
	public static void initializeWorld(double maximumRadius) {
		world = new World(new Vec2(0f, 0f), false);
		Settings.velocityThreshold = 0f;
		worldScale = (float) (10 / maximumRadius);
	}

	/**
	 * Creates static JBox2D body for part of area of simulation.
	 * 
	 * @param beginPos
	 *            begin position of line
	 * @param endPos
	 *            end position of line
	 * @param centerPos
	 *            center position of line
	 * @return JBox2D body
	 */
	public static Body createSimulationAreaPart(Vec2 beginPos, Vec2 endPos, Vec2 centerPos) {
		PolygonShape ps = new PolygonShape();
		ps.setAsEdge(new Vec2(beginPos.sub(centerPos)), new Vec2(endPos.sub(centerPos)));

		FixtureDef fd = new FixtureDef();
		fd.shape = ps;
		fd.density = 1f;
		fd.filter.categoryBits = simulationAreaCategory;
		fd.filter.maskBits = simulationAreaMask;

		BodyDef bd = new BodyDef();
		bd.type = BodyType.STATIC;
		bd.position = centerPos;

		Body body = world.createBody(bd);
		body.createFixture(fd);
		body.setFixedRotation(true);

		return body;
	}

	/**
	 * Creates dynamic JBox2D body for part of circumscribed circle.
	 * 
	 * @param beginPos
	 *            begin position of line
	 * @param endPos
	 *            end position of line
	 * @param centerPos
	 *            center position of line
	 * @return JBox2D body
	 */
	public static Body createCircumscribedCirclePart(Vec2 beginPos, Vec2 endPos, Vec2 centerPos) {
		PolygonShape ps = new PolygonShape();
		ps.setAsEdge(new Vec2(beginPos.sub(centerPos)), new Vec2(endPos.sub(centerPos)));

		FixtureDef fd = new FixtureDef();
		fd.shape = ps;
		fd.density = 1f;
		fd.friction = 1f;
		fd.filter.categoryBits = circumscribedCirclePartsCategory;
		fd.filter.maskBits = cicrumscribedCirclePartsMask;

		BodyDef bd = new BodyDef();
		bd.type = BodyType.DYNAMIC;
		bd.position = centerPos;

		MassData md = new MassData();
		md.mass = 20f;

		Body body = world.createBody(bd);
		body.createFixture(fd);
		body.setFixedRotation(true);
		body.setMassData(md);

		return body;
	}

	/**
	 * Creates dynamic JBox2D body for circle.
	 * 
	 * @param x
	 *            x coordinate of center of circle
	 * @param y
	 *            y coordinate of center of circle
	 * @param radius
	 *            radius of circle
	 * @return JBox2D body
	 */
	public static Body createCircleBody(double x, double y, double radius) {
		BodyDef bd = new BodyDef();
		bd.type = BodyType.DYNAMIC;
		bd.position.set((new Vec2((float) x, (float) y)).mul(worldScale));

		CircleShape cs = new CircleShape();
		cs.m_radius = (float) (radius * worldScale);

		FixtureDef fd = new FixtureDef();
		fd.shape = cs;
		fd.density = 0.3f;
		fd.friction = 0.1f;
		fd.restitution = 0.1f;
		fd.filter.categoryBits = circleCategory;
		fd.filter.maskBits = circleMask;

		Body body = world.createBody(bd);
		body.createFixture(fd);

		return body;
	}

	/**
	 * Add force to center [0;0] to given body. Force is proportional to radius.
	 * 
	 * @param body
	 *            JBox2D body
	 * @param radius
	 *            radius of circle
	 */
	public static void applyCentralForceToCircleBody(Body body, double radius) {
		Vec2 vectorToOrigin = new Vec2(body.getPosition().negate());
		Vec2 force = vectorToOrigin.mul((float) (radius) * worldScale * vectorToOrigin.length());

		body.applyForce(force, body.getPosition());
	}

	/**
	 * Add force to given body. Force is proportional to radius.
	 * 
	 * @param body
	 *            JBox2D body
	 * @param radius
	 *            radius of circle
	 * @param x
	 *            x coordinate of force direction
	 * @param y
	 *            y coordinate of force direction
	 */
	public static void applyVectorForceToCircleBody(Body body, double radius, float x, float y) {
		Vec2 direction = new Vec2(x, y);
		Vec2 forceVector = direction.mul((float) (radius * worldScale * direction.length()));

		body.applyForce(forceVector, body.getPosition());
	}

	/**
	 * Sets velocity of body to zero, stops moving.
	 * 
	 * @param body
	 *            JBox2D body
	 */
	public static void stopMoving(Body body) {
		body.setLinearVelocity(new Vec2(0f, 0f));
	}

	/**
	 * Computes if given circle contains given point.
	 * 
	 * @param circle
	 *            tested circle
	 * @param x
	 *            x coordinate of tested point
	 * @param y
	 *            y coordinate of tested point
	 * @return circle contains point or does not
	 */
	public static boolean doesCircleContainPoint(Circle circle, double x, double y) {
		double length;
		if (circle.body != null)
			length = ComputePositions.dist(x, y, circle.body.getPosition().x / worldScale, circle.body.getPosition().y / worldScale);
		else
			length = ComputePositions.dist(x, y, circle.x, circle.y);

		return (Double.compare(length, circle.radius) < 0);
	}

	/**
	 * Checks if all circles are inside circumscribed circle, if not, circle is moved inside.
	 * 
	 * @param circles
	 *            field of circles
	 * @param radiusOfCircumscribedCircle
	 *            radius of circumscribed circle
	 */
	public static void areCirclesInsideCircumcircle(Circle[] circles, double radiusOfCircumscribedCircle) {
		for (int c = 0; c < circles.length; c++) {
			double lengthFromOrigin = Math.sqrt(circles[c].body.getPosition().x * circles[c].body.getPosition().x + circles[c].body.getPosition().y * circles[c].body.getPosition().y);

			if (Double.compare(lengthFromOrigin, radiusOfCircumscribedCircle * worldScale) >= 0) {
				Vec2 toCenter = new Vec2(-circles[c].body.getPosition().x, -circles[c].body.getPosition().y);
				toCenter.normalize();
				toCenter.mul((float) (lengthFromOrigin - radiusOfCircumscribedCircle * worldScale + circles[c].radius * worldScale));

				Vec2 transform = new Vec2(circles[c].body.getPosition().x + toCenter.x, circles[c].body.getPosition().y + toCenter.y);
				System.out.println(transform);

				circles[c].body.setTransform(transform, circles[c].body.getAngle());
				stopMoving(circles[c].body);
			}
		}
	}
}
