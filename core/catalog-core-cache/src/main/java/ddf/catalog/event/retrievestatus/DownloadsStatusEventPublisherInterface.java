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
package ddf.catalog.event.retrievestatus;


import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;

public interface DownloadsStatusEventPublisherInterface {

    public static enum ProductRetrievalStatus {
        STARTED, IN_PROGRESS, RETRYING, CANCELLED, FAILED, COMPLETE;
    }

    public void postRetrievalStatus(final ResourceResponse resourceResponse, ProductRetrievalStatus status, Metacard metacard, String detail, Long bytes, String downloadIdentifier);

    public void setNotificationEnabled(boolean enabled);

    public void setActivityEnabled(boolean enabled);


}
