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
 */
package ddf.catalog.transformer.input;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static junit.framework.Assert.assertNotNull;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;

public class MicrosoftTransformerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftTransformerTest.class);


    @Test(expected = CatalogTransformerException.class)
    public void testNullInputStream() throws Exception {
        transform(null);
    }

    @Test
    public void testWordDoc() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testWORD.docx");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Sample Word Document"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2008-12-11 16:04:00 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2010-11-12 16:21:00 UTC"));
        assertNotNull(metacard.getMetadata());
        /*
        assertThat(metacard.getMetadata(),
                containsString("<p>This is a sample Microsoft Word Document.</p>"));
        */
        assertThat(metacard.getContentTypeName(),
                is("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    private String convertDate(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        LOGGER.debug(df.format(date));
        return df.format(date);
    }

    private Metacard transform(InputStream stream) throws Exception {
        MicrosoftInputTransformer msWordTransformer = new MicrosoftInputTransformer();
        Metacard metacard = msWordTransformer.transform(stream);
        return metacard;
    }
}
