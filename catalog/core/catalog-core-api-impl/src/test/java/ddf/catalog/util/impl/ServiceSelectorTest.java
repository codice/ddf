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
package ddf.catalog.util.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Comparator;
import java.util.SortedSet;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ServiceSelectorTest {

  private ServiceReference mockServiceReference1;

  private ServiceReference mockServiceReference2;

  private BundleContext mockBundleContext;

  private Calendar mockCalendar1;

  private Calendar mockCalendar2;

  @Before
  public void setUp() {
    this.mockBundleContext = buildMockBundleContext();
    this.mockServiceReference1 = buildMockServiceReference(1, 101L);
    this.mockServiceReference2 = buildMockServiceReference(2, 201L);

    this.mockCalendar1 = buildMockCalendar();
    this.mockCalendar2 = buildMockCalendar();

    when(mockBundleContext.getService(mockServiceReference1)).thenReturn(mockCalendar1);
    when(mockBundleContext.getService(mockServiceReference2)).thenReturn(mockCalendar2);
    when(mockServiceReference1.compareTo(mockServiceReference2)).thenReturn(-1);
    when(mockServiceReference2.compareTo(mockServiceReference1)).thenReturn(1);
  }

  @Test
  public void testBindUnbind() {
    ServiceSelector<Calendar> calendarServiceSelector = spy(new ServiceSelector());
    when(calendarServiceSelector.getBundleContext()).thenReturn(mockBundleContext);

    calendarServiceSelector.bindService(mockServiceReference1);
    calendarServiceSelector.bindService(mockServiceReference2);
    Calendar calendar = calendarServiceSelector.getService();
    assertThat(calendar, is(mockCalendar2));

    calendarServiceSelector.unbindService(mockServiceReference2);
    calendar = calendarServiceSelector.getService();
    assertThat(calendar, is(mockCalendar1));

    calendarServiceSelector.unbindService(mockServiceReference1);
    calendar = calendarServiceSelector.getService();
    assertThat(calendar, nullValue());
  }

  @Test
  public void testGetAllServices() {
    ServiceSelector<Calendar> calendarServiceSelector = spy(new ServiceSelector());
    when(calendarServiceSelector.getBundleContext()).thenReturn(mockBundleContext);

    calendarServiceSelector.bindService(mockServiceReference1);
    verify(calendarServiceSelector, times(1)).getBundleContext();
    calendarServiceSelector.bindService(mockServiceReference2);
    SortedSet<ServiceReference<Calendar>> calendarSet = calendarServiceSelector.getAllServices();

    assertThat(calendarSet, notNullValue());
    assertThat(calendarSet.size(), is(2));
  }

  @Test
  public void testExplicitConstructor() {
    Comparator mockComparator = buildMockComparator();
    ServiceSelectionStrategy mockServiceSelectionStrategy = buildMockServiceSelectionStrategy();

    ServiceSelector<Calendar> calendarServiceSelector =
        spy(new ServiceSelector(mockComparator, mockServiceSelectionStrategy));
    when(calendarServiceSelector.getBundleContext()).thenReturn(mockBundleContext);

    calendarServiceSelector.bindService(mockServiceReference1);
    calendarServiceSelector.bindService(mockServiceReference2);
    verify(mockComparator, atLeastOnce())
        .compare(any(ServiceReference.class), any(ServiceReference.class));
    verify(mockServiceSelectionStrategy, times(2)).selectService(any(SortedSet.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullComparator() {
    new ServiceSelector<Calendar>((Comparator) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testComparatorNullServiceSelectionStrategy() {
    new ServiceSelector<Calendar>((ServiceSelectionStrategy) null);
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

  private ServiceSelectionStrategy buildMockServiceSelectionStrategy() {
    ServiceSelectionStrategy serviceSelectionStrategy = mock(ServiceSelectionStrategy.class);
    return serviceSelectionStrategy;
  }

  private Comparator buildMockComparator() {
    // this is intentionally the reverse order of (Mock)ServiceReference.compare()
    Comparator comparator = mock(Comparator.class);

    when(comparator.compare(mockServiceReference1, mockServiceReference2)).thenReturn(1);
    when(comparator.compare(mockServiceReference2, mockServiceReference1)).thenReturn(-1);

    return comparator;
  }
}
