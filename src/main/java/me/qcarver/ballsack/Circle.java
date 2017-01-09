/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.qcarver.ballsack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import static processing.core.PConstants.OPAQUE;
import processing.core.PApplet;

/**
 *
 * @author Quinn
 */
public class Circle {
        float transparency = OPAQUE; //255
        public float centerX, centerY, radius;
        public boolean makeCircle;
        public String name;
        Color c;

        protected static PApplet P = null;
        
        /**
         * Constructor for non-UI construction
         * @param name 
         */
        Circle(String name){
            this(0,0);
            this.name = name;      
        }

        Circle(float centerX, float centerY) {
            this.centerX = centerX;
            this.centerY = centerY;
            makeCircle = false;
            c = new Color(P.random(1), P.random(1), P.random(1));
        }
        
        public static void setPApplet(PApplet pApplet){
            P = pApplet;
        }

        public float diameter() {
            return 2 * radius;
        }

        public float area() {
            return (float) Math.PI * radius * radius;
        }

        public void draw() {
            P.fill(c.getRGB(), transparency);
            P.ellipse(centerX, centerY, radius, radius);
        }

        public void update(float targetX, float targetY, float radius) {
            this.radius = radius;
            centerX = targetX;
            centerY = targetY;
        }

        public void update(float targetX, float targetY) {
            centerX = targetX;
            centerY = targetY;
        }
    };
