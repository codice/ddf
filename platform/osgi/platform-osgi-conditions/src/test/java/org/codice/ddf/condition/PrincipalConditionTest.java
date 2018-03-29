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
package org.codice.ddf.condition;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.HashSet;
import javax.security.auth.Subject;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.ConditionInfo;

public class PrincipalConditionTest {

  @Test
  public void testIsSatisfiedNoSubject() {
    Bundle bundle = mock(Bundle.class);
    PrincipalCondition principalCondition =
        new PrincipalCondition(
            bundle,
            new ConditionInfo(
                PrincipalCondition.class.getName(), new String[] {"role1", "role2", "role3"}));
    boolean satisfied = principalCondition.isSatisfied();
    assertThat(satisfied, is(false));
  }

  @Test
  public void testIsNotSatisfiedPrincipalMissing() {
    Bundle bundle = mock(Bundle.class);
    HashSet emptySet = new HashSet();
    HashSet<Principal> principalSet = new HashSet<>();
    principalSet.add(() -> "role2");
    Subject javaSubject = new javax.security.auth.Subject(true, principalSet, emptySet, emptySet);
    PrincipalCondition principalCondition =
        new PrincipalCondition(
            bundle,
            new ConditionInfo(
                PrincipalCondition.class.getName(), new String[] {"role1", "role2", "role3"}));

    boolean satisfied =
        Subject.doAs(javaSubject, (PrivilegedAction<Boolean>) principalCondition::isSatisfied);
    assertThat(satisfied, is(false));
  }

  @Test
  public void testIsSatisfiedAllPrincipals() {
    Bundle bundle = mock(Bundle.class);
    HashSet emptySet = new HashSet();
    HashSet<Principal> principalSet = new HashSet<>();
    principalSet.add(() -> "role1");
    principalSet.add(() -> "role2");
    principalSet.add(() -> "role3");
    Subject javaSubject = new javax.security.auth.Subject(true, principalSet, emptySet, emptySet);
    PrincipalCondition principalCondition =
        new PrincipalCondition(
            bundle,
            new ConditionInfo(
                PrincipalCondition.class.getName(), new String[] {"role1", "role2", "role3"}));

    boolean satisfied =
        Subject.doAs(javaSubject, (PrivilegedAction<Boolean>) principalCondition::isSatisfied);
    assertThat(satisfied, is(true));
  }

  @Test
  public void testIsSatisfiedExtraPrincipals() {
    Bundle bundle = mock(Bundle.class);
    HashSet emptySet = new HashSet();
    HashSet<Principal> principalSet = new HashSet<>();
    principalSet.add(() -> "role1");
    principalSet.add(() -> "role2");
    principalSet.add(() -> "role3");
    principalSet.add(() -> "role4");
    principalSet.add(() -> "role5");
    principalSet.add(() -> "role6");
    Subject javaSubject = new javax.security.auth.Subject(true, principalSet, emptySet, emptySet);
    PrincipalCondition principalCondition =
        new PrincipalCondition(
            bundle,
            new ConditionInfo(
                PrincipalCondition.class.getName(), new String[] {"role1", "role2", "role3"}));

    boolean satisfied =
        Subject.doAs(javaSubject, (PrivilegedAction<Boolean>) principalCondition::isSatisfied);
    assertThat(satisfied, is(true));
  }
}
