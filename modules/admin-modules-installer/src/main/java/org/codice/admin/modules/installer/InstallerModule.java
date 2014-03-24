package org.codice.admin.modules.installer;

import java.net.URI;
import java.net.URISyntaxException;

import org.codice.ddf.ui.admin.api.module.AdminModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tustisos on 3/24/14.
 */
public class InstallerModule implements AdminModule {

    private Logger logger = LoggerFactory.getLogger(InstallerModule.class);

    @Override
    public String getName() {
        return "Installation";
    }

    @Override
    public String getId() {
        return "installation";
    }

    @Override
    public URI getJSLocation() {
        try {
            return new URI("/installer/js/modules/app.module.js");
        } catch (URISyntaxException e) {
            logger.warn("Must set the JS location to a valid URI.", e);
        }
        return null;
    }

    @Override
    public URI getCSSLocation() {
        try {
            return new URI("/installer/css/style.css");
        } catch (URISyntaxException e) {
            logger.warn("Must set the JS location to a valid URI.", e);
        }
        return null;
    }

    @Override
    public URI getIframeLocation() {
        return null;
    }
}
