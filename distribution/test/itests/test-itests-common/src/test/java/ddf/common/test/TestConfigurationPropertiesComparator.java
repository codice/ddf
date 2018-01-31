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
package ddf.common.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Dictionary;
import org.apache.felix.cm.impl.CaseInsensitiveDictionary;
import org.codice.ddf.itests.common.matchers.ConfigurationPropertiesComparator;
import org.junit.Before;
import org.junit.Test;

public class TestConfigurationPropertiesComparator {
  private ConfigurationPropertiesComparator configurationPropertiesComparator;

  @Before
  public void setUp() {
    configurationPropertiesComparator = new ConfigurationPropertiesComparator();
  }

  @Test
  public void equalWithBothArgumentsNull() {
    assertThat(configurationPropertiesComparator.equal(null, null), is(true));
  }

  @Test
  public void equalWithFirstArgumentNull() {
    assertThat(
        configurationPropertiesComparator.equal(null, newDictionary("Key", "Value")), is(false));
  }

  @Test
  public void equalWithSecondArgumentNull() {
    assertThat(
        configurationPropertiesComparator.equal(newDictionary("Key", "Value"), null), is(false));
  }

  @Test
  public void equalDictionaryWithItself() {
    Dictionary<String, Object> dictionary = newDictionary("Key", "Value");
    assertThat(configurationPropertiesComparator.equal(dictionary, dictionary), is(true));
  }

  @Test
  public void equalWithEmptyDictionaries() {
    Dictionary<String, Object> dictionary = newDictionary();
    assertThat(configurationPropertiesComparator.equal(dictionary, dictionary), is(true));
  }

  @Test
  public void equalWithEmptyAndNonEmptyDictionaries() {
    assertThat(
        configurationPropertiesComparator.equal(newDictionary(), newDictionary("Key", "Value")),
        is(false));
  }

  @Test
  public void equalWithDictionariesThatHaveTheSameKeysAndValues() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary("Key", "Value"), newDictionary("Key", "Value")),
        is(true));
  }

  @Test
  public void equalWithDictionariesThatHaveDifferentKeys() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary("K1", "V1", "K2", "V2", "K3", "V3"),
            newDictionary("K1", "V1", "Different", "V2", "K3", "V3")),
        is(false));
  }

  @Test
  public void equalWithDictionariesThatHaveDifferentSizes() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary("K1", "V1"), newDictionary("K1", "V1", "K2", "V2")),
        is(false));
  }

  @Test
  public void equalWithDictionariesThatHaveDifferentValues() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary("K1", "V1", "K2", "V2", "K3", "V3"),
            newDictionary("K1", "V1", "K2", "Different", "K3", "V3")),
        is(false));
  }

  @Test
  public void equalWithDictionariesThatHaveDifferentValueTypes() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary("K1", "V1", "K2", "10", "K3", "V3"),
            newDictionary("K1", "V1", "K2", 10, "K3", "V3")),
        is(false));
  }

  @Test
  public void equalWithDictionariesThatHaveSameKeysAndArrayValues() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary("K1", new String[] {"V1", "V2"}, "K2", new String[] {"V3", "V4"}),
            newDictionary("K1", new String[] {"V1", "V2"}, "K2", new String[] {"V3", "V4"})),
        is(true));
  }

  @Test
  public void equalWithDictionariesThatHaveDifferentArrayValues() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary(
                "K1",
                new String[] {"V1"},
                "K2",
                new String[] {"V1", "V2", "V3"},
                "K3",
                new String[] {"V3"}),
            newDictionary(
                "K1",
                new String[] {"V1"},
                "K2",
                new String[] {"V1", "Different", "V3"},
                "K3",
                new String[] {"V3"})),
        is(false));
  }

  @Test
  public void equalWithDictionaryThatHasArrayValueAndAnotherThatHasSimpleValue() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary("K1", "V1", "K2", new String[] {"V2"}, "K3", "V3"),
            newDictionary("K1", "V1", "K2", "V2", "K3", "V3")),
        is(false));
  }

  @Test
  public void equalWithDictionaryThatHasSimpleValueAndAnotherThatHasArrayValue() {
    assertThat(
        configurationPropertiesComparator.equal(
            newDictionary("K1", "V1", "K2", "V2", "K3", "V3"),
            newDictionary("K1", "V1", "K2", new String[] {"V2"}, "K3", "V3")),
        is(false));
  }

  private Dictionary<String, Object> newDictionary(Object... keyValuePairs) {
    assertThat("List of key/value arguments must be even", keyValuePairs.length % 2, equalTo(0));

    @SuppressWarnings("unchecked")
    Dictionary<String, Object> dictionary = new CaseInsensitiveDictionary();

    for (int i = 0; i < keyValuePairs.length; i += 2) {
      dictionary.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
    }

    return dictionary;
  }
}
