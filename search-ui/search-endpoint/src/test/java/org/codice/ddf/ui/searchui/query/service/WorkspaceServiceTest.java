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
package org.codice.ddf.ui.searchui.query.service;

import java.io.StringReader;
import java.util.Map;

import org.cometd.common.JSONContext;
import org.cometd.common.JacksonJSONContextClient;
import org.junit.Test;

public class WorkspaceServiceTest {

    @Test
    public void test() throws Exception {
//        JSONContext.Client jsonContext = (JSONContext.Client)getClass().getClassLoader().loadClass(JacksonJSONContextClient.class).newInstance();
//        Data data1 = new Data("data");
//        Extra extra1 = new Extra(42L);
//        Map<String, Object> map1 = new HashMap<>();
//        map1.put("data", data1);
//        map1.put("extra", extra1);
//        String json = jsonContext.getGenerator().generate(map1);
        JSONContext.Client jsonContext = new JacksonJSONContextClient();
        String json = "{\"id\":\"16\",\"data\":{\"workspaces\":[{\"name\":\"admin_ws\",\"searches\":[{\"radiusUnits\":\"meters\",\"result\":{},\"startIndex\":1,\"count\":250,\"federation\":\"enterprise\",\"q\":\"admin_phrase\",\"name\":\"admin_search\",\"radiusValue\":0,\"radius\":0,\"offsetTimeUnits\":\"hours\",\"src\":\"ddf.distribution\",\"format\":\"geojson\",\"timeType\":\"modified\"}],\"metacards\":[]}],\"successful\":true},\"channel\":\"/service/workspaces\"}";
        Map map2 = jsonContext.getParser().parse(new StringReader(json), Map.class);
        int x = 1;
//        Data data2 = (Data)map2.get("data");
//        Extra extra2 = (Extra)map2.get("extra");
//        Assert.assertEquals(data1.content, data2.content);
//        Assert.assertEquals(extra1.content, extra2.content);
    }

}
