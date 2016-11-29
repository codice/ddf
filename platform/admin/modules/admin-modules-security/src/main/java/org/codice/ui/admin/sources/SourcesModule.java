package org.codice.ui.admin.sources;

import java.net.URI;
import java.net.URISyntaxException;

import org.codice.ddf.ui.admin.api.module.AdminModule;

public class SourcesModule implements AdminModule {
    @Override
    public String getName() {
        return "Sources";
    }

    @Override
    public String getId() {
        return "Sources";
    }

    @Override
    public URI getJSLocation() {
        return null;
    }

    @Override
    public URI getCSSLocation() {
        return null;
    }

    @Override
    public URI getIframeLocation() {
        try {
            return new URI("./sources");
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
