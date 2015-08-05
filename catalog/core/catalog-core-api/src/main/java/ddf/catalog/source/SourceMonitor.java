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
package ddf.catalog.source;


/**
 * The Interface {@link SourceMonitor} is used as a callback object to set the availability for the
 * {@link ddf.catalog.util.SourcePollerRunner}
 *
 */
public interface SourceMonitor {

    /**
     * Sets the availability of the monitored source to {@link SourceStatus.AVAILABLE}. This method
     * is used to identify to the caller that the {@link Source} is now available. A {@link Source}
     * would call this method when that {@link Source} becomes available.
     *
     * @see Source#isAvailable(SourceMonitor)
     * @see ddf.catalog.util.SourcePollerRunner
     */
    public void setAvailable();

    /**
     * Sets the availability of the monitored source to {@link SourceStatus.UNAVAILABLE}. This
     * method is used to identify to the caller that the {@link Source} is now unavailable. A
     * {@link Source} would call this method when that {@link Source} has become unavailable.
     *
     * @see Source#isAvailable(SourceMonitor)
     * @see ddf.catalog.util.SourcePollerRunner
     */
    public void setUnavailable();
}
