/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.platform.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static junit.framework.TestCase.assertFalse;

import org.junit.ClassRule;
import org.junit.Test;

import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public class InputValidationTest {

    private static final String BAD_FILE = "../.././myfile.exe.bat.unk";

    private static final String BAD_FILE1 = "/../myfile.exe/.bat.exe";

    private static final String BAD_FILE2 = ".";

    private static final String BAD_FILE3 = "robots.TXT";

    private static final String SANI_BAD_FILE = "myfile.bin.bin.unk";

    private static final String SANI_BAD_FILE1 = "bin.bin";

    private static final String GOOD_FILE = "myfile.bin";

    private static final String KNOWN_BAD_FILE = ".htaccess";

    private static final String DEFAULT_FILE = "file.bin";

    private static final String GOOD_MIME = "application/pdf";

    private static final String BAD_MIME = "text/html";

    private static final String IGNORE_FILE = ".DS_Store";

    private static final String IGNORE_FILE_CASE_INSENSITIVE = ".dS_sTorE";

    private static final String IGNORE_FILE_KNOWN_GOOD = "valid_ddms_record.xml";

    @ClassRule
    public static final ProvideSystemProperty BAD_FILES_SYSTEM_PROPERTIES = new ProvideSystemProperty(
            "bad.files",
            "crossdomain.xml,clientaccesspolicy.xml,.htaccess,.htpasswd,hosts,passwd,group,resolv.conf,nfs.conf,ftpd.conf,ntp.conf,web.config,robots.txt");

    @ClassRule
    public static final ProvideSystemProperty BAD_FILE_EXTENSTIONS_SYSTEM_PROPERTIES =
            new ProvideSystemProperty("bad.file.extensions",
                    ".exe,.jsp,.html,.js,.php,.phtml,.php3,.php4,.php5,.phps,.shtml,.jhtml,.pl,.py,.cgi,.msi,.com,.scr,.gadget,.application,.pif,.hta,.cpl,.msc,.jar,.kar,.bat,.cmd,.vb,.vbs,.vbe,.jse,.ws,.wsf,.wsc,.wsh,.ps1,.ps1xml,.ps2,.ps2xml,.psc1,.psc2,.msh,.msh1,.msh2,.mshxml,.msh1xml,.msh2xml,.scf,.lnk,.inf,.reg,.dll,.vxd,.cpl,.cfg,.config,.crt,.cert,.pem,.jks,.p12,.p7b,.key,.der,.csr,.jsb,.mhtml,.mht,.xhtml,.xht");

    @ClassRule
    public static final ProvideSystemProperty BAD_MIME_TYPES_SYSTEM_PROPERTIES =
            new ProvideSystemProperty("bad.mime.types",
                    "text/html,text/javascript,text/x-javascript,application/x-shellscript,text/scriptlet,application/x-msdownload,application/x-msmetafile");

    @ClassRule
    public static final ProvideSystemProperty IGNORE_FILES_SYSTEM_PROPERTIES =
            new ProvideSystemProperty("ignore.files", ".DS_Store,Thumbs.db");

    @Test
    public void testSanitizeFilenameBad() {
        String sanitizedName = InputValidation.sanitizeFilename(BAD_FILE);
        assertThat("Actual sanitized filename does not match expected output.",
                sanitizedName,
                is(SANI_BAD_FILE));
    }

    @Test
    public void testSanitizeFilenameBad1() {
        String sanitizedName = InputValidation.sanitizeFilename(BAD_FILE1);
        assertThat("Actual sanitized filename does not match expected output.",
                sanitizedName,
                is(SANI_BAD_FILE1));
    }

    @Test
    public void testSanitizeFilenameBad2() {
        String sanitizedName = InputValidation.sanitizeFilename(BAD_FILE2);
        assertThat("Actual sanitized filename does not match expected output.",
                sanitizedName,
                is(DEFAULT_FILE));
    }

    @Test
    public void testSanitizeFilenameGood() {
        String sanitizedName = InputValidation.sanitizeFilename(GOOD_FILE);
        assertThat("Actual sanitized filename does not match expected output.",
                sanitizedName,
                is(GOOD_FILE));
    }

    @Test
    public void testSanitizeFilenameKnownBad() {
        String sanitizedName = InputValidation.sanitizeFilename(KNOWN_BAD_FILE);
        assertThat("Actual sanitized filename does not match expected output.",
                sanitizedName,
                is(DEFAULT_FILE));
    }

    @Test
    public void testIsMimeTypeClientSideSafeMimeTypeBad() {
        boolean result = InputValidation.isMimeTypeClientSideSafe(BAD_MIME);
        assertFalse(result);
    }

    @Test
    public void testIsMimeTypeClientSideSafeMimeTypeGood() {
        boolean result = InputValidation.isMimeTypeClientSideSafe(GOOD_MIME);
        assertTrue(result);
    }

    @Test
    public void testSanitizeFilenameKnownBadCaseInsensitive() {
        String sanitizedName = InputValidation.sanitizeFilename(BAD_FILE3);
        assertThat("Actual sanitized filename does not match expected output.",
                sanitizedName,
                is(DEFAULT_FILE));
    }

    @Test
    public void testIsFileNameClientSideSafeFileNameBad() {
        boolean result = InputValidation.isFileNameClientSideSafe(IGNORE_FILE);
        assertFalse(result);
    }

    @Test
    public void testIsFileNameClientSideSafeFileNameBadCaseInsensitive() {
        boolean result =
                InputValidation.isFileNameClientSideSafe(IGNORE_FILE_CASE_INSENSITIVE);
        assertFalse(result);
    }

    @Test
    public void testIsFileNameClientSideSafeFileNameGood() {
        boolean result = InputValidation.isFileNameClientSideSafe(IGNORE_FILE_KNOWN_GOOD);
        assertTrue(result);
    }
}
