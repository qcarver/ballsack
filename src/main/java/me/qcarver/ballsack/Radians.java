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
    private final static float pi = (float)(Math.PI);
    private final float value;
    
    //Though Math.PI is a double, processing will cast down to a float anyway
    public Radians(double value){
        this((float)value);
    }
    
    public Radians(float value){
        //internally we only to deal with the range 0...2Pi
        this.value = (value < 0)? (value % (2 * pi)) + 2*pi : value % (2*pi); 
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
    /**
     * Think of a negative result as being ccw to, positive cw to
     */
    public int compareTo(Object o) {
        return compareTo(((Radians)o).value());     
    }
    
    public int compareTo(double otherAngle){
        return compareTo((float)otherAngle);
    }
    
    public int compareTo(float otherAngle){     
        //(cpu_facing-player_degree+360)%360>180
        return (int) ((otherAngle-value+2f*pi)%2*pi-pi);
//        float adj = 0;
//        float ourAdjustedAngle = value;
//        //if the comparison will cross the origin (0/2Pi)
//        if ((otherAngle > pi) && (value < pi)){
//            adj = 2*pi - otherAngle;
//            if (adj + value < pi){
//            otherAngle += adj;
//            ourAdjustedAngle += adj;
//            }
//        }
//        return (int)((otherAngle)*1000 - (ourAdjustedAngle)*1000);      
    }


    
    


}
