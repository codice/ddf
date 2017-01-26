package org.codice.ddf.commands.catalog.export;

import java.net.URI;

import javax.annotation.concurrent.Immutable;

import ddf.catalog.data.impl.MetacardImpl;

@Immutable
public class IdAndUriMetacard extends MetacardImpl {
    private final String id;

    private final URI uri;

    public IdAndUriMetacard(String id, URI uri) {
        this.id = id;
        this.uri = uri;
    }

    @Override
    public URI getResourceURI() {
        return this.uri;
    }

    @Override
    public String getId() {
        return this.id;
    }
}
