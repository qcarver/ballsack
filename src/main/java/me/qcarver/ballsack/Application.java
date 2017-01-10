package me.qcarver.ballsack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Quinn
 */
public class Application {
    static String[] args;

    public static void main(String[] args) {
        Application.args = args;
        new Application();
    }

    public Application() {
        init();
    }

    private void init() {
        Visualization.main("me.qcarver.ballsack.Visualization");
    }
}
