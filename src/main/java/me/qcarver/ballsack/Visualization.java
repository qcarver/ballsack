package me.qcarver.ballsack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
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
        
        public float diameter(){
            return 2*radius;
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
    
    public static Comparator<Circle> LargestFirstComparator 
            = new Comparator<Circle>(){
                //Sorts in order of deceding radius size
                public int compare(Circle circle1, Circle circle2){
                    //multiplied to avoid lossiness in conversion to int
                    return (int)(circle2.radius * 10000f) -
                            (int)(circle1.radius * 10000f);
                }
            };
    
    int grayValue = 128;
    Circle currentCircle;
            
    TreeSet<Circle> circles;

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
                circles = new TreeSet<Circle>(LargestFirstComparator);
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
        Circle bigGuy = null;
        //for each contained circle .. (remember sorted by size decending)
        for (Circle circle : circles){
            float rho = circle.radius;
            if (bigGuy == null){
                circle.centerX = x;
                circle.centerY = y;
                bigGuy = circle;
            }
            else //the big guys' posse
            {
                //get the angle x,y to circle center
                float theta = atan2(y - circle.centerY, x - circle.centerX);
                //find the new circle mid-point which is radius away from the x,y
                
                //update the circle
                float targetX = (rho - bigGuy.radius *-1) * cos(theta) + x;
                float targetY = (rho - bigGuy.radius *-1) * sin(theta) + y;
                circle.update(targetX,targetY,rho);
            }
            //update the maxRadius of the containing circle
            maxRadius = (maxRadius < bigGuy.radius + circle.diameter() + 2)? 
                    bigGuy.radius + circle.diameter() + 2 : maxRadius;
        }
        currentCircle.update(x,y,maxRadius);
        currentCircle.setSack(true);
    }
    
    public boolean contains(Circle container, Circle containee){
        return (dist(container.centerX, container.centerY,
                containee.centerX, containee.centerY)<=container.radius);
    }
}
