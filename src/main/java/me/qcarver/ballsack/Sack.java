/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.qcarver.ballsack;

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
    private TreeSet<Circle> circles;

    private Sack(float centerX, float centerY) {
        super(centerX, centerY);
    }

    public Sack(Circle circle,
            List<Circle> circlesInSack) {
        this(circle.centerX, circle.centerY);
        radius = circle.radius;
        //in the GUI we represent the sack by being transparent
        transparency = 0;
        //these are the circles this sack is in charge of
        circles = new TreeSet<Circle>(new DescendingRadius());
        circles.addAll(circlesInSack);
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
        //Pick the biggest circle as the middle of the clusteContents
        Circle biggest = null;
        //for each contained circle .. (remember sorted by size decending)
        for (Circle circle : circles) {
            //hueristic: things pack neater if we put big guy in middle
            if (biggest == null) {
                circle.update(centerX, centerY);
                biggest = circle;
            } else //the rest of the posse
            {
                //get the angle from center of biggest circle to this one
                float theta = atan2(centerY - circle.centerY, centerX - circle.centerX);
                //find the distance from center of biggest circle to this one
                float rho = biggest.radius + circle.radius;
                //update the circle to its new location (-1 dunno why but works)
                float targetX = (rho * -1) * cos(theta) + centerX;
                float targetY = (rho * -1) * sin(theta) + centerY;
                circle.update(targetX, targetY);
            }
        }
    }
    
    private void adjustRadiusAndCenterToContents(){
        float maxDist = circles.first().diameter();
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
