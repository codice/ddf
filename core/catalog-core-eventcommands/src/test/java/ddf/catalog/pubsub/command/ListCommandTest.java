/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.pubsub.command;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.mockito.ArgumentCaptor;

public class ListCommandTest {
    static {
        BasicConfigurator.configure();
    }

    private static final String SUBSCRIPTION_ID_PROPERTY_KEY = "subscription-id";

    private static final String MY_SUBSCRIPTION_ID = "my.contextual.id|http://172.18.14.169:8088/mockCatalogEventConsumerBinding?WSDL";

    private static final String YOUR_SUBSCRIPTION_ID = "your.contextual.id|http://172.18.14.169:8088/mockCatalogEventConsumerBinding?WSDL";

    /**
     * Test subscriptions:list command with no args. Should return all registered subscriptions.
     * 
     * @throws Exception
     */
    @Test
    public void testListNoArgsSubscriptionsFound() throws Exception {
        ListCommand listCommand = new ListCommand();

        BundleContext bundleContext = mock(BundleContext.class);
        listCommand.setBundleContext(bundleContext);

        ServiceReference mySubscription = mock(ServiceReference.class);
        when(mySubscription.getPropertyKeys()).thenReturn(
                new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
        when(mySubscription.getProperty("subscription-id")).thenReturn(MY_SUBSCRIPTION_ID);

        ServiceReference yourSubscription = mock(ServiceReference.class);
        when(yourSubscription.getPropertyKeys()).thenReturn(
                new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
        when(yourSubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(
                YOUR_SUBSCRIPTION_ID);

        ServiceReference[] refs = new ServiceReference[] {mySubscription, yourSubscription};
        when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), anyString()))
                .thenReturn(refs);

        PrintStream realSystemOut = System.out;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        System.setOut(new PrintStream(buffer));

        // when
        listCommand.doExecute();

        /* cleanup */
        System.setOut(realSystemOut);

        // then
        List<String> linesWithText = getConsoleOutputText(buffer);
        assertThat(linesWithText.size(), is(4));
        assertThat(
                linesWithText,
                hasItems("Total subscriptions found: 2", ListCommand.CYAN_CONSOLE_COLOR
                        + ListCommand.SUBSCRIPTION_ID_COLUMN_HEADER
                        + ListCommand.DEFAULT_CONSOLE_COLOR, MY_SUBSCRIPTION_ID,
                        YOUR_SUBSCRIPTION_ID));

        buffer.close();
    }

