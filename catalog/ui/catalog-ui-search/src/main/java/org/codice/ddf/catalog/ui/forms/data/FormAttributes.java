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
package org.codice.ddf.catalog.ui.forms.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.types.Core;
import java.util.Map;
import java.util.Set;

/** Central definition for all the attribute data needed for forms support. */
@SuppressWarnings("squid:S1135" /* Action has a ticket number and will be addressed later */)
public class FormAttributes {
  private FormAttributes() {}

  /**
   * Set of attributes needed to enable sharing on Metacards. Should be refactored to live in a more
   * central location at a later time.
   *
   * <p>TODO DDF-3671 Revisit sharing functionality for metacards
   */
  public static class Sharing implements MetacardType {
    public static final String NAME = "shareable";

    public static final String FORM_SHARING = "metacard.sharing";

    // @formatter:off
    private static final Map<String, AttributeDescriptor> DESCRIPTORS =
        ImmutableMap.of(
            Core.METACARD_OWNER,
            new AttributeDescriptorImpl(
                Core.METACARD_OWNER,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE),
            FORM_SHARING,
            new AttributeDescriptorImpl(
                FORM_SHARING,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                BasicTypes.XML_TYPE));
    // @formatter:on

    @Override
    public String getName() {
      return NAME;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
      return ImmutableSet.copyOf(DESCRIPTORS.values());
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
      return DESCRIPTORS.get(attributeName);
    }
  }

  /**
   * Represents a data structure for storing a form, also referred to as a "query template". The
   * {@code forms.filter} field stores standards compliant Filter XML 2.0 with substitution metadata
   * expressed as a {@code fes:Function} element.
   *
   * @see <a href="http://schemas.opengis.net/filter/2.0/">schemas.opengis.net/filter/2.0/</a>
   * @see org.codice.ddf.catalog.ui.forms.model.JsonModel
   * @see org.codice.ddf.catalog.ui.forms.model.JsonTransformVisitor
   */
  public static class Query implements MetacardType {
    public static final String TAG = "form-query";

    public static final String FORMS_FILTER = "forms.filter";

    // @formatter:off
    private static final Map<String, AttributeDescriptor> DESCRIPTORS =
        ImmutableMap.of(
            FORMS_FILTER,
            new AttributeDescriptorImpl(
                FORMS_FILTER,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.XML_TYPE));
    // @formatter:on

    @Override
    public String getName() {
      return TAG;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
      return ImmutableSet.copyOf(DESCRIPTORS.values());
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
      return DESCRIPTORS.get(attributeName);
    }
  }

  /**
   * Represents a data structure for storing form result customization data. The "detail level" is
   * just a list of attribute descriptor names. They represent the fields that a user is interested
   * in.
   */
  public static class Result implements MetacardType {
    public static final String TAG = "form-result";

    public static final String DETAIL_LEVEL = "forms.detail-level";

    // @formatter:off
    private static final Map<String, AttributeDescriptor> DESCRIPTORS =
        ImmutableMap.of(
            DETAIL_LEVEL,
            new AttributeDescriptorImpl(
                DETAIL_LEVEL,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                BasicTypes.STRING_TYPE));
    // @formatter:on

    @Override
    public String getName() {
      return TAG;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
      return ImmutableSet.copyOf(DESCRIPTORS.values());
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
      return DESCRIPTORS.get(attributeName);
    }
  }
}
