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
package org.codice.ddf.registry.schemabindings.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;

public class InternationalStringTypeHelperTest {
    private static final ObjectFactory RIM_FACTORY = new ObjectFactory();

    private InternationalStringTypeHelper istHelper = new InternationalStringTypeHelper();

    private static final String US = "us";

    private static final String CANADA = "canada";

    private static final String ITALY = "italy";

    private static final String EMPTY_STRING = "";

    @Before
    public void setUp() {
        istHelper.setLocale(Locale.getDefault());
    }

    @Test
    public void testGetString() throws Exception {
        InternationalStringType ist = getTestInternationalStringType();

        String istString = istHelper.getString(ist);
        assertThat(istString, is(equalTo(US)));

        istHelper.setLocale(Locale.CANADA);
        istString = istHelper.getString(ist);
        assertThat(istString, is(equalTo(CANADA)));

        istHelper.setLocale(Locale.ITALY);
        istString = istHelper.getString(ist);
        assertThat(istString, is(equalTo(ITALY)));
    }

    @Test
    public void testGetStringWithNull() throws Exception {
        InternationalStringType ist = null;
        String istString = istHelper.getString(ist);

        assertThat(istString, is(equalTo(EMPTY_STRING)));
    }

    @Test
    public void testGetStringWithNoMatchingLocale() throws Exception {
        InternationalStringType ist = getTestInternationalStringType();
        istHelper.setLocale(Locale.CHINA);

        String istString = istHelper.getString(ist);
        assertThat(istString, is(equalTo(EMPTY_STRING)));
    }

    @Test
    public void testCreate() throws Exception {
        int expectedSize = 1;
        InternationalStringType ist = istHelper.create(US);
        assertThat(ist.isSetLocalizedString(), is(true));

        List<LocalizedStringType> lst = ist.getLocalizedString();
        assertThat(lst, hasSize(expectedSize));

        String stringFromIst = istHelper.getString(ist);

        assertThat(stringFromIst, is(equalTo(US)));
    }

    @Test
    public void testCreateWithBlanks() throws Exception {
        int expectedSize = 1;
        String stringToWrap = "  ";
        InternationalStringType ist = istHelper.create(stringToWrap);
        assertThat(ist.isSetLocalizedString(), is(true));

        List<LocalizedStringType> lst = ist.getLocalizedString();
        assertThat(lst, hasSize(expectedSize));

        String stringFromIst = istHelper.getString(ist);

        assertThat(stringFromIst, is(equalTo(stringToWrap)));
    }

    @Test
    public void testCreateWithEmpty() throws Exception {
        InternationalStringType ist = istHelper.create(EMPTY_STRING);
        assertThat(ist.isSetLocalizedString(), is(false));

        String stringFromIst = istHelper.getString(ist);

        assertThat(stringFromIst, is(equalTo(EMPTY_STRING)));
    }

    @Test
    public void testCreateWithNull() throws Exception {
        InternationalStringType ist = istHelper.create(null);
        assertThat(ist.isSetLocalizedString(), is(false));

        String stringFromIst = istHelper.getString(ist);

        assertThat(stringFromIst, is(equalTo(EMPTY_STRING)));
    }

    private InternationalStringType getTestInternationalStringType() {
        LocalizedStringType lstUs = RIM_FACTORY.createLocalizedStringType();
        lstUs.setLang(Locale.US.toLanguageTag());
        lstUs.setValue(US);

        LocalizedStringType lstCanada = RIM_FACTORY.createLocalizedStringType();
        lstCanada.setLang(Locale.CANADA.toLanguageTag());
        lstCanada.setValue(CANADA);

        LocalizedStringType lstItaly = RIM_FACTORY.createLocalizedStringType();
        lstItaly.setLang(Locale.ITALY.toLanguageTag());
        lstItaly.setValue(ITALY);

        InternationalStringType ist = RIM_FACTORY.createInternationalStringType();
        ist.getLocalizedString()
                .add(lstUs);
        ist.getLocalizedString()
                .add(lstCanada);
        ist.getLocalizedString()
                .add(lstItaly);

        return ist;
    }

}