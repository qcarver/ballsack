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
public class DescendingRadius implements Comparator<Circle> {
    //Sorts in order of desceding radius size
    public int compare(Circle circle1, Circle circle2) {
        //multiplied to avoid lossiness in conversion to int
        return (int) (circle2.radius * 10000f)
                - (int) (circle1.radius * 10000f);
    }
};
