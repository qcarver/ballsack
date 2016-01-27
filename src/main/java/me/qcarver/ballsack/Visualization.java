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
        private List<Circle> circles;
        
        Circle(int startX, int startY){
            this.centerX = startX;
            this.centerY = startY;
            makeCircle = false;
            c = new Color(random(1),random(1), random(1));
        }
        
        public float diameter(){
            return 2*radius;
        }
        
        public void makeSack(List<Circle> circlesInSack){
            //in the GUI we represent the sack by being transparent
            transparency = 0;
            //these are the circles this sack is in charge of
            circles = new ArrayList<Circle>();
            circles = circlesInSack;
        }
        
        public float area(){
            return (float)Math.PI * radius * radius;
        }

        public void draw(){
            fill(c.getRGB(),transparency);
            ellipse(centerX, centerY, radius, radius);
            
            if (circles != null){
            for (Circle circle : circles){
                circle.draw();
            }
            }
        }
        
        public void update(float targetX, float targetY,float radius){  
           this.radius = radius; 
           update(targetX, targetY);
        }

        public void update(float targetX, float targetY){           
            if (circles != null){
                float deltaX = targetX - centerX;
                float deltaY = targetY - centerY;
                for (Circle circle : circles){
                    //circle.centerX += deltaX;
                    //circle.centerY += deltaY;
                    circle.update(circle.centerX + deltaX, circle.centerY + deltaY);
                }
            }
            centerX = targetX;
            centerY = targetY;        
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

    
    //cluster circles .. and make the current circle bag them
    public void cluster(float x, float y, List<Circle> circles){
        float maxRadius = 0;
        float avX = 0, avY = 0, sumRadius = 0;
        //Pick the biggest circle as the middle of the cluster
        Circle biggest = null;
        //for each contained circle .. (remember sorted by size decending)
        for (Circle circle : circles){
            //hueristic: things pack neater if we put big guy in middle
            if (biggest == null){
                circle.update(x,y);
                maxRadius = circle.radius + 2;
                biggest = circle;
            }
            else //the rest of the posse
            {
                //get the angle from center of biggest circle to this one
                float theta = atan2(y - circle.centerY, x - circle.centerX);
                //find the distance from center of biggest circle to this one
                float rho = biggest.radius + circle.radius;
                //update the circle to it's new location
                float targetX = (rho * -1) * cos(theta) + x;
                float targetY = (rho * -1) * sin(theta) + y;
                circle.update(targetX,targetY);
                //update the maxRadius of the containing circle
                maxRadius = (maxRadius < biggest.radius + circle.diameter() + 2)? 
                    biggest.radius + circle.diameter() + 2 : maxRadius;               
            }            
            //building up new data to guestimate new mid point of the cluster
            sumRadius +=circle.radius;
            avX += circle.centerX * circle.radius;
            avY += circle.centerY * circle.radius;            
        }
        //current circle becomes the containing sack for Circles
        currentCircle.update(avX/sumRadius,avY/sumRadius,maxRadius);
        currentCircle.makeSack(circles);
        
        //the sacked circles are no longer managed by this object
        this.circles.removeAll(circles);
    }
    
    public boolean contains(Circle container, Circle containee){
        return (dist(container.centerX, container.centerY,
                containee.centerX, containee.centerY)<=container.radius);
    }
}