    /**
     * Test subscriptions:list command with no args. Should return no registered subscriptions.
     * 
     * @throws Exception
     */
    @Test
    public void testListNoArgsNoSubscriptionsFound() throws Exception {
        ListCommand listCommand = new ListCommand();

        BundleContext bundleContext = mock(BundleContext.class);
        listCommand.setBundleContext(bundleContext);
        when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), anyString()))
                .thenReturn(new ServiceReference[] {});

        PrintStream realSystemOut = System.out;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        System.setOut(new PrintStream(buffer));

        // when
        listCommand.doExecute();

        /* cleanup */
        System.setOut(realSystemOut);

        // then
        assertThat(buffer.toString(), startsWith(ListCommand.RED_CONSOLE_COLOR
                + ListCommand.NO_SUBSCRIPTIONS_FOUND_MSG + ListCommand.DEFAULT_CONSOLE_COLOR));

        buffer.close();
    }

    /**
     * Test subscriptions:list command with one subscription ID argument not matching any registered
     * subscriptions. Should return no subscriptions.
     * 
     * @throws Exception
     */
    @Test
    public void testListOneNonMatchingSubscriptionIdArg() throws Exception {
        // Setup argument captor for LDAP filter that will be passed in to getServiceReferences()
        // call
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        ListCommand listCommand = new ListCommand();

        BundleContext bundleContext = mock(BundleContext.class);
        listCommand.setBundleContext(bundleContext);

        when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), anyString()))
                .thenReturn(new ServiceReference[] {});

        PrintStream realSystemOut = System.out;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        System.setOut(new PrintStream(buffer));

        // when
        listCommand.id = MY_SUBSCRIPTION_ID;
        listCommand.doExecute();

        /* cleanup */
        System.setOut(realSystemOut);

        // then
        assertThat(buffer.toString(), startsWith(ListCommand.RED_CONSOLE_COLOR
                + ListCommand.NO_SUBSCRIPTIONS_FOUND_MSG + ListCommand.DEFAULT_CONSOLE_COLOR));

        buffer.close();

        // Verify the LDAP filter passed in when mock BundleContext.getServiceReferences() was
        // called.
        verify(bundleContext).getServiceReferences(anyString(), argument.capture());
        String expectedLdapFilter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + MY_SUBSCRIPTION_ID
                + ")";
        assertThat(argument.getValue(), containsString(expectedLdapFilter));
    }

    /**
     * Test subscriptions:list command with one subscription ID argument. Should return the one
     * matching registered subscriptions.
     * 
     * @throws Exception
     */
    @Test
    public void testListOneMatchingSubscriptionIdArg() throws Exception {
        // Setup argument captor for LDAP filter that will be passed in to getServiceReferences()
        // call
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        ListCommand listCommand = new ListCommand();

        BundleContext bundleContext = mock(BundleContext.class);
        listCommand.setBundleContext(bundleContext);

        ServiceReference mySubscription = mock(ServiceReference.class);
        when(mySubscription.getPropertyKeys()).thenReturn(
                new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
        when(mySubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(
                MY_SUBSCRIPTION_ID);

        ServiceReference yourSubscription = mock(ServiceReference.class);
        when(yourSubscription.getPropertyKeys()).thenReturn(
                new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
        when(yourSubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(
                YOUR_SUBSCRIPTION_ID);

        when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), anyString()))
                .thenReturn(new ServiceReference[] {mySubscription});

        PrintStream realSystemOut = System.out;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        System.setOut(new PrintStream(buffer));

        // when
        listCommand.id = MY_SUBSCRIPTION_ID;
        listCommand.doExecute();

        /* cleanup */
        System.setOut(realSystemOut);

        // then
        List<String> linesWithText = getConsoleOutputText(buffer);
        assertThat(linesWithText.size(), is(3));
        assertThat(
                linesWithText,
                hasItems("Total subscriptions found: 1", ListCommand.CYAN_CONSOLE_COLOR
                        + ListCommand.SUBSCRIPTION_ID_COLUMN_HEADER
                        + ListCommand.DEFAULT_CONSOLE_COLOR, MY_SUBSCRIPTION_ID));

        buffer.close();

        // Verify the LDAP filter passed in when mock BundleContext.getServiceReferences() was
        // called.
        verify(bundleContext).getServiceReferences(anyString(), argument.capture());
        String expectedLdapFilter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + MY_SUBSCRIPTION_ID
                + ")";
        assertThat(argument.getValue(), containsString(expectedLdapFilter));
    }

    /**
     * Test subscriptions:list command with the LDAP filter arg specified, e.g., subscriptions:list
     * -f "(subscription-id=my*)" Should return matching subscriptions.
     * 
     * @throws Exception
     */
    @Test
    public void testListWithLdapFilterArg() throws Exception {
        // Setup argument captor for LDAP filter that will be passed in to getServiceReferences()
        // call
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        ListCommand listCommand = new ListCommand();

        BundleContext bundleContext = mock(BundleContext.class);
        listCommand.setBundleContext(bundleContext);

        ServiceReference mySubscription = mock(ServiceReference.class);
        when(mySubscription.getPropertyKeys()).thenReturn(
                new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
        when(mySubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(
                MY_SUBSCRIPTION_ID);
        when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), anyString()))
                .thenReturn(new ServiceReference[] {mySubscription});

        String ldapFilter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=my*)";

        PrintStream realSystemOut = System.out;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        System.setOut(new PrintStream(buffer));

        // when
        listCommand.id = ldapFilter;
        listCommand.ldapFilter = true;
        listCommand.doExecute();

        /* cleanup */
        System.setOut(realSystemOut);

        // then
        List<String> linesWithText = getConsoleOutputText(buffer);
        assertThat(linesWithText.size(), is(3));
        assertThat(
                linesWithText,
                hasItems("Total subscriptions found: 1", ListCommand.CYAN_CONSOLE_COLOR
                        + ListCommand.SUBSCRIPTION_ID_COLUMN_HEADER
                        + ListCommand.DEFAULT_CONSOLE_COLOR, MY_SUBSCRIPTION_ID));

        buffer.close();

        // Verify the LDAP filter passed in when mock BundleContext.getServiceReferences() was
        // called.
        verify(bundleContext).getServiceReferences(anyString(), argument.capture());
        assertThat(argument.getValue(), containsString(ldapFilter));
    }

    private List<String> getConsoleOutputText(ByteArrayOutputStream buffer) {
        // Get console output as individual lines that are not whitespace or only new lines
        List<String> linesWithText = new ArrayList<String>();
        String[] lines = buffer.toString().split("\n");
        if (lines != null) {
            for (String line : lines) {
                String text = StringUtils.chomp(line);
                if (!StringUtils.isEmpty(text)) {
                    linesWithText.add(text);
                }
            }
        }

        return linesWithText;
    }
}
