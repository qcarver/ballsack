package me.qcarver.ballsack;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;

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
        final JFrame frame = new JFrame("xwatch");
        //make sure to shut down the application, when the frame is closed
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final Visualization applet = new Visualization();
        
        applet.frame = frame;
        frame.setResizable(true);
        frame.setLocation(100,100);
        applet.resize(500,500);
        applet.setPreferredSize(new Dimension(500,500));
        applet.setMinimumSize(new Dimension(500,500));
        
        frame.add(applet, BorderLayout.CENTER);
        applet.init();
        frame.pack();
        frame.setVisible(true);
    }

}
