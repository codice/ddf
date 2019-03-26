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
package ddf.catalog.data.inject;

import static ddf.catalog.data.impl.MetacardImpl.BASIC_METACARD;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeRegistryImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.InjectableAttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Core;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

public class AttributeInjectorImplTest {
  private static final String NITF = "nitf";

  private static final MetacardType NITF_TYPE =
      new MetacardTypeImpl(NITF, BASIC_METACARD.getAttributeDescriptors());

  private final String globalAttributeName = "foo";

  private final String basicAttributeName = "bar";

  private final String basicAndNitfAttributeName = "baz";

  private final AttributeDescriptor globalAttribute =
      new AttributeDescriptorImpl(globalAttributeName, true, true, true, true, BasicTypes.XML_TYPE);

  private final AttributeDescriptor basicAttribute =
      new AttributeDescriptorImpl(basicAttributeName, true, true, true, true, BasicTypes.XML_TYPE);

  private final AttributeDescriptor basicAndNitfAttribute =
      new AttributeDescriptorImpl(
          basicAndNitfAttributeName, true, true, true, true, BasicTypes.XML_TYPE);

  private final InjectableAttribute globalInjection =
      new InjectableAttributeImpl(globalAttributeName, null);

  private final InjectableAttribute basicInjection =
      new InjectableAttributeImpl(basicAttributeName, Sets.newHashSet(BASIC_METACARD.getName()));

  private final InjectableAttribute basicAndNitfInjection =
      new InjectableAttributeImpl(
          basicAndNitfAttributeName, Sets.newHashSet(BASIC_METACARD.getName(), NITF));

  private AttributeInjectorImpl attributeInjector;

  @Before
  public void setUp() {
    AttributeRegistry attributeRegistry = new AttributeRegistryImpl();
    attributeInjector = new AttributeInjectorImpl(attributeRegistry);

    attributeRegistry.register(globalAttribute);
    attributeRegistry.register(basicAttribute);
    attributeRegistry.register(basicAndNitfAttribute);

    attributeInjector.setInjectableAttributes(
        Lists.newArrayList(globalInjection, basicInjection, basicAndNitfInjection));
  }

  @Test
  public void testInjectIntoMetacardType() {
    final MetacardType expectedBasicMetacardType =
        new MetacardTypeImpl(
            BASIC_METACARD.getName(),
            BASIC_METACARD,
            Sets.newHashSet(globalAttribute, basicAttribute, basicAndNitfAttribute));
    assertThat(attributeInjector.injectAttributes(BASIC_METACARD), is(expectedBasicMetacardType));

    final MetacardType expectedNitfMetacardType =
        new MetacardTypeImpl(
            NITF, NITF_TYPE, Sets.newHashSet(globalAttribute, basicAndNitfAttribute));
    assertThat(attributeInjector.injectAttributes(NITF_TYPE), is(expectedNitfMetacardType));
  }

  @Test
  public void testInjectIntoMetacard() {
    final String title = "title";
    final Date created = new Date();
    final MetacardImpl basicMetacard = new MetacardImpl();
    basicMetacard.setTitle(title);
    basicMetacard.setCreatedDate(created);

    final MetacardType expectedBasicMetacardType =
        new MetacardTypeImpl(
            BASIC_METACARD.getName(),
            BASIC_METACARD,
            Sets.newHashSet(globalAttribute, basicAttribute, basicAndNitfAttribute));

    final Metacard injectedBasicMetacard = attributeInjector.injectAttributes(basicMetacard);
    assertThat(injectedBasicMetacard.getMetacardType(), is(expectedBasicMetacardType));
    assertThat(injectedBasicMetacard.getTitle(), is(title));
    assertThat(injectedBasicMetacard.getAttribute(Core.CREATED).getValue(), is(created));

    final MetacardImpl nitfMetacard = new MetacardImpl(NITF_TYPE);
    nitfMetacard.setTitle(title);
    nitfMetacard.setCreatedDate(created);

    final MetacardType expectedNitfMetacardType =
        new MetacardTypeImpl(
            NITF, NITF_TYPE, Sets.newHashSet(globalAttribute, basicAndNitfAttribute));

    final Metacard injectedNitfMetacard = attributeInjector.injectAttributes(nitfMetacard);
    assertThat(injectedNitfMetacard.getMetacardType(), is(expectedNitfMetacardType));
    assertThat(injectedNitfMetacard.getTitle(), is(title));
    assertThat(injectedNitfMetacard.getAttribute(Core.CREATED).getValue(), is(created));
  }

  @Test
  public void testInjectNothingIntoMetacardType() {
    attributeInjector.setInjectableAttributes(Lists.newArrayList(basicInjection));

    final MetacardType injectedMetacardType = attributeInjector.injectAttributes(NITF_TYPE);

    assertThat(injectedMetacardType, is(sameInstance(NITF_TYPE)));
  }

  @Test
  public void testInjectNothingIntoMetacard() {
    attributeInjector.setInjectableAttributes(Lists.newArrayList(basicInjection));

    final Metacard original = new MetacardImpl(NITF_TYPE);
    final Metacard injected = attributeInjector.injectAttributes(original);

    assertThat(injected, is(sameInstance(original)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullMetacardType() {
    attributeInjector.injectAttributes((MetacardType) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullMetacard() {
    attributeInjector.injectAttributes((Metacard) null);
  }
}
