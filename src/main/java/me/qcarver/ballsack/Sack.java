/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.qcarver.ballsack;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import static processing.core.PApplet.atan2;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.sin;

/**
 *
 * @author Quinn
 */
public class Sack extends Circle {
    //refrences to all the circles in the sack
    private List<Circle> circles = null;
    
    //references to the circles in order of descending radius size
    private TreeSet<Circle> descendingRadius = null;
    
    //references to the circles in order of distance from the center
    private TreeSet<Circle> ascendingDistance = null;
    
    //reference to the biggest circle in the sack
    private Circle biggest = null;
    
    private Sack(float centerX, float centerY) {
        super(centerX, centerY);
    }

    public Sack(Circle circle,
            List<Circle> circlesInSack) {
        this(circle.centerX, circle.centerY);
        radius = circle.radius;
        //in the GUI we represent the sack by being transparent
        transparency = 0;
        //init a bunch of npe'able member vars now
        circles = new ArrayList<Circle>();
        circles.addAll(circlesInSack);
        //circles sorted Biggest to Smallest
        descendingRadius = new TreeSet<Circle>(new DescendingRadius());
        descendingRadius.addAll(circles);
        //self explanatory
        biggest = descendingRadius.first();
        biggest.update(centerX, centerY);
        //all other circles sorted by distance from center circle.. 
        ascendingDistance = new TreeSet<Circle>(new DistanceToCenterCircle(biggest));
        ascendingDistance.addAll(circles);
        ascendingDistance.remove(biggest);
        //get the circles organized
        clusterContents();
        adjustRadiusAndCenterToContents();
    }
  
    public void draw() {
        super.draw();

        if (circles != null) {
            for (Circle circle : circles) {
                circle.draw();
            }
        }
    }

    public void update(float targetX, float targetY) {
        if (circles != null) {
            float deltaX = targetX - centerX;
            float deltaY = targetY - centerY;
            for (Circle circle : circles) {
                circle.update(
                        circle.centerX + deltaX, circle.centerY + deltaY);
            }
        }
        //order is impt -- if we did this first deltas would be zero
        super.update(targetX, targetY, radius);
    }
    
    //cluster circles inside this sack
    public void clusterContents() {
        //for each contained circle from closest to center out
        for (Circle circle : ascendingDistance) {
            //get the angle from center of biggest circle to this one
            float theta = angleToCircleFromCenter(circle);
            //find the distance from center of biggest circle to this one
            float rho = distanceFromCircleToCenter(circle);
            //update the circle to its new location (-1 dunno why but works)
            float targetX = (rho * -1) * cos(theta) + centerX;
            float targetY = (rho * -1) * sin(theta) + centerY;
            circle.update(targetX, targetY);
        }
    }
        
    /**
     * returns (rho) the angle from the middle of the biggest circle to a circle
     * @param circle the satelite circle of the biggest circle, passed in.
     * @return the angle in radians from the biggest circles center
     */
    public float angleToCircleFromCenter(Circle circle){
        return atan2(biggest.centerY - circle.centerY, 
                biggest.centerX - circle.centerX);
    }
    
    /**
     * returns (theta) distance from the middles of the big circle and another
     * @param circle
     * @return 
     */
    private float distanceFromCircleToCenter(Circle circle){
        return biggest.radius + circle.radius;
    }
    
    /**
     * Gets angle from big circles middle to a circle's counter clockwise edge
     * @param circle the circle to measure to the edge of
     * @return the angle from the center of the biggest circle to the ccw edge
     */
    private float ccwEclipsingAngle(Circle circle){
        return angleToCircleFromCenter(circle) - 
                (2f*(float)(Math.PI) - 
                (float)(Math.PI)/2f - 
                angleToCircleFromCenter(circle));       
    }
    
    /**
     * Gets angle from big circles middle to a circle's counter clockwise edge
     * @param circle the circle to measure to the edge of
     * @return the angle from the center of the biggest circle to the ccw edge
     */    
    private float cwEclipsingAngle(Circle circle){
        return angleToCircleFromCenter(circle) + 
                (2f*(float)(Math.PI) - 
                (float)(Math.PI)/2f - 
                angleToCircleFromCenter(circle));        
    }
    
    private void adjustRadiusAndCenterToContents(){
        float maxDist = biggest.diameter();
        float weightedXSum = 0, weightedYSum = 0, sumRadius = 0;
        //O(ln(n^2)) loop of circles compared to unique circles
        for (Circle circleA : circles){
            for (Circle circleB : circles){
                //symetric matrix only want to measure unique circles once
                if (circleA == circleB) break;
                //tally info needed to determine radius size
                float distAB = P.dist(  circleA.centerX,circleA.centerY,
                                        circleB.centerX, circleB.centerY);
                distAB += (circleA.radius + circleB.radius);
                maxDist = (maxDist > distAB)? maxDist : distAB;
                //tally info needed to determine center point (center of mass)
            }
            sumRadius += circleA.radius;
            weightedXSum += circleA.centerX * circleA.radius;
            weightedYSum += circleA.centerY * circleA.radius;
        }
        update(weightedXSum/sumRadius,weightedYSum/sumRadius,maxDist/2 + 2);
    }
}
