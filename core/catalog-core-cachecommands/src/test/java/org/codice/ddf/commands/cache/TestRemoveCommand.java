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

package org.codice.ddf.commands.cache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import ddf.catalog.cache.solr.impl.SolrCacheMBean;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class TestRemoveCommand {

    List<Metacard> metacardList = getMetacardList(5);

    @Test
    public void testdoExecute() throws Exception {
        final SolrCacheMBean mbean = mock(SolrCacheMBean.class);

        String[] ids = {metacardList.get(0).getId()};

        RemoveCommand removeCommand = new RemoveCommand() {
            @Override
            protected SolrCacheMBean getCacheProxy() {
                return mbean;
            }
        };

        removeCommand.ids = ids;

        removeCommand.doExecute();

        verify(mbean, times(1)).removeById(ids);
    }

    private java.util.List<Metacard> getMetacardList(int amount) {

        List<Metacard> metacards = new ArrayList<Metacard>();

        for (int i = 0; i < amount; i++) {

            String id = UUID.randomUUID().toString();
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(id);

            metacards.add(metacard);

        }

        return metacards;
    }

    private void removeMetacard(String[] ids) {

    }

}
