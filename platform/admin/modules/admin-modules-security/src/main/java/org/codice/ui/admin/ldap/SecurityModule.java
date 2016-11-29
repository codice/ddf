package org.codice.ui.admin.ldap;

import java.net.URI;
import java.net.URISyntaxException;

import org.codice.ddf.ui.admin.api.module.AdminModule;

public class SecurityModule implements AdminModule {
    @Override
    public String getName() {
        return "Security";
    }

    @Override
    public String getId() {
        return "Security";
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
            return new URI("./security");
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
