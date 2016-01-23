package me.qcarver.ballsack;
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
        public int startX, startY, endX, endY;
        public boolean makeCircle ;
        Circle(int startX, int startY){
            this.startX = startX;
            this.startY = startY;
            makeCircle = false;
        }
        public float getCircleSize(){
            return (endX - startX + endY - startY) * 1.2f;
        }
        public void draw(){
            ellipse(startX, startY, getCircleSize(), getCircleSize());
        }
    };
    
    int grayValue = 128;
    Circle currentCircle;
    
    List<Circle> circles;

    public void setup() {
        size(500,400);
        background(grayValue);
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
        currentCircle.endX = mouseX;
        currentCircle.endY = mouseY;
    }
    
    public void mouseReleased(){
        //if mouse was released after dragging makeCircle will be true
        if (currentCircle.makeCircle == true){
            //if this is the first time we have used our list.. allocate for it       
            if(circles == null){
                circles = new ArrayList<Circle>();
            }
            //archive the currentCircle
            circles.add(currentCircle);
  
            //make way for a new current circle
            currentCircle.makeCircle = false;
            currentCircle = null; 
        }
    }
}
