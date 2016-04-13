package org.codice.ddf.ui.searchui.standard.endpoints;

public class StandardSearchException extends Exception {
    public StandardSearchException(String s) {
        super(s);
    }

    public StandardSearchException(Exception e) {
        super(e);
    }

    public StandardSearchException(String s, Exception e) {
        super(s, e);
    }

    public StandardSearchException(String s, Exception e, boolean x, boolean y) {
        super(s, e, x, y);
    }
}
