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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import org.apache.commons.lang.StringUtils;

/**
 * This class provides helper methods for parsing and creating InternationalStringType Locale is
 * used in getting or setting the String in the LocalizedStringType The JVM's default locale will be
 * used unless one is set.
 */
public class InternationalStringTypeHelper {
  private Locale locale;

  private static final String DEFAULT_LANG = "en-US";

  public InternationalStringTypeHelper() {
    this(Locale.getDefault());
  }

  InternationalStringTypeHelper(Locale locale) {
    setLocale(locale);
  }

  /**
   * This is is a convenience method that pulls the string value from the InternationalStringType
   * This convenience method will use the locale to get the string from the LocalizedStringType
   *
   * @param internationalString the internationalStringType, null returns empty String
   * @return the String value pulled from the wrapped localizedString Empty string if a matching
   *     language tag is not found
   */
  public String getString(InternationalStringType internationalString) {
    Optional<String> optionalString = Optional.empty();
    if (internationalString != null) {
      optionalString = getLocalizedString(internationalString.getLocalizedString());
    }

    return optionalString.orElse("");
  }

  /**
   * This is a convenience method that creates an InternationalStringType object from the given
   * String The string will be wrapped in a LocalizedStringType using the Locale, default Locale if
   * one hasn't been set
   *
   * @param internationalizeThis the string to be wrapped in the InternationalStringType, null
   *     returns empty InternationalStringType
   * @return the InternationalStringType wrapped around the provided string
   */
  public InternationalStringType create(String internationalizeThis) {
    InternationalStringType ist = RIM_FACTORY.createInternationalStringType();

    if (StringUtils.isNotEmpty(internationalizeThis)) {
      LocalizedStringType lst = RIM_FACTORY.createLocalizedStringType();
      lst.setValue(internationalizeThis);
      // The LocalizedStringType handles the default languageTag so don't need to set it
      if (!locale.toLanguageTag().equals(DEFAULT_LANG)) {
        lst.setLang(locale.toLanguageTag());
      }
      ist.setLocalizedString(Collections.singletonList(lst));
    }

    return ist;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  private Optional<String> getLocalizedString(List<LocalizedStringType> localizedStrings) {
    Optional<String> optionalLocalString =
        localizedStrings
            .stream()
            .filter(localizedString -> locale.toLanguageTag().equals(localizedString.getLang()))
            .findFirst()
            .map(LocalizedStringType::getValue);

    // Default to the the first string in the list if one matching the locale wasn't found
    if (!optionalLocalString.isPresent()) {
      optionalLocalString =
          localizedStrings.stream().findFirst().map(LocalizedStringType::getValue);
    }

    return optionalLocalString;
  }
}
