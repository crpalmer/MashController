package org.crpalmer.mashcontroller;

/**
 * Wrapper for XML based exceptions
 */

public class XmlException extends Exception {
    public XmlException(String s) {
        super(s);
    }

    public XmlException(String s, Exception cause) {
        super(s, cause);
    }
}
