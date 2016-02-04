/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.qcarver.ballsack;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author Quinn
 */
public class RadiansTest {

    private List<Radians> radians = null;
    private static final double pi = Math.PI;

    public RadiansTest() {
        radians = new ArrayList<Radians>();
        radians.add(new Radians(-1 * (pi / 8)));
        radians.add(new Radians(0));
        radians.add(new Radians(.25 * pi));
        radians.add(new Radians(1.5 * pi));
        radians.add(new Radians(2 * pi));
        radians.add(new Radians(1.1 * pi));
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of value method, of class Radians.
     */
    @org.junit.Test
    public void testValue() {
        System.out.println("value");
        Radians instance = new Radians(2 * pi);
        float expResult = 0.0F;
        float result = instance.value();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of compareTo method, of class Radians.
     */
    @org.junit.Test
    public void testCompareTo_Object() {
        boolean first = true;
        System.out.print("Radians values unsorted: ");
        for (Radians radian : radians) {
            assert (radian.value() >= 0);
            assert (radian.value() < 2 * pi);
            System.out.print((first) ? radian : ", " + radian);
        }
        for (Radians radian : radians) {
            for (Radians otherRadian : radians) {
                if (radian.compareTo(otherRadian) < 0) {
                    System.out.println(radian.value() + " was counterclockwise from " + otherRadian.value());
                } else if (radian.compareTo(otherRadian) == 0) {
                    System.out.println(radian.value() + " is the same as " + otherRadian.value());
                } else {
                    System.out.println(radian.value() + " was clockwise from " + otherRadian.value());
                }
            }
        }
    }

}
