/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.qcarver.ballsack;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Quinn
 */
public class XmlVisualization extends DefaultHandler {
    //constructor recieves a file or a stream of xml data

    //Sax parser
    //open tag: 
    //create Circle|Sack pointer
    //(last tag was open tag)? assign new Sack to pointer
    //close tag: 
    //(last tag was open tag)? assign new Circle to last pointer
    //put previous circles in prev open tags sack}
    String xmlFileName;
    String tmpValue;
    boolean elementStarted = false;
    SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd");
    Stack<PlaceHolder> ancestry = new Stack<>();
    Set<Circle> sackMe = new HashSet<>();

    /**
     * Until we close an element, we don't know if it's a circle or a sack keep
     * a pointer to hold its place, and also remember the name to give it
     */
    private class PlaceHolder {

        //The element's name as it appears in the opening tag
        String name;
        //Sack is also a Circle so this can be either
        Circle instance = null;

        PlaceHolder(String name) {
            this.name = name;
        }
    }

    public XmlVisualization(String xmlFileName) {
        this.xmlFileName = xmlFileName;
        parseDocument();
    }

    private void parseDocument() {
        // parse
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(xmlFileName, this);
        } catch (ParserConfigurationException e) {
            System.out.println("ParserConfig error");
        } catch (SAXException e) {
            System.out.println("SAXException : xml not well formed");
        } catch (IOException e) {
            System.out.println("IO error");
        }
    }

    @Override
    public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException {
        //open tag: 
        //create Circle|Sack pointer
        //(last tag was open tag)? assign new Sack to pointer

        //we don't know if this is a circle or a sack yet...placeholder
        PlaceHolder placeHolder = new PlaceHolder(elementName);
        ancestry.push(placeHolder);
        //TODOD: Change to log line
        System.out.print("(");
        elementStarted = true;

    }

    @Override
    public void endElement(String s, String s1, String element) throws SAXException {
        //close tag: 
        //(last tag was open tag)? assign new Circle to last pointer
        //put previous circles in prev open tags sack}
        Circle newElement = null;

        //we are closing a leaf node... this is represented by a circle
        if (elementStarted) {
            //make a new circle with the last childs name and size
            ancestry.peek().instance = new Circle(ancestry.peek().name);
            newElement = ancestry.pop().instance;
            //TODO: Change to log line
            System.out.print(newElement.name+" ");
        } else {
            //we are closing an internal node ... represented by a sack
            newElement = ancestry.pop().instance;
            //((Sack) newElement).closeSack();
        }
        //If this is not the close of the root anscestor..
        if (ancestry.size() > 1) {
            //so preceeding instance must be an internal node ...(a sack)
            if (ancestry.peek().instance == null) {
                //first sibling to go in this sack, must make the sack first
                ancestry.peek().instance = new Sack(ancestry.peek().name);
            }
            //put the new circle in the sack ...recall Sack is also a kind of circle
            ((Sack) ancestry.peek().instance).addCircle(newElement);
        }
                    //TODO: Change to log line
            System.out.print(")");
        elementStarted = false;
    }

    @Override
    public void characters(char[] ac, int i, int j) throws SAXException {
        tmpValue = new String(ac, i, j);
    }

    public static void main(String[] args) {
        new XmlVisualization("/Users/Quinn/Desktop/musicXml/cd_catalog.xml");
    }
}
