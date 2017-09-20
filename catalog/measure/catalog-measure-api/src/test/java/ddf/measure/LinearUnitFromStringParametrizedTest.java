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
package ddf.measure;

import static ddf.measure.Distance.LinearUnit;
import static ddf.measure.Distance.LinearUnit.FOOT_U_S;
import static ddf.measure.Distance.LinearUnit.KILOMETER;
import static ddf.measure.Distance.LinearUnit.METER;
import static ddf.measure.Distance.LinearUnit.MILE;
import static ddf.measure.Distance.LinearUnit.NAUTICAL_MILE;
import static ddf.measure.Distance.LinearUnit.YARD;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests the different string values supported by the {@link LinearUnit#fromString(String)} method.
 */
@RunWith(Parameterized.class)
public class LinearUnitFromStringParametrizedTest {

  @Parameters(name = "fromString({0})")
  public static Iterable<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"FOOT_U_S", FOOT_U_S},
          {"foot_u_s", FOOT_U_S},
          {"foot", FOOT_U_S},
          {"FOOT", FOOT_U_S},
          {"meter", METER},
          {"METER", METER},
          {"kilometer", KILOMETER},
          {"KILOMETER", KILOMETER},
          {"nautical_mile", NAUTICAL_MILE},
          {"NAUTICAL_MILE", NAUTICAL_MILE},
          {"nauticalMile", NAUTICAL_MILE},
          {"mile", MILE},
          {"MILE", MILE},
          {"yard", YARD},
          {"YARD", YARD}
        });
  }

  @Parameter public String enumValueString;

  @Parameter(1)
  public LinearUnit expectedEnumValue;

  @Test
  public void testLinearUnit() {
    assertThat(LinearUnit.fromString(enumValueString), equalTo(expectedEnumValue));
  }
}
