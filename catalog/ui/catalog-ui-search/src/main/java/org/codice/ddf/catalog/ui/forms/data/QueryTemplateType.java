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

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.SecurityAttributes;
import java.util.Set;
import org.codice.ddf.catalog.ui.forms.filter.TransformVisitor;
import org.codice.ddf.catalog.ui.metacard.query.data.metacard.QueryMetacardTypeImpl;

/**
 * Represents a data structure for storing a query template. The {@code template.query.filter} field
 * stores standards compliant Filter XML 2.0 with substitution metadata expressed as a {@code
 * fes:Function} element.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 *
 * @see <a href="http://schemas.opengis.net/filter/2.0/">schemas.opengis.net/filter/2.0/</a>
 * @see org.codice.ddf.catalog.ui.forms.model.pojo.FormTemplate
 * @see org.codice.ddf.catalog.ui.forms.model.pojo.FilterNode
 * @see TransformVisitor
 */
public class QueryTemplateType extends MetacardTypeImpl {
  public static final String QUERY_TEMPLATE_TAG = "query-template";

  public static final String QUERY_TEMPLATE_FILTER = "ui.template-filter";

  // @formatter:off
  private static final Set<AttributeDescriptor> QUERY_TEMPLATE_ATTRIBUTES =
      ImmutableSet.of(
          new AttributeDescriptorImpl(
              QUERY_TEMPLATE_FILTER,
              false /* indexed */,
              true /* stored */,
              false /* tokenized */,
              false /* multivalued */,
              BasicTypes.XML_TYPE));
  // @formatter:on

  public QueryTemplateType() {
    super(
        QUERY_TEMPLATE_TAG,
        ImmutableSet.<AttributeDescriptor>builder()
            .addAll(new CoreAttributes().getAttributeDescriptors())
            .addAll(new SecurityAttributes().getAttributeDescriptors())
            .addAll(new QueryMetacardTypeImpl().getAttributeDescriptors())
            .addAll(QUERY_TEMPLATE_ATTRIBUTES)
            .build());
  }
}
