/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.qcarver.ballsack;

import java.util.Comparator;

/**
 *
 * @author Quinn
 */
public class DistanceToCenterCircle implements Comparator<Circle> {
    Circle centerCircle;
    
    //hide private c'tor, this method comparator needs the centerCircle
    private DistanceToCenterCircle(){};

    public DistanceToCenterCircle(Circle centerCircle) {
        this.centerCircle = centerCircle;
    }
    
    //Sorts in order of ascending distance from circle centers
    @Override
    public int compare(Circle circle1, Circle circle2) {
        //multiplied to avoid lossiness in conversion to int
        return distanceFrom(circle1) - distanceFrom(circle2);
    }
    
    private int distanceFrom(Circle circle){
        //multiplied to avoid lossiness in conversion to int
        return (int) Math.sqrt(
            Math.pow(Math.abs(circle.centerX - centerCircle.centerX),2)+ 
            Math.pow(Math.abs(circle.centerY - centerCircle.centerY),2)
        ) * 1000;
    } 
};
