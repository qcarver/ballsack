package me.qcarver.ballsack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import processing.core.PApplet;



/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Quinn
 */
public class Visualization extends PApplet{
 
    class Circle{
        public float centerX, centerY, radius;
        public boolean makeCircle ;
        Color c;
        float transparency = OPAQUE; //255
        Circle(int startX, int startY){
            this.centerX = startX;
            this.centerY = startY;
            makeCircle = false;
            c = new Color(random(1),random(1), random(1));
        }
        public void setSack(boolean isSack){
            if (isSack){
                transparency = 0;
            }
        }

        public void draw(){
            fill(c.getRGB(),transparency);
            ellipse(centerX, centerY, radius, radius);
        }

        public void update(float targetX, float targetY,float radius){
            centerX = targetX;
            centerY = targetY;
            this.radius = radius;
        }
    };
    
    int grayValue = 128;
    Circle currentCircle;
    
    List<Circle> circles;

    public void setup() {
        size(500,400);
        background(grayValue);
        noFill();
        ellipseMode(RADIUS);
        
    }

    public void draw() {
        background(grayValue);
        
        //draw previous circles
        if (circles != null){
            for (Circle circle : circles){
                circle.draw();
            }  
        }
            
        if ((currentCircle != null) && (currentCircle.makeCircle)){
            //draw the circle we are currently dragging out
            currentCircle.draw();
            
        }
    }
    
    public void mousePressed(){
        currentCircle = new Circle(mouseX, mouseY);
    }
    
    public void mouseDragged(){
        currentCircle.makeCircle = true;
        currentCircle.radius =
                dist(currentCircle.centerX, currentCircle.centerY,mouseX, mouseY);
       
    }
    
    public void mouseReleased(){
        //if mouse was released after dragging makeCircle will be true
        if (currentCircle.makeCircle == true){
            //if this is the first time we have used our list.. allocate for it       
            if(circles == null){
                circles = new ArrayList<Circle>();
            }
            //check and process for containing circles
            condense();
            
            //archive the currentCircle
            circles.add(currentCircle);
  
            //make way for a new current circle
            currentCircle.makeCircle = false;
            currentCircle = null; 
        }
    }
    
    //if there are circles with this circle condense them all together
    public void condense(){
        List<Circle> containedCircles = null;
        float newCenterX = 0, newCenterY = 0;
        for (Circle circle : circles){
            if (contains(currentCircle, circle)){
                if (containedCircles == null){
                    containedCircles = new ArrayList<Circle>();
                }
                newCenterX += circle.centerX;
                newCenterY += circle.centerY;
                containedCircles.add(circle);
            }
        }
        
        if (containedCircles != null){
            cluster (newCenterX/containedCircles.size(),
                    newCenterY/containedCircles.size(),
                    containedCircles);
        }
    }

    
    //cluster circles around a point .. and make the current circle bag them
    public void cluster(float x, float y, List<Circle> circles){
        float maxRadius = 0;
        //for each contained circle
        for (Circle circle : circles){
            //get the angle x,y to circle center
            float theta = atan2(y - circle.centerY, x - circle.centerX);
            //find the new circle mid-point which is radius away from the x,y
            float rho = circle.radius;
            //update the circle
            float targetX = (rho*-1) * cos(theta) + x;
            float targetY = (rho*-1) * sin(theta) + y;
            circle.update(targetX,targetY,rho);
            //update the maxRadius of the containing circle
            maxRadius = (maxRadius < rho)? rho * 2 + 10 : maxRadius;
        }
        currentCircle.update(x,y,maxRadius);
        currentCircle.setSack(true);
    }
    
    public boolean contains(Circle container, Circle containee){
        return (dist(container.centerX, container.centerY,
                containee.centerX, containee.centerY)<=container.radius);
    }
}
