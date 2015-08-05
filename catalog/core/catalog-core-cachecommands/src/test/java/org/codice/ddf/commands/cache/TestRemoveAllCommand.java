/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.commands.cache;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import ddf.catalog.cache.SolrCacheMBean;




public class TestRemoveAllCommand {

    @Test
    public void testDoExecute() throws Exception {

        final SolrCacheMBean mbean = mock(SolrCacheMBean.class);

        RemoveAllCommand removeAllCommand = new RemoveAllCommand() {
            @Override
            protected SolrCacheMBean getCacheProxy() {
                return mbean;
            }
        };

        removeAllCommand.force = true;
        removeAllCommand.doExecute();

        verify(mbean, times(1)).removeAll();

    }
}
