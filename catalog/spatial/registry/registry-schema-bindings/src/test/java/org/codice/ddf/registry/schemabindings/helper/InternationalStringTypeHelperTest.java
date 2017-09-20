/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.registry.schemabindings.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;
import org.junit.Before;
import org.junit.Test;

public class InternationalStringTypeHelperTest {
  private static final ObjectFactory RIM_FACTORY = new ObjectFactory();

  private InternationalStringTypeHelper istHelper = new InternationalStringTypeHelper();

  private static final String US = "us";

  private static final String CANADA = "canada";

  private static final String ITALY = "italy";

  private static final ImmutableMap<String, String> LOCALE_MAP =
      ImmutableMap.of(
          Locale.US.toLanguageTag(),
          US,
          Locale.CANADA.toLanguageTag(),
          CANADA,
          Locale.ITALY.toLanguageTag(),
          ITALY);

  private static final String DEF_LOCALE_NAME = "deflocale";

  private static final String TAIWAN = "taiwan";

  private static final String EMPTY_STRING = "";

  @Before
  public void setUp() {
    istHelper.setLocale(Locale.getDefault());
  }

  @Test
  public void testGetString() throws Exception {
    InternationalStringType ist = getTestInternationalStringType();

    String istString = istHelper.getString(ist);

    istHelper.setLocale(Locale.US);
    istString = istHelper.getString(ist);
    assertThat(istString, is(equalTo(US)));

    istHelper.setLocale(Locale.CANADA);
    istString = istHelper.getString(ist);
    assertThat(istString, is(equalTo(CANADA)));

    istHelper.setLocale(Locale.ITALY);
    istString = istHelper.getString(ist);
    assertThat(istString, is(equalTo(ITALY)));
  }

  @Test
  public void testGetStringWithDefaultLocale() throws Exception {
    InternationalStringType ist = getTestInternationalStringWithDefault();

    String istString = istHelper.getString(ist);
    assertThat(istString, is(equalTo(DEF_LOCALE_NAME)));
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
    // Ensure that we're adding an unknown locale and not the default system locale
    if (Locale.getDefault() == Locale.CHINA) {
      istHelper.setLocale(Locale.KOREA);
    } else {
      istHelper.setLocale(Locale.CHINA);
    }

    String istString = istHelper.getString(ist);
    // If unknown locale isn't matched, the first in the list of localized strings will be returned. US, in this case.
    assertThat(istString, is(equalTo(US)));
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
    InternationalStringType ist = RIM_FACTORY.createInternationalStringType();

    for (Map.Entry<String, String> row : LOCALE_MAP.entrySet()) {
      LocalizedStringType localizedStringType = RIM_FACTORY.createLocalizedStringType();
      localizedStringType.setLang(row.getKey());
      localizedStringType.setValue(row.getValue());
      ist.getLocalizedString().add(localizedStringType);
    }

    return ist;
  }

  private InternationalStringType getTestInternationalStringWithDefault() {
    InternationalStringType ist = RIM_FACTORY.createInternationalStringType();

    LocalizedStringType lstLocale = RIM_FACTORY.createLocalizedStringType();
    lstLocale.setLang(Locale.getDefault().toLanguageTag());
    lstLocale.setValue(DEF_LOCALE_NAME);
    ist.getLocalizedString().add(lstLocale);

    LocalizedStringType lstTaiwan = RIM_FACTORY.createLocalizedStringType();
    lstTaiwan.setLang(Locale.TAIWAN.toLanguageTag());
    lstTaiwan.setValue(TAIWAN);
    ist.getLocalizedString().add(lstTaiwan);

    return ist;
  }
}
