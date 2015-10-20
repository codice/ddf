package org.codice.ddf.checksum;

import java.io.Serializable;

public class SerializableTestObject implements Serializable {

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public  String name;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public  String description;
}
