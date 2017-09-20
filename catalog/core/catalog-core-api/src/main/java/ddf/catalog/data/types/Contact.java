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
package ddf.catalog.data.types;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Contact {

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the physical address of the contributor
   * of the {@link Metacard}. <br>
   */
  String CONTRIBUTOR_ADDRESS = "contact.contributor-address";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the email of the contributor of the
   * {@link Metacard}. <br>
   */
  String CONTRIBUTOR_EMAIL = "contact.contributor-email";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the name of the contributor of the {@link
   * Metacard}. <br>
   */
  String CONTRIBUTOR_NAME = "contact.contributor-name";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the phone number of the contributor of
   * the {@link Metacard}. <br>
   */
  String CONTRIBUTOR_PHONE = "contact.contributor-phone";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the physical address of the creator of
   * the {@link Metacard}. <br>
   */
  String CREATOR_ADDRESS = "contact.creator-address";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the email of the creator of the {@link
   * Metacard}. <br>
   */
  String CREATOR_EMAIL = "contact.creator-email";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the name of the creator of the {@link
   * Metacard}. <br>
   */
  String CREATOR_NAME = "contact.creator-name";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the phone number of the creator of the
   * {@link Metacard}. <br>
   */
  String CREATOR_PHONE = "contact.creator-phone";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the physical address of the POC of the
   * {@link Metacard}. <br>
   */
  String POINT_OF_CONTACT_ADDRESS = "contact.point-of-contact-address";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the email of the POC of the {@link
   * Metacard}. <br>
   */
  String POINT_OF_CONTACT_EMAIL = "contact.point-of-contact-email";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the name of the POC of the {@link
   * Metacard}. <br>
   */
  String POINT_OF_CONTACT_NAME = "contact.point-of-contact-name";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the phone number of the POC of the {@link
   * Metacard}. <br>
   */
  String POINT_OF_CONTACT_PHONE = "contact.point-of-contact-phone";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the physical address of the publisher of
   * the {@link Metacard}. <br>
   */
  String PUBLISHER_ADDRESS = "contact.publisher-address";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the email of the publisher of the {@link
   * Metacard}. <br>
   */
  String PUBLISHER_EMAIL = "contact.publisher-email";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the name of the publisher of the {@link
   * Metacard}. <br>
   */
  String PUBLISHER_NAME = "contact.publisher-name";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the phone number of the publisher of the
   * {@link Metacard}. <br>
   */
  String PUBLISHER_PHONE = "contact.publisher-phone";
}
