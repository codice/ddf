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
package org.codice.ddf.platform.bootflag;

/**
 * A {@code BootServiceFlag} is a marker interface that indicates some sort of condition is met
 * indicated by the service being available in the OSGi service registry. A service property should
 * be used to allow filtering.
 *
 * <p>The condition for the {@code BootServiceFlag} is only checked once at system startup time. If
 * the service is registered, unregistering it at a later time will have no effect. The service
 * should only be relied upon at system startup time.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface BootServiceFlag {}
