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
public class Visualization extends PApplet {
    int grayValue = 128;
    Circle currentCircle;

    List<Circle> circles;

    @Override
    public void setup() {
        //need to pass the Processing context to apps that use it
        Circle.setPApplet(this);
        
        //Typical Processing setup stuff
        size(1000, 800);
        background(grayValue);
        noFill();
        ellipseMode(RADIUS);

    }
    
    public void addCircle(Circle addMe){
        //if this is the first time we have used our list.. allocate for it       
            if (circles == null) {
                circles = new ArrayList<Circle>();
            }
            circles.add(addMe);
    }
    
    boolean runOnce = true;

    public void draw() {
        background(grayValue);
        
        if (runOnce){
            XmlVisualization xmlViz =         
                    new XmlVisualization(//"/Users/Quinn/Desktop/musicXml/cd_catalog.xml");
                            //"/Users/Quinn/Desktop/simpleXml/Plants.xml");
                            //"/Users/Quinn/Desktop/XmlTestData/NavigationStatusUpdate_ac110404-9777-89da-0000-00000001a3d7.xml");
            "/Users/Quinn/Desktop/XmlTestData/AlertSourceList_ac110404-9777-89da-0000-000000006395.xml");
            Circle docCircle = xmlViz.getCircle();
            docCircle.update(width/2, height/2);
            addCircle(docCircle);

            runOnce = false;
        }

        //draw previous circles
        if (circles != null) {
            for (Circle circle : circles) {
                circle.draw();
            }
        }

        if ((currentCircle != null) && (currentCircle.makeCircle)) {
            //draw the circle we are currently dragging out
            currentCircle.draw();
        }
    }

    public void mousePressed() {
        currentCircle = new Circle(mouseX, mouseY);
    }

    public void mouseDragged() {
        currentCircle.makeCircle = true;
        currentCircle.radius
                = dist(currentCircle.centerX, currentCircle.centerY, mouseX, mouseY);
    }

    public void mouseReleased() {
        //if mouse was released after dragging makeCircle will be true
        if (currentCircle.makeCircle == true) {
            //if this is the first time we have used our list.. allocate for it       
            if (circles == null) {
                circles = new ArrayList<Circle>();
            }
            //check and process for containing circles
            if (condense() == false){
                //archive the currentCircle
                circles.add(currentCircle);
            }
            //make way for a new current circle
            currentCircle.makeCircle = false;
            currentCircle = null;
        }
    }

    //if there are circles with this circle condense them all together
    public boolean condense() {      
        List<Circle> containedCircles = null;
        float newCenterX = 0, newCenterY = 0;
        for (Circle circle : circles) {
            if (contains(currentCircle, circle)) {
                if (containedCircles == null) {
                    containedCircles = new ArrayList<Circle>();
                }
                //keep summations of x and y positions to use in average later
                newCenterX += circle.centerX;
                newCenterY += circle.centerY;
                containedCircles.add(circle);
            }
        }

        if (containedCircles != null) {
            currentCircle.update(
                    //change the new circles position to be the average
                    newCenterX / containedCircles.size(),
                    newCenterY / containedCircles.size());
            Sack sack = new Sack(currentCircle, containedCircles);
            circles.removeAll(containedCircles);
            circles.add(sack);
        }        
        return (containedCircles != null);
    }

    public boolean contains(Circle container, Circle containee) {
        return (dist(container.centerX, container.centerY,
                containee.centerX, containee.centerY) <= container.radius);
    }
}
