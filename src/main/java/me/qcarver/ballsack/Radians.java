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
public class Radians implements Comparable{
    float pi = (float)(Math.PI);
    private float value;
    public Radians(float value){
        this.value = value;
    }
    
    public float value(){
        return value;
    }
    
    /**
     * Returns the absolute angle in radians opposite of the member value
     * @return the angle opposite of the member value
     */
    private float oppositeAngle(){
        float oppositeAngle = value - pi;
        //Adj for case where 0/2Pi is between value and opposite angle
        if (oppositeAngle < 0f){
            oppositeAngle = 2*pi - oppositeAngle;
        }
        return oppositeAngle;
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

    @Override
    public int compareTo(Object o) {
        Radians other = (Radians)o;
        float adj = 0;
        //if the comparison will cross the origin (0/2Pi)
        if (oppositeAngle() > pi){
            adj = 2*pi;
        }
        return (int)((this.value + 2*pi)*1000 - (other.value)*1000);         
    }


    
    


}
