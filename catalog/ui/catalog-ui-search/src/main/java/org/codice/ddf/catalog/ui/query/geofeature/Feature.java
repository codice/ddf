package org.codice.ddf.catalog.ui.query.geofeature;

public abstract class Feature {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    private String name;
    // this shouldn't be an instance property (it's constant for the class),
    // but I haven't seen the right way yet to customize boon's serialization
    // to get anything but properties...
    protected String type;
}
