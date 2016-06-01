/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.pdp.realm.xacml;

public class XACMLConstants {

    public static final String ROLE_CLAIM =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    public static final String STRING_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#string";

    public static final String BOOLEAN_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#boolean";

    public static final String INTEGER_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#integer";

    public static final String DOUBLE_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#double";

    public static final String TIME_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#time";

    public static final String DATE_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#date";

    public static final String DATE_TIME_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#dateTime";

    public static final String URI_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#anyURI";

    public static final String RFC822_NAME_DATA_TYPE =
            "urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name";

    public static final String IP_ADDRESS_DATA_TYPE =
            "urn:oasis:names:tc:xacml:2.0:data-type:ipAddress";

    public static final String X500_NAME_DATA_TYPE =
            "urn:oasis:names:tc:xacml:1.0:data-type:x500Name";

    public static final String ACTION_CATEGORY =
            "urn:oasis:names:tc:xacml:3.0:attribute-category:action";

    public static final String RESOURCE_CATEGORY =
            "urn:oasis:names:tc:xacml:3.0:attribute-category:resource";

    public static final String ENVIRONMENT_CATEGORY =
            "urn:oasis:names:tc:xacml:3.0:attribute-category:environment";

    public static final String ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id";

    public static final String ACCESS_SUBJECT_CATEGORY =
            "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";

    public static final String RECIPIENT_SUBJECT_CATEGORY =
            "urn:oasis:names:tc:xacml:1.0:subject-category:recipient-subject";

    public static final String INTERMEDIARY_SUBJECT_CATEGORY =
            "urn:oasis:names:tc:xacml:1.0:subject-category:intermediary-subject";

    public static final String MACHINE_SUBJECT_CATEGORY =
            "urn:oasis:names:tc:xacml:1.0:subject-category:requesting-machine";

    public static final String CODEBASE_SUBJECT_CATEGORY =
            "urn:oasis:names:tc:xacml:1.0:subject-category:codebase";

    public static final String SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

    public static final String SUBJECT_NAME_FORMAT =
            "urn:oasis:names:tc:xacml:1.0:subject:name-format";

    public static final String RESOURCE_ID = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    public static final String RESOURCE_NAMESPACE =
            "urn:oasis:names:tc:xacml:2.0:resource:target-namespace";

    public static final String FILTER_ACTION = "filter";

    private XACMLConstants() {

    }

}
