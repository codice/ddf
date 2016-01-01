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
package ddf.catalog.transformer.input.video;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import ddf.catalog.data.Metacard;

public class VideoInputTransformerTest {
    @Test
    public void testMp4Video() throws Exception {
        InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("testMP4Video.mp4");
        Metacard metacard = transform(stream);

        assertThat(metacard, notNullValue());
        assertThat(convertDate(metacard.getModifiedDate()), is("2012-09-01 12:31:21 UTC"));

        String metadata = metacard.getMetadata();
        assertThat(metadata, notNullValue());
        assertThat(metadata, containsString("<meta name=\"tiff:ImageLength\" content=\"360\"/>"));
        assertThat(metadata, containsString("<meta name=\"tiff:ImageWidth\" content=\"480\"/>"));
        assertThat(metacard.getContentTypeName(), is("video/mp4"));
    }

    private Metacard transform(InputStream stream) throws Exception {
        VideoInputTransformer videoInputTransformer = new VideoInputTransformer();
        return videoInputTransformer.transform(stream);
    }

    private String convertDate(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }
}
