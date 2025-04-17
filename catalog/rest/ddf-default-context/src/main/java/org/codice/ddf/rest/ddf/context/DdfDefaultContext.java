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
package org.codice.ddf.rest.ddf.context;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContext;

//
// <bean id="ddfDefaultContextHelper" class="org.codice.ddf.rest.ddf.context.DdfDefaultContext"
// scope="bundle">
// <argument ref="blueprintBundleContext"/>
// </bean>
//
// <service interface="org.osgi.service.http.context.ServletContextHelper"
// ref="ddfDefaultContextHelper">
// <service-properties>
// <entry key="osgi.http.whiteboard.context.path" value="/"/>
// <entry key="osgi.http.whiteboard.context.name" value="ddfDefaultContext"/>
// <entry key="service.ranking" value="2147483647" /> <!-- Integer.MAX_VALUE -->
// </service-properties>
//
// </service>

@Component(service = ServletContextHelper.class, scope = ServiceScope.BUNDLE)
@HttpWhiteboardContext(name = "ddfDefaultContext", path = "/")
public class DdfDefaultContext extends ServletContextHelper {}
