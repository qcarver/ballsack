package me.qcarver.ballsack;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JFrame;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Quinn
 */
public class Application extends JFrame implements DropTargetListener {

    static String[] args;

    public enum DragState {

        Waiting,
        Accept,
        Reject
    }
    private DragState state = DragState.Waiting;

    DropTarget dt = new DropTarget(
            this,
            DnDConstants.ACTION_COPY_OR_MOVE,
            this,
            true);

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        state = DragState.Reject;

        repaint();
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        System.out.println("drag over");
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        state = DragState.Waiting;
        repaint();
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        state = DragState.Waiting;
        getFileNameDraggedEvent(dtde);
        repaint();
    }

    /**
     * Thanks to Rocket Hazmat on Stack Overflow for this gem. Behavior in
     * Windows different than Mac/Linux, so DnD impl is not straight fwd!
     *
     * @param dtde
     * @return fileName or empty if not a file/didn't-work
     */
    private String getFileNameDraggedEvent(DropTargetDropEvent dtde) {
        String filename = "";

        try {
            // Get the object to be transferred
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();

            // If flavors is empty get flavor list from DropTarget
            flavors = (flavors.length == 0) ? dtde.getCurrentDataFlavors() : flavors;

            // Select best data flavor
            DataFlavor flavor = DataFlavor.selectBestTextFlavor(flavors);

            // Flavor will be null on Windows
            // In which case use the 1st available flavor
            flavor = (flavor == null) ? flavors[0] : flavor;

            // Flavors to check
            DataFlavor Linux = new DataFlavor("text/uri-list;class=java.io.Reader");
            DataFlavor Windows = DataFlavor.javaFileListFlavor;

            // On Linux (and OS X) file DnD is a reader
            if (flavor.equals(Linux)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                BufferedReader read = new BufferedReader(flavor.getReaderForText(tr));
                // Remove 'file://' from file name
                filename = read.readLine().substring(7).replace("%20", " ");
                // Remove 'localhost' from OS X file names
                if (filename.substring(0, 9).equals("localhost")) {
                    filename = filename.substring(9);
                }
                read.close();

                dtde.dropComplete(true);
                System.out.println("File Dragged:" + filename);
                //mainWindow.openFile(fileName);
            } // On Windows file DnD is a file list
            else if (flavor.equals(Windows)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                @SuppressWarnings("unchecked")
                List<File> list = (List<File>) tr.getTransferData(flavor);
                dtde.dropComplete(true);

                if (list.size() == 1) {
                    System.out.println("File Dragged: " + list.get(0));
                    //mainWindow.openFile(list.get(0).toString());
                }
            } else {
                System.err.println("DnD Error");
                dtde.rejectDrop();
            }
            return filename;
        } //TODO: OS X Throws ArrayIndexOutOfBoundsException on first DnD
        catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("DnD not initalized properly, please try again.");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (UnsupportedFlavorException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
        return filename;
    }

    public static void main(String[] args) {
        Application.args = args;
        new Application();
    }

    public Application() {
        init();
    }

    private void init() {
        //final JFrame frame = new JFrame("xwatch");
        //make sure to shut down the application, when the frame is closed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final Visualization applet = new Visualization();

        applet.frame = this;
        setResizable(true);
        setLocation(100, 100);
        applet.resize(500, 500);
        applet.setPreferredSize(new Dimension(500, 500));
        applet.setMinimumSize(new Dimension(500, 500));

        add(applet, BorderLayout.CENTER);
        applet.init();
        pack();
        setVisible(true);
    }
}
