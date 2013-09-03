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
package ddf.security.sts;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

import ddf.security.sts.StsIssueTest.StsPortTypes;

public final class StsTestRunner {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        StsIssueTest sit = new StsIssueTest();

        // Test the Transport Port
        try {
            sit.testBearerUsernameTokenSaml2(StsPortTypes.TRANSPORT);
            sit.testBearerWebSsoTokenSaml2(StsPortTypes.TRANSPORT);
        } catch (Exception e) {
            Logger logger = LogUtils.getLogger(StsTestRunner.class);
            logger.log(Level.SEVERE, "An Error Occurred Calling the STS", e);
        }

    }

    private StsTestRunner() {
    };

}
