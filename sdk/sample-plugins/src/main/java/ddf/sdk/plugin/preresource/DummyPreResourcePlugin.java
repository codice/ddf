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
package ddf.sdk.plugin.preresource;



import java.io.Serializable;
import java.util.Map;

import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.sdk.plugin.prequery.DummyPreQueryPlugin;



/***************************************************************************************
* Follow DDF Developer's Guide to implement Life-cycle Services, Sources, or Transformers
* This template/example shows the skeleton code for a Pre-Resource Plugin
****************************************************************************************/

public class DummyPreResourcePlugin implements PreResourcePlugin 
{
    @Override
    public ResourceRequest process(ResourceRequest input) throws PluginExecutionException, StopProcessingException
    {
        ResourceRequest newResourceRequest = input;
        
        if (newResourceRequest != null)
        {
           	Map<String,Serializable> requestProperties = newResourceRequest.getProperties();
           	requestProperties.put(ResourceRequest.IS_ENTERPRISE, true);
        }
		return newResourceRequest;
    }
}