/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.qcarver.ballsack;


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
    * method for comparing angles (delta in radians * 1000)
     * @param other
     * @return value in radians * 1000 where negative is counterclockwise
     */
    public int compareTo(Object other) {
        return compareTo(((Radians)other).value());     
    }
    
     /**
     * method for comparing angles (delta in radians * 1000)
     * @param otherAngle
     * @return value in radians * 1000 where negative is counterclockwise
     */
    public int compareTo(double otherAngle){
        return compareTo((float)otherAngle);
    }
 
    /**
     * method for comparing angles (delta in radians * 1000)
     * @param otherAngle
     * @return value in radians * 1000 where negative is counterclockwise
     */
    public int compareTo(float otherAngle){  
        return (int)(delta(otherAngle)*1000);
    }
    
     /**
     * Finds the size of the angle rotating from this angle to another
     * @param other
     * @return value in radians where negative is counterclockwise
     */
    public float delta(Radians other){
        return delta(other.value());
    }
    
     /**
     * Finds the size of the angle rotating from this angle to another
     * @param otherAngle
     * @return value in radians where negative is counterclockwise
     */
    public float delta(float otherAngle){  
        float difference = pi;
        //these are simple cases where the angle can't span the origin
        if ((value < pi) && (otherAngle < pi) ||
            (value > pi) && (otherAngle > pi)){
            difference = value - otherAngle;
        }
        //this is the case where the angle could span the origin
        else {
            float bigAngle = otherAngle;
            float lilAngle = value;
            float sign = 1;
            if (value > otherAngle){
                bigAngle = value;
                lilAngle = otherAngle;
                sign = -1;
            }
            //the shortest route doesn't span the origin
            if ((bigAngle - lilAngle) < pi){
                difference = sign * (bigAngle - lilAngle);
            } else {
                //the shortest route DOES span the origin
                float adj = (2f*pi - bigAngle);
                difference = sign * (lilAngle + adj);
            }
        }
        return difference;
    }


    
    


}
