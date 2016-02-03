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
public class Radians{
    private float value;
    public Radians(float value){
        this.value = value;
    }
    
    public float value(){
        return value;
    }

    //TODO isCcwOf... isCwOf
    
        //if ccwTanLineAngleToCenter is ccw of plantedCircles cw tangent
                    //flag need to skooch clockwise
                    //note ccw coliding circle.. if there is more than one, only consider nearest one
                //if cwTanLineAngleToCenter is cw of planteCircles ccw tangent
                    //flag needs to skooch counterclockwise
                    //note cw coliding circle .. if there is more than one, only consider nearest one
                //if !flags and !(colliding circles)
                    //drop circle tangent to biggest (as before)
                //if ccw flag and 1 colliding cicle 
                    //skooch cirlce cw to angle tanget with colliding cirlces cw angle from center
                //if cw flag and 1 colliding circle
                    //skooch circle ccw to angle tangent with colliding circles ccw angle from center
                //if both flags and one colliding circle
                    //this is a smaller circle whch will rest on top of a larger one
                //if both flags and two colliding circles
                    //circle will rest on top of colliding circles 
                    //(you have known side lenths of the triangle)
}
