package org.codice.ddf.ui.searchui.standard.endpoints;

public class AssociationsNotFoundException extends Exception {
    public AssociationsNotFoundException(String s) {
        super(s);
    }

    public AssociationsNotFoundException(Exception e) {
        super(e);
    }

    public AssociationsNotFoundException(String s, Exception e) {
        super(s, e);
    }

    public AssociationsNotFoundException(String s, Exception e, boolean x, boolean y) {
        super(s, e, x, y);
    }
}
