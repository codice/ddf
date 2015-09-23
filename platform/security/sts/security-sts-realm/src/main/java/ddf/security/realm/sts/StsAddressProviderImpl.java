/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.realm.sts;

import ddf.security.sts.client.configuration.STSClientConfiguration;
import ddf.security.sts.client.configuration.StsAddressProvider;

public class StsAddressProviderImpl implements StsAddressProvider {
    private boolean useWss = false;

    private final STSClientConfiguration internalSts;

    private final STSClientConfiguration wssSts;

    public StsAddressProviderImpl(STSClientConfiguration internalSts,
            STSClientConfiguration wssSts) {
        this.internalSts = internalSts;
        this.wssSts = wssSts;
    }

    public boolean isUseWss() {
        return useWss;
    }

    public void setUseWss(boolean useWss) {
        this.useWss = useWss;
    }

    @Override
    public String getStsAddress() {
        String currentStsAddress;
        if (useWss) {
            currentStsAddress = wssSts.getAddress();
        } else {
            currentStsAddress = internalSts.getAddress();
        }
        return currentStsAddress;
    }
}
