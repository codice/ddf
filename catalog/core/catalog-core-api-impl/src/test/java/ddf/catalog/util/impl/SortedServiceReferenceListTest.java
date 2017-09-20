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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

import java.util.Random;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortedServiceReferenceListTest {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(SortedServiceReferenceListTest.class);

  @Test
  public void testAscending() {

    SortedServiceReferenceList refList = new SortedServiceReferenceList();

    int loopAmount = 100;

    for (int i = 0; i < loopAmount; i++) {
      refList.bindService(new ServiceReferenceImpl(i));
    }

    assertThat(refList.size(), is(loopAmount));

    assertInOrder(refList, loopAmount);
  }

  @Test
  public void testDescending() {

    SortedServiceReferenceList refList = new SortedServiceReferenceList();

    int loopAmount = 100;

    for (int i = loopAmount; i > 0; i--) {
      refList.bindService(new ServiceReferenceImpl(i));
    }

    assertThat(refList.size(), is(loopAmount));

    assertInOrder(refList, loopAmount);
  }

  @Test
  public void testRandomOrder() {

    SortedServiceReferenceList refList = new SortedServiceReferenceList();

    int loopAmount = 100;

    Random random = new Random(System.currentTimeMillis());

    for (int i = 0; i < loopAmount; i++) {
      refList.bindService(new ServiceReferenceImpl(random.nextInt()));
    }

    assertThat(refList.size(), is(loopAmount));

    assertInOrder(refList, Integer.MAX_VALUE);
  }

  protected void assertInOrder(SortedServiceReferenceList refList, int startingLowestRank) {
    int lowestRanking = startingLowestRank;

    for (ServiceReference s : refList) {

      Integer ranking = (Integer) s.getProperty(Constants.SERVICE_RANKING);
      LOGGER.debug("service is ranked [{}], lowest current ranking [{}]", ranking, lowestRanking);
      assertThat(ranking, lessThanOrEqualTo(lowestRanking));

      lowestRanking = ranking;
    }
  }
}
