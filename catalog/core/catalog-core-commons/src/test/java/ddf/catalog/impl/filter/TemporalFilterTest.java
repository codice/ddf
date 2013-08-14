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
package ddf.catalog.impl.filter;

import static org.junit.Assert.*;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

public class TemporalFilterTest {

	@Test
	public void testParseDate() {
		Date expected = new DateTime(1996, 12, 19, 16, 39, 57, 0, DateTimeZone.forID("-08:00")).toDate();
		Date actual = TemporalFilter.parseDate("1996-12-19T16:39:57-08:00");
		assertEquals(expected, actual);

		expected = new DateTime(1985, 4, 12, 23, 20, 50, 520, DateTimeZone.UTC).toDate();
		actual = TemporalFilter.parseDate("1985-04-12T23:20:50.52Z");
		assertEquals(expected, actual);

		expected = new DateTime(1998, 7, 16, 19, 20, 30, 0, DateTimeZone.forID("-01:00")).toDate();
		actual = TemporalFilter.parseDate("1998-07-16T19:20:30-01:00");
		assertEquals(expected, actual);

		/* Basic profiles */
		expected = new DateTime(2007, 4, 5, 12, 30, 0, 0, DateTimeZone.forID("+02:00")).toDate();
		actual = TemporalFilter.parseDate("20070405T123000+0200");
		assertEquals(expected, actual);

		expected = new DateTime(2011, 10, 4, 5, 48, 27, 891, DateTimeZone.forID("-07:00")).toDate();
		actual = TemporalFilter.parseDate("20111004T054827.891-0700");
		assertEquals(expected, actual);
	}

	@Test
	public void testParseInvalidDates() {
		// Space instead of "T" to separate date/time
		Date date = TemporalFilter.parseDate("1996-12-19 16:39:57-08:00");
		assertNull(date);

		// Combines extended and basic ISO 8601 formats
		date = TemporalFilter.parseDate("1985-04-12T232050.52Z");
		assertNull(date);

		// No time zone specified
		date = TemporalFilter.parseDate("1998-07-16T19:20:30");
		assertNull(date);
	}

}
