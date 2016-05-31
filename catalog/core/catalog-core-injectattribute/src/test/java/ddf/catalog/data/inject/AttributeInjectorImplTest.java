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
package ddf.catalog.data.inject;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static ddf.catalog.data.impl.BasicTypes.BASIC_METACARD;

import java.util.Date;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

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

@PrepareForTest(FrameworkUtil.class)
public class AttributeInjectorImplTest {
    private static final String NITF = "nitf";

    private static final MetacardType NITF_TYPE = new MetacardTypeImpl(NITF,
            BASIC_METACARD.getAttributeDescriptors());

    private final String globalAttributeName = "foo";

    private final String basicAttributeName = "bar";

    private final String basicAndNitfAttributeName = "baz";

    private final AttributeDescriptor globalAttribute = new AttributeDescriptorImpl(
            globalAttributeName,
            true,
            true,
            true,
            true,
            BasicTypes.XML_TYPE);

    private final AttributeDescriptor basicAttribute = new AttributeDescriptorImpl(
            basicAttributeName,
            true,
            true,
            true,
            true,
            BasicTypes.XML_TYPE);

    private final AttributeDescriptor basicAndNitfAttribute = new AttributeDescriptorImpl(
            basicAndNitfAttributeName,
            true,
            true,
            true,
            true,
            BasicTypes.XML_TYPE);

    private final InjectableAttribute globalInjection = new InjectableAttributeImpl(
            globalAttributeName,
            null);

    private final InjectableAttribute basicInjection = new InjectableAttributeImpl(
            basicAttributeName,
            Sets.newHashSet(BASIC_METACARD.getName()));

    private final InjectableAttribute basicAndNitfInjection = new InjectableAttributeImpl(
            basicAndNitfAttributeName,
            Sets.newHashSet(BASIC_METACARD.getName(), NITF));

    @Rule
    public PowerMockRule powerMockRule = new PowerMockRule();

    private BundleContext mockBundleContext = mock(BundleContext.class);

    private AttributeRegistry attributeRegistry;

    private AttributeInjectorImpl attributeInjector;

    @Before
    public void setUp() {
        attributeRegistry = new AttributeRegistryImpl();
        attributeInjector = new AttributeInjectorImpl(attributeRegistry);

        mockStatic(FrameworkUtil.class);
        Bundle mockBundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(AttributeInjectorImpl.class)).thenReturn(mockBundle);
        when(mockBundle.getBundleContext()).thenReturn(mockBundleContext);

        setUpInjectableAttributes();
    }

    private void setUpInjectableAttributes() {
        attributeRegistry.register(globalAttribute);
        attributeRegistry.register(basicAttribute);
        attributeRegistry.register(basicAndNitfAttribute);

        bindService(globalInjection);
        bindService(basicInjection);
        bindService(basicAndNitfInjection);
    }

    @Test
    public void testInjectIntoMetacardType() {
        final MetacardType expectedBasicMetacardType =
                new MetacardTypeImpl(BASIC_METACARD.getName(),
                        BASIC_METACARD,
                        Sets.newHashSet(globalAttribute, basicAttribute, basicAndNitfAttribute));
        assertThat(attributeInjector.injectAttributes(BASIC_METACARD),
                is(expectedBasicMetacardType));

        final MetacardType expectedNitfMetacardType = new MetacardTypeImpl(NITF,
                NITF_TYPE,
                Sets.newHashSet(globalAttribute, basicAndNitfAttribute));
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
                new MetacardTypeImpl(BASIC_METACARD.getName(),
                        BASIC_METACARD,
                        Sets.newHashSet(globalAttribute, basicAttribute, basicAndNitfAttribute));

        final Metacard injectedBasicMetacard = attributeInjector.injectAttributes(basicMetacard);
        assertThat(injectedBasicMetacard.getMetacardType(), is(expectedBasicMetacardType));
        assertThat(injectedBasicMetacard.getTitle(), is(title));
        assertThat(injectedBasicMetacard.getCreatedDate(), is(created));

        final MetacardImpl nitfMetacard = new MetacardImpl(NITF_TYPE);
        nitfMetacard.setTitle(title);
        nitfMetacard.setCreatedDate(created);

        final MetacardType expectedNitfMetacardType = new MetacardTypeImpl(NITF,
                NITF_TYPE,
                Sets.newHashSet(globalAttribute, basicAndNitfAttribute));

        final Metacard injectedNitfMetacard = attributeInjector.injectAttributes(nitfMetacard);
        assertThat(injectedNitfMetacard.getMetacardType(), is(expectedNitfMetacardType));
        assertThat(injectedNitfMetacard.getTitle(), is(title));
        assertThat(injectedNitfMetacard.getCreatedDate(), is(created));
    }

    @Test
    public void testUnbind() {
        unbindService(globalInjection);
        unbindService(basicAndNitfInjection);

        assertThat(attributeInjector.injectAttributes(NITF_TYPE), is(NITF_TYPE));
        final Metacard nitfMetacard = new MetacardImpl(NITF_TYPE);
        assertThat(attributeInjector.injectAttributes(nitfMetacard),
                is(sameInstance(nitfMetacard)));

        final MetacardType expectedBasicMetacardType =
                new MetacardTypeImpl(BASIC_METACARD.getName(),
                        BASIC_METACARD,
                        Sets.newHashSet(basicAttribute));
        assertThat(attributeInjector.injectAttributes(BASIC_METACARD),
                is(expectedBasicMetacardType));

        final MetacardImpl basicMetacard = new MetacardImpl();
        assertThat(attributeInjector.injectAttributes(basicMetacard)
                .getMetacardType(), is(expectedBasicMetacardType));
    }

    private void bindService(InjectableAttribute injectableAttribute) {
        handleService(injectableAttribute, attributeInjector::bind);
    }

    private void unbindService(InjectableAttribute injectableAttribute) {
        handleService(injectableAttribute, attributeInjector::unbind);
    }

    private void handleService(InjectableAttribute injectableAttribute,
            Consumer<ServiceReference<InjectableAttribute>> consumer) {
        ServiceReference<InjectableAttribute> serviceRef = mock(ServiceReference.class);
        when(mockBundleContext.getService(serviceRef)).thenReturn(injectableAttribute);
        consumer.accept(serviceRef);
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
