/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.admin.insecure.defaults.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

public class PaxWebCfgFileValidatorTest {

    private static final String PAX_WEB_CFG_FILE_HTTP_ENABLED = "/org.ops4j.pax.web.cfg";

    private static final String PAX_WEB_CFG_FILE_HTTP_DISABLED = "/org.ops4j.pax.web.http.disabled.cfg";

    private static final String FAKE_PAX_WEB_CFG_FILE = "/fakeCfgFile.cfg";

    @Test
    public void testPaxWebConfigFileDoesNotExist() throws Exception {
        // Setup
        PaxWebCfgFileValidator pax = new PaxWebCfgFileValidator();
        pax.setPath(Paths.get(FAKE_PAX_WEB_CFG_FILE));

        // Perform Test
        List<Alert> alerts = pax.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(
                alerts.get(0).getMessage(),
                is(String.format(PaxWebCfgFileValidator.GENERIC_INSECURE_DEFAULTS_MSG,
                        FAKE_PAX_WEB_CFG_FILE)
                        + FAKE_PAX_WEB_CFG_FILE
                        + " (No such file or directory)"));
    }

    @Test
    public void testPaxWebConfigFileHasHttpEnabled() throws Exception {
        // Setup
        PaxWebCfgFileValidator pax = new PaxWebCfgFileValidator();
        Path path = Paths.get(getClass().getResource(PAX_WEB_CFG_FILE_HTTP_ENABLED).toURI());
        pax.setPath(path);

        // Perform Test
        List<Alert> alerts = pax.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getMessage(), is(String.format(
                PaxWebCfgFileValidator.HTTP_ENABLED_MSG,
                PaxWebCfgFileValidator.HTTP_ENABLED_PROPERTY, path)));
    }

    @Test
    public void testPaxWebConfigFileHasHttpDisabled() throws Exception {
        // Setup
        PaxWebCfgFileValidator pax = new PaxWebCfgFileValidator();
        pax.setPath(Paths.get(getClass().getResource(PAX_WEB_CFG_FILE_HTTP_DISABLED).toURI()));

        // Perform Test
        List<Alert> alerts = pax.validate();

        // Verify
        assertThat(alerts.size(), is(0));
    }
}
