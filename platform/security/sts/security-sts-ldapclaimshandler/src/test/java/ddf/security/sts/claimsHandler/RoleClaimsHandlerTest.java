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
package ddf.security.sts.claimsHandler;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class RoleClaimsHandlerTest {
    @Test
    public void testRetrieveClaimsValuesNullPrincipal() {
        RoleClaimsHandler claimsHandler = new RoleClaimsHandler();
        ClaimsParameters claimsParameters = new ClaimsParameters();
        ClaimCollection claimCollection = new ClaimCollection();
        ProcessedClaimCollection processedClaims = claimsHandler.retrieveClaimValues(
                claimCollection, claimsParameters);

        Assert.assertThat(processedClaims.size(), CoreMatchers.is(equalTo(0)));
    }
}