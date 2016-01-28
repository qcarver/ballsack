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
        cluster();
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
    public void cluster() {
        float maxRadius = 0;
        //used to guesimate the new center of the cluster
        float avX = 0, avY = 0, sumRadius = 0;
        //Pick the biggest circle as the middle of the cluster
        Circle biggest = null;
        //Pick the next biggest circle to help us figure sack radius
        Circle veep = null;
        //for each contained circle .. (remember sorted by size decending)
        for (Circle circle : circles) {
            //hueristic: things pack neater if we put big guy in middle
            if (biggest == null) {
                circle.update(centerX, centerY);
                maxRadius = circle.radius + 2;
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
                //update our (the sack's) radius --HACK assumes posse only is one ring thick!
                if (veep == null){
                    maxRadius = biggest.radius + circle.diameter() + 2;
                    veep = circle;
                }
            }
            //building up new data to guestimate new mid point of the cluster
            sumRadius += circle.radius;
            avX += circle.centerX * circle.radius;
            avY += circle.centerY * circle.radius;
        }
        update(avX/sumRadius,avY/sumRadius,maxRadius);
    }
}
