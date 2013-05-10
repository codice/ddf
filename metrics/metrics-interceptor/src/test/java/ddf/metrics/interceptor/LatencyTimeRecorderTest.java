/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.metrics.interceptor;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author willisod
 *
 */
public class LatencyTimeRecorderTest {

    /**
     * Test method for {@link ddf.metrics.interceptor.LatencyTimeRecorder#getLatencyTime()}.
     * @throws InterruptedException 
     */
    @Test
    public void testGetLatencyTime() throws InterruptedException {
        
        // Setup
        LatencyTimeRecorder ltr = new LatencyTimeRecorder();
        
        long startTime = System.currentTimeMillis();
        
        // Perform test
        ltr.beginHandling();
        Thread.sleep(1);
        ltr.endHandling();
        
        // validate
        Thread.sleep(1);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        assertThat(ltr.getLatencyTime(), is(lessThan(totalTime)));
        assertThat(ltr.getLatencyTime(), is(greaterThanOrEqualTo(1L)));
    }

}
