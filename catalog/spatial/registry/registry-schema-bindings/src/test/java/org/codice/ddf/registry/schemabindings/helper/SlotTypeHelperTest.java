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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.junit.BeforeClass;
import org.junit.Test;

public class SlotTypeHelperTest {

  private static Parser parser;

  private static ParserConfigurator configurator;

  private static RegistryObjectType registryObject;

  private SlotTypeHelper stHelper = new SlotTypeHelper();

  @BeforeClass
  public static void setUpOnce() throws Exception {
    parser = new XmlParser();

    configurator =
        parser.configureParser(
            Arrays.asList(
                RegistryObjectType.class.getPackage().getName(),
                EbrimConstants.OGC_FACTORY.getClass().getPackage().getName(),
                EbrimConstants.GML_FACTORY.getClass().getPackage().getName()),
            SlotTypeHelper.class.getClassLoader());

    registryObject = getRegistryObjectFromResource("/registry-package-slots-only.xml");
  }

  @Test
  public void testGetSlotByName() throws Exception {
    List<SlotType1> slots = registryObject.getSlot();

    Map<String, SlotType1> slotMap = stHelper.getNameSlotMap(slots);

    for (String slotName : slotMap.keySet()) {
      SlotType1 slot = slotMap.get(slotName);
      assertThat(slot, is(equalTo(stHelper.getSlotByName(slotName, slots))));
    }
  }

  @Test
  public void testGetNameSlotMap() throws Exception {
    RegistryObjectType rot =
        getRegistryObjectFromResource("/registry-package-slots-only-with-dup.xml");
    List<SlotType1> slots = rot.getSlot();

    String specialSlotCase = "serviceType";
    String expectedServiceType = "SOAP";

    Map<String, SlotType1> slotMap = stHelper.getNameSlotMap(slots);

    for (SlotType1 slot : slots) {
      String slotName = slot.getName();
      // Skip special case which will be tested below
      if (specialSlotCase.equals(slotName)) {
        continue;
      }
      assertThat(slotMap, hasKey(slotName));

      SlotType1 mappedSlot = slotMap.get(slotName);
      assertThat(mappedSlot, is(equalTo(slot)));
    }

    // ServiceType slot is repeated in the test xml
    // Testing that the second one (SOAP)is stored
    SlotType1 serviceType = slotMap.get(specialSlotCase);
    assertThat(serviceType, notNullValue());
    String value = stHelper.getStringValues(serviceType).get(0);
    assertThat(value, is(equalTo(expectedServiceType)));
  }

  @Test
  public void getNameSlotMapDuplicateSlotNamesAllowed() throws Exception {
    RegistryObjectType rot =
        getRegistryObjectFromResource("/registry-package-slots-only-with-dup.xml");
    List<SlotType1> slots = rot.getSlot();

    String duplicateSlotCase = "serviceType";
    int expectedSize = 1;
    int expectedServiceTypeSize = 2;

    Map<String, List<SlotType1>> slotMap = stHelper.getNameSlotMapDuplicateSlotNamesAllowed(slots);

    for (SlotType1 slot : slots) {
      String slotName = slot.getName();

      // duplicate slotCase
      if (duplicateSlotCase.equals(slotName)) {
        continue;
      }
      assertThat(slotMap, hasKey(slotName));
      assertThat(slotMap.get(slotName), hasSize(expectedSize));

      SlotType1 mappedSlot = slotMap.get(slotName).get(0);
      assertThat(mappedSlot, is(equalTo(slot)));
    }

    // ServiceType slot is repeated in the test xml
    // Testing that both slots(SOAP & REST) are stored
    List<SlotType1> serviceTypes = slotMap.get(duplicateSlotCase);
    assertThat(serviceTypes, notNullValue());
    assertThat(serviceTypes, hasSize(expectedServiceTypeSize));

    for (SlotType1 serviceTypeSlot : serviceTypes) {
      String value = stHelper.getStringValues(serviceTypeSlot).get(0);
      assertThat(value, anyOf(equalTo("SOAP"), equalTo("REST")));
    }
  }

  @Test
  public void testGetStringValues() throws Exception {
    List<SlotType1> slots = registryObject.getSlot();

    int expectedSize = 1;

    for (SlotType1 slot : slots) {
      String slotTypeUC = slot.getSlotType().toUpperCase();
      // skip location because helper doesn't handle GM_Point
      if (slot.getName().equals("location")) {
        continue;
      }

      if (!slotTypeUC.contains("DATE")) {
        List<String> values = stHelper.getStringValues(slot);

        if (slot.getName().equals("inputDataSources") || slot.getName().equals("dataTypes")) {
          assertThat("SlotName: " + slot.getName(), values, hasSize(2));
        } else {
          assertThat("SlotName: " + slot.getName(), values, hasSize(expectedSize));
        }
      }
    }
  }

  @Test
  public void getDateValues() throws Exception {
    List<SlotType1> slots = registryObject.getSlot();

    int expectedSize = 1;

    for (SlotType1 slot : slots) {
      String slotTypeUC = slot.getSlotType().toUpperCase();

      if (slotTypeUC.contains("DATE")) {
        List<Date> dates = stHelper.getDateValues(slot);

        assertThat(dates, hasSize(expectedSize));
      }
    }
  }

  @Test
  public void createSingleValue() throws Exception {
    String slotName = "slotName";
    String slotValue = "slotValue";
    String slotType = "slotType";

    SlotType1 slot = stHelper.create(slotName, slotValue, slotType);
    assertThat(slot, notNullValue());

    assertThat(slot.getName(), is(equalTo(slotName)));
    assertThat(slot.getSlotType(), is(equalTo(slotType)));
    List<String> values = stHelper.getStringValues(slot);
    assertThat(values, hasSize(1));
    assertThat(values.get(0), is(equalTo(slotValue)));
  }

  @Test
  public void createMultiValue() throws Exception {
    String slotName = "slotName";
    String slotValue1 = "slotValue1";
    String slotValue2 = "slotValue2";
    String slotType = "slotType";

    List<String> values = new ArrayList<>();
    values.add(slotValue1);
    values.add(slotValue2);
    SlotType1 slot = stHelper.create(slotName, values, slotType);
    assertThat(slot, notNullValue());

    assertThat(slot.getName(), is(equalTo(slotName)));
    assertThat(slot.getSlotType(), is(equalTo(slotType)));
    values = stHelper.getStringValues(slot);
    assertThat(values, hasSize(2));
    assertThat(values, contains(slotValue1, slotValue2));
  }

  private static RegistryObjectType getRegistryObjectFromResource(String path)
      throws ParserException {
    RegistryObjectType rot = null;
    JAXBElement<RegistryObjectType> jaxbRegistryObject =
        parser.unmarshal(
            configurator, JAXBElement.class, SlotTypeHelperTest.class.getResourceAsStream(path));

    if (jaxbRegistryObject != null) {
      rot = jaxbRegistryObject.getValue();
    }

    return rot;
  }
}
