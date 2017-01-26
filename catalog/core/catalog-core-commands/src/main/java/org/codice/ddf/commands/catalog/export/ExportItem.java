package org.codice.ddf.commands.catalog.export;

import java.net.URI;
import java.util.Objects;

public class ExportItem {
    private String id = "";

    private String metacardTag = "";

    private URI resourceURI;

    public String getId() {
        return id;
    }

    public String getMetacardTag() {
        return metacardTag;
    }

    public URI getResourceURI() {
        return resourceURI;
    }

    public ExportItem(String id, String metacardTag, URI resourceURI) {
        this.id = id;
        this.metacardTag = metacardTag;
        this.resourceURI = resourceURI;
    }

    @Override
    public String toString() {
        return String.format("ExportItem{id='%s', metacardTag='%s', resourceURI='%s'}",
                id,
                metacardTag,
                resourceURI);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExportItem that = (ExportItem) o;
        return Objects.equals(id, that.id) && Objects.equals(metacardTag, that.metacardTag)
                && Objects.equals(resourceURI, that.resourceURI);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, metacardTag);
    }
}
