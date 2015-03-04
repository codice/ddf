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
package ddf.security.sts;

import ddf.security.common.audit.SecurityLogger;
import org.apache.cxf.sts.event.map.KEYS;
import org.apache.cxf.sts.event.map.MapEvent;
import org.apache.cxf.sts.event.map.MapEventListener;

import java.util.Map;

/**
 * STSEventListener that logs events send by the STS during operations and logs it to the DDF
 * SecurityLogger.
 */
public class STSEventListener implements MapEventListener {

    private static final String FAILURE_STATUS = "FAILURE";

    @Override
    public void onEvent(MapEvent event) {

        Map<String, ?> eventProps = event.getProperties();

        StringBuilder builder = new StringBuilder();
        builder.append("Security Token Service REQUEST\n");
        appendNotNull(eventProps, builder, KEYS.STATUS);
        appendNotNull(eventProps, builder, KEYS.OPERATION);
        appendNotNull(eventProps, builder, KEYS.URL);
        appendNotNull(eventProps, builder, KEYS.WS_SEC_PRINCIPAL);
        appendNotNull(eventProps, builder, KEYS.ONBEHALFOF_PRINCIPAL);
        appendNotNull(eventProps, builder, KEYS.ACTAS_PRINCIPAL);
        appendNotNull(eventProps, builder, KEYS.VALIDATE_PRINCIPAL);
        appendNotNull(eventProps, builder, KEYS.CANCEL_PRINCIPAL);
        appendNotNull(eventProps, builder, KEYS.RENEW_PRINCIPAL);
        appendNotNull(eventProps, builder, KEYS.TOKENTYPE);
        appendNotNull(eventProps, builder, KEYS.APPLIESTO);
        appendNotNull(eventProps, builder, KEYS.CLAIMS_PRIMARY);
        appendNotNull(eventProps, builder, KEYS.CLAIMS_SECONDARY);

        // check type of event
        Object status = eventProps.get(KEYS.STATUS.toString());

        if (status != null && FAILURE_STATUS.equals(status.toString())) {
            appendNotNull(eventProps, builder, KEYS.EXCEPTION);
            // on failure send as warn
            SecurityLogger.logWarn(builder.toString());
        } else {
            // otherwise throw as info
            SecurityLogger.logInfo(builder.toString());
        }

    }

    private String safeConvert(Object obj) {
        if (obj != null) {
            return obj.toString();
        } else {
            return "<null>";
        }
    }

    private void appendNotNull(Map<String, ?> eventProps, StringBuilder builder, KEYS key) {
        String keyStr = key.toString();
        if (eventProps.containsKey(keyStr)) {
            builder.append(key);
            builder.append(": ");
            builder.append(safeConvert(eventProps.get(keyStr)));
            builder.append("\n");
        }
    }

}
