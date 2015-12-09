/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.store.felix;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.google.common.collect.Sets.newHashSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.codice.ddf.configuration.store.ConfigurationFileException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FelixPersistenceStrategyTest {

    @Mock
    private PropertyConverter propertyConverter;

    private Dictionary<String, Object> properties;

    private ByteArrayOutputStream outputSream;

    private class FelixPersistenceStrategyUnderTest extends FelixPersistenceStrategy {
        private String fileContent;

        FelixPersistenceStrategyUnderTest(String fileContent) {
            this.fileContent = fileContent;
        }

        @Override
        PropertyConverter createPropertyConverter(StringBuilder filteredOutput) {
            filteredOutput.append(fileContent);
            return propertyConverter;
        }
    }

    @Before
    public void setUp() throws Exception {
        properties = new Hashtable<>();
        properties.put("key1", "value1");
        outputSream = new ByteArrayOutputStream();
    }

    @Test
    public void testReadEmptyFile() throws Exception {
        FelixPersistenceStrategy felixPersistenceStrategy =
                new FelixPersistenceStrategyUnderTest("");
        InputStream inputStream = new ByteArrayInputStream("".getBytes());

        Dictionary<String, Object> properties = felixPersistenceStrategy.read(inputStream);

        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), equalTo(0));
        verify(propertyConverter, never()).accept(anyString());
    }

    @Test
    public void testReadMultiLineFile() throws Exception {
        when(propertyConverter.getPropertyNames()).thenReturn(newHashSet("key1", "key2"));

        String line1 = "key1=\"value1\"";
        String line2 = "key2=\"value2\"";
        String fileContent = line1 + "\r\n" + line2 + "\r\n";

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategyUnderTest(
                fileContent);
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        Dictionary<String, Object> properties = felixPersistenceStrategy.read(inputStream);

        assertThat(properties, is(notNullValue()));
        assertThat(properties.get("key1"), equalTo("value1"));
        assertThat(properties.get("key2"), equalTo("value2"));

        InOrder inOrder = inOrder(propertyConverter);
        inOrder.verify(propertyConverter)
                .accept(line1);
        inOrder.verify(propertyConverter)
                .accept(line2);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testReadWhenConfigurationHandlerFindsBadValueType() throws Exception {
        when(propertyConverter.getPropertyNames()).thenReturn(newHashSet("key1", "key2"));

        String line1 = "key1=\"value1\"";
        String line2 = "key2=Z\"value2\"";
        String fileContent = line1 + "\r\n" + line2 + "\r\n";

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategyUnderTest(
                fileContent);
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        felixPersistenceStrategy.read(inputStream);
    }

    /**
     * Note that Felix' {@link org.apache.felix.cm.file.ConfigurationHandler#read(InputStream)}
     * does not fail when a boolean's value is wrong, it just sets it to {@code false}.
     */
    @Test
    public void testReadWhenConfigurationHandlerFindsBadBoolean() throws Exception {
        when(propertyConverter.getPropertyNames()).thenReturn(newHashSet("key1", "key2"));

        String line1 = "key1=\"value1\"";
        String line2 = "key2=B\"invalid\"";
        String fileContent = line1 + "\r\n" + line2 + "\r\n";

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategyUnderTest(
                fileContent);
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        Dictionary<String, Object> properties = felixPersistenceStrategy.read(inputStream);

        assertThat(properties, is(notNullValue()));
        assertThat(properties.get("key1"), equalTo("value1"));
        assertThat(properties.get("key2"), equalTo(false));
    }

    @Test(expected = ConfigurationFileException.class)
    public void testReadWhenConfigurationHandlerFindsBadInteger() throws Exception {
        when(propertyConverter.getPropertyNames()).thenReturn(newHashSet("key1", "key2"));

        String line1 = "key1=\"value1\"";
        String line2 = "key2=I\"ABCD\"";
        String fileContent = line1 + "\r\n" + line2 + "\r\n";

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategyUnderTest(
                fileContent);
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        felixPersistenceStrategy.read(inputStream);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testReadWhenConfigurationHandlerFindsBadFloat() throws Exception {
        when(propertyConverter.getPropertyNames()).thenReturn(newHashSet("key1", "key2"));

        String line1 = "key1=\"value1\"";
        String line2 = "key2=F\"ABCD\"";
        String fileContent = line1 + "\r\n" + line2 + "\r\n";

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategyUnderTest(
                fileContent);
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        felixPersistenceStrategy.read(inputStream);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testReadWhenConfigurationHandlerFindsBadArray() throws Exception {
        when(propertyConverter.getPropertyNames()).thenReturn(newHashSet("key1", "key2"));

        String line1 = "key1=\"value1\"";
        String line2 = "key2=F\"[\"1.1\", \"ABCD\"]";
        String fileContent = line1 + "\r\n" + line2 + "\r\n";

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategyUnderTest(
                fileContent);
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        felixPersistenceStrategy.read(inputStream);
    }

    @Test(expected = ConfigurationFileException.class)
    public void testReadWhenConfigurationHandlerFindsBadCollection() throws Exception {
        when(propertyConverter.getPropertyNames()).thenReturn(newHashSet("key1", "key2"));

        String line1 = "key1=\"value1\"";
        String line2 = "key2=I\"(\"100\", \"ABCD\")";
        String fileContent = line1 + "\r\n" + line2 + "\r\n";

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategyUnderTest(
                fileContent);
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        felixPersistenceStrategy.read(inputStream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadWithNullInputStream() throws Exception {
        new FelixPersistenceStrategy().read(null);
    }

    @Test
    public void write() throws IOException {

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategy();
        felixPersistenceStrategy.write(outputSream, properties);

        assertThat(outputSream.toString(), equalTo("key1=\"value1\"\r\n"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteWithNullOutputStream() throws IOException {
        new FelixPersistenceStrategy().write(null, properties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteWithNullProperties() throws IOException {
        new FelixPersistenceStrategy().write(outputSream, null);
    }
}
