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
package org.codice.ddf.registry.schemabindings.converter.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.OrganizationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class OrganizationWebConverter extends RegistryObjectWebConverter {
  public static final String ADDRESS_KEY = "Address";

  public static final String EMAIL_ADDRESS_KEY = "EmailAddress";

  public static final String PARENT = "parent";

  public static final String PRIMARY_CONTACT = "primaryContact";

  public static final String TELEPHONE_KEY = "TelephoneNumber";

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the OrganizationType provided. The
   * following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>PARENT = "parent"; PRIMARY_CONTACT = "primaryContact"; ADDRESS_KEY = "Address";
   * EMAIL_ADDRESS_KEY = "EmailAddress"; TELEPHONE_KEY = "TelephoneNumber";
   *
   * <p>This will also try to parse RegistryObjectType values to the map.
   *
   * <p>Uses: PostalAddressWebConverter EmailAddressWebConverter TelephoneNumberWebConverter
   *
   * @param organization the OrganizationType to be converted into a map, null returns empty Map
   * @return Map<String, Object> representation of the OrganizationType provided
   */
  public Map<String, Object> convert(OrganizationType organization) {
    Map<String, Object> organizationMap = new HashMap<>();
    if (organization == null) {
      return organizationMap;
    }

    webMapHelper.putAllIfNotEmpty(organizationMap, super.convertRegistryObject(organization));

    if (organization.isSetAddress()) {
      List<Map<String, Object>> addresses = new ArrayList<>();
      PostalAddressWebConverter addressConverter = new PostalAddressWebConverter();

      for (PostalAddressType address : organization.getAddress()) {
        Map<String, Object> addressMap = addressConverter.convert(address);

        if (MapUtils.isNotEmpty(addressMap)) {
          addresses.add(addressMap);
        }
      }

      webMapHelper.putIfNotEmpty(organizationMap, ADDRESS_KEY, addresses);
    }

    if (organization.isSetEmailAddress()) {
      List<Map<String, Object>> emailAddresses = new ArrayList<>();
      EmailAddressWebConverter emailConverter = new EmailAddressWebConverter();

      for (EmailAddressType email : organization.getEmailAddress()) {
        Map<String, Object> emailMap = emailConverter.convert(email);

        if (MapUtils.isNotEmpty(emailMap)) {
          emailAddresses.add(emailMap);
        }
      }

      webMapHelper.putIfNotEmpty(organizationMap, EMAIL_ADDRESS_KEY, emailAddresses);
    }

    webMapHelper.putIfNotEmpty(organizationMap, PARENT, organization.getParent());
    webMapHelper.putIfNotEmpty(organizationMap, PRIMARY_CONTACT, organization.getPrimaryContact());

    if (organization.isSetTelephoneNumber()) {
      List<Map<String, Object>> telephoneNumbers = new ArrayList<>();
      TelephoneNumberWebConverter telephoneConverter = new TelephoneNumberWebConverter();

      for (TelephoneNumberType telephone : organization.getTelephoneNumber()) {
        Map<String, Object> telephoneMap = telephoneConverter.convert(telephone);

        if (MapUtils.isNotEmpty(telephoneMap)) {
          telephoneNumbers.add(telephoneMap);
        }
      }

      webMapHelper.putIfNotEmpty(organizationMap, TELEPHONE_KEY, telephoneNumbers);
    }

    return organizationMap;
  }
}
