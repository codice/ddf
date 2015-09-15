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
 **/

package ddf.catalog.util.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.osgi.framework.Constants;

import java.util.Calendar;

public class TestServiceFactoryImpl {

    private ServiceReference mockServiceReference1;
    private ServiceReference mockServiceReference2;
    private BundleContext mockBundleContext;
    private Calendar mockCalendar1;
    private Calendar mockCalendar2;
    private ServiceFactoryImpl<Calendar> calendarFactory;

    @Before
    public void testSetup() {
        this.mockBundleContext = buildMockBundleContext();
        this.mockServiceReference1 = buildMockServiceReference(1, 101L);
        this.mockServiceReference2 = buildMockServiceReference(2, 201L);
        this.mockCalendar1 = buildMockCalendar();
        this.mockCalendar2 = buildMockCalendar();
        this.calendarFactory = buildCalendarFactory();

        when(mockBundleContext.getService(mockServiceReference1)).thenReturn(mockCalendar1);
        when(mockBundleContext.getService(mockServiceReference2)).thenReturn(mockCalendar2);
        when(mockServiceReference1.compareTo(mockServiceReference2)).thenReturn( -1 );
        when(mockServiceReference2.compareTo(mockServiceReference1)).thenReturn( 1 );
        when(calendarFactory.getBundleContext()).thenReturn(mockBundleContext);
    }

    @Test
    public void testBind_Unbind() {
        calendarFactory.bindService(mockServiceReference1);
        calendarFactory.bindService(mockServiceReference2);
        Calendar calendar = calendarFactory.getService();
        Assert.assertEquals(mockCalendar2, calendar);

        calendarFactory.unbindService(mockServiceReference2);
        calendar = calendarFactory.getService();
        Assert.assertEquals(mockCalendar1, calendar);

        calendarFactory.unbindService(mockServiceReference1);
        calendar = calendarFactory.getService();
        assertNull(calendar);
    }

    private ServiceFactoryImpl buildCalendarFactory() {
        ServiceFactoryImpl serviceFactoryImpl = spy(new ServiceFactoryImpl(Calendar.class));
        return serviceFactoryImpl;
    }

    private ServiceReference buildMockServiceReference(Integer serviceRanking, Long serviceId) {
        ServiceReference serviceReference = mock(ServiceReference.class);
        when(serviceReference.getProperty(Constants.SERVICE_RANKING)).thenReturn(serviceRanking);
        when(serviceReference.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
        return serviceReference;
    }

    private BundleContext buildMockBundleContext() {
        BundleContext mockContext = mock(BundleContext.class);
        return mockContext;
    }

    private Calendar buildMockCalendar() {
        Calendar mockCalendar = mock(Calendar.class);
        return mockCalendar;
    }
}
