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
package org.codice.ddf.catalog.plugin.metacard.backup.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class MetacardTemplateTest {

    private static final String ID = "testId";

    private final Date cardDate = new Date(1495071840000L);

    @Test
    public void testSimpleExpression() throws IOException {
        MetacardTemplate templateBean = new MetacardTemplate("/test/{{id}}.xml");
        Metacard metacard = getTestMetacard();
        String str = templateBean.applyTemplate(metacard);
        assertThat(str, is("/test/" + ID + ".xml"));
    }

    @Test
    public void testSubstringExpression() throws IOException {
        MetacardTemplate templateBean = new MetacardTemplate(
                "/test/{{substring id 0 3}}/{{id}}.xml");
        Metacard metacard = getTestMetacard();
        String str = templateBean.applyTemplate(metacard);
        String subStr = ID.substring(0, 3);
        assertThat(str, is("/test/" + subStr + "/" + ID + ".xml"));
    }

    @Test
    public void testDateExpression() throws IOException {
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = dateFormatGmt.format(cardDate);

        MetacardTemplate templateBean = new MetacardTemplate("/test/{{dateFormat created \"yyyy-MM-dd\"}}/{{id}}.xml");
        Metacard metacard = getTestMetacard();
        String str = templateBean.applyTemplate(metacard);
        assertThat(str, is("/test/" + dateStr + "/" + ID + ".xml"));
    }

    public Metacard getTestMetacard() {

        MetacardImpl metacard = new MetacardImpl();
        metacard.setId(ID);
        metacard.setCreatedDate(cardDate);
        return metacard;
    }
}
