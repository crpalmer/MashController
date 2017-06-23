package org.crpalmer.mashcontroller;

import android.util.Log;
import android.util.Xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static java.lang.System.in;

/**
 * Created by crpalmer on 6/23/17.
 */

public class MashSession implements ContentHandler {
    private final ArrayList<MashStep> mashSteps = new ArrayList<>();

    private static final String MASH_STEP = "MASH_STEP";
    private static final String NAME = "NAME";
    private static final String STEP_TIME = "STEP_TIME";
    private static final String STEP_TEMP = "STEP_TEMP";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String INFUSE_TEMP = "INFUSE_TEMP";
    private static final String INFUSE_AMOUNT = "INFUSE_AMOUNT";

    private StringBuilder sb;
    private String name;
    private Double stepTemperature;
    private Integer stepSeconds;
    private String description;
    private Double infuseTemp;
    private Double infuseAmount;

    public MashSession(File file) throws XmlException, FileNotFoundException {
        Reader in = new InputStreamReader(new FileInputStream(file));
        try {
            Xml.parse(in, this);
        } catch (IOException | SAXException e) {
            throw new XmlException("Failed to parse " + file, e);
        }
    }

    public double getInfuseTemp() {
        return infuseTemp;
    }

    public double getInfuseAmount() {
        return infuseAmount;
    }

    public List<MashStep> getMashSteps() {
        return mashSteps;
    }

    @Override
    public void setDocumentLocator(Locator locator) {

    }

    @Override
    public void startDocument() throws SAXException {

    }

    @Override
    public void endDocument() throws SAXException {

    }

    @Override
    public void startPrefixMapping(String s, String s1) throws SAXException {

    }

    @Override
    public void endPrefixMapping(String s) throws SAXException {

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (MASH_STEP.equals(qName)) {
            name = null;
            stepTemperature = null;
            stepSeconds = null;
            description = null;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (MASH_STEP.equals(qName)) {
            if (stepTemperature == null) {
                throw new SAXNotSupportedException("Missing step temperature for step " + mashSteps.size() + 1);
            }
            if (stepSeconds == null) {
                throw new SAXNotSupportedException("Missing step temperature for step " + mashSteps.size() + 1);
            }
            mashSteps.add(new MashStep(stepTemperature, stepSeconds, name != null ? name : description));
        } else if (STEP_TEMP.equals(qName)) {
            stepTemperature = getTemperature(sb.toString());
        } else if (STEP_TIME.equals(qName)) {
            stepSeconds = (int) Math.round(Double.valueOf(sb.toString().trim()) * 60);
        } else if (DESCRIPTION.equals(qName)) {
            description = sb.toString();
        } else if (INFUSE_TEMP.equals(qName) && infuseTemp == null) {
            infuseTemp = getTemperature(sb.toString());
        } else if (INFUSE_AMOUNT.equals(qName) && infuseAmount == null) {
            infuseAmount = Double.valueOf(sb.toString()) / 4;
        }
        sb = null;
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        if (sb == null) {
            sb = new StringBuilder();
        }
        sb.append(chars, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException {

    }

    @Override
    public void processingInstruction(String s, String s1) throws SAXException {

    }

    @Override
    public void skippedEntity(String s) throws SAXException {

    }

    private double getTemperature(String string) {
        boolean degC = true;
        if (string.endsWith("F")) {
            string = string.substring(0, string.length()-2);
            degC = false;
        } else if (string.endsWith("C")) {
            string = string.substring(0, string.length()-2);
        }

        double temp = Double.valueOf(string);

        if (degC) {
            return temp / 5 * 9 + 32;
        } else {
            return temp;
        }
    }
}
