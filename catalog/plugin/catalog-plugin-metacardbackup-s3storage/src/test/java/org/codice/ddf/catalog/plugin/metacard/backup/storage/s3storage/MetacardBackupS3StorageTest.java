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
package org.codice.ddf.catalog.plugin.metacard.backup.storage.s3storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.junit.Test;

public class MetacardBackupS3StorageTest {

    private static final String OBJECT_TEMPLATE = "test-dir/test-output/";

    private static final String BUCKET = "s3-bucket";

    private static final String ENDPOINT = "test.amazonaws.com";

    private static final String ACCESS_KEY = "access_key";

    private static final String SECRET_KEY = "secret_key";

    private static final String CANNED_ACL = "Private";

    private CamelContext camelContext = new DefaultCamelContext();

    private MetacardS3StorageRoute s3StorageProvider = new MetacardS3StorageRoute(camelContext);

    @Before
    public void setUp() throws Exception {
        s3StorageProvider.setObjectTemplate(OBJECT_TEMPLATE);
        s3StorageProvider.setS3Bucket(BUCKET);
        s3StorageProvider.setS3Endpoint(ENDPOINT);
        s3StorageProvider.setS3AccessKey(ACCESS_KEY);
        s3StorageProvider.setS3SecretKey(SECRET_KEY);
        s3StorageProvider.setS3CannedAclName(CANNED_ACL);
    }

    @Test
    public void testOutputPathTemplate() {
        assertThat(s3StorageProvider.getObjectTemplate(), is(OBJECT_TEMPLATE));
    }

    @Test
    public void testRefresh() throws Exception {
        String newObjectTemplate = "target" + File.separator + "temp";
        boolean backupInvalidCards = false;
        boolean keepDeletedMetacards = false;
        String metacardTransformerId = "testTransformer";
        String accessKey = "newAccessKey";
        String secretKey = "newSecretKey";
        String bucket = "new-bucket";
        String endpoint = "endpoint.amazonaws.com";
        String cannedAcl = "PublicRead";

        Map<String, Object> properties = new HashMap<>();
        properties.put("objectTemplate", newObjectTemplate);
        properties.put("backupInvalidMetacards", backupInvalidCards);
        properties.put("keepDeletedMetacards", keepDeletedMetacards);
        properties.put("metacardTransformerId", metacardTransformerId);
        properties.put("s3AccessKey", accessKey);
        properties.put("s3SecretKey", secretKey);
        properties.put("s3Bucket", bucket);
        properties.put("s3Endpoint", endpoint);
        properties.put("s3CannedAclName", cannedAcl);

        s3StorageProvider.refresh(properties);
        assertThat(s3StorageProvider.getObjectTemplate(), is(newObjectTemplate));
        assertThat(s3StorageProvider.isBackupInvalidMetacards(), is(backupInvalidCards));
        assertThat(s3StorageProvider.isKeepDeletedMetacards(), is(keepDeletedMetacards));
        assertThat(s3StorageProvider.getMetacardTransformerId(), is(metacardTransformerId));
        assertThat(s3StorageProvider.getS3AccessKey(), is(accessKey));
        assertThat(s3StorageProvider.getS3SecretKey(), is(secretKey));
        assertThat(s3StorageProvider.getS3Bucket(), is(bucket));
        assertThat(s3StorageProvider.getS3Endpoint(), is(endpoint));
        assertThat(s3StorageProvider.getS3CannedAclName(), is(cannedAcl));
    }

    @Test
    public void testRefreshBadValues() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("objectTemplate", 2);
        properties.put("s3AccessKey", 3.0);
        properties.put("s3SecretKey", 2.1);
        properties.put("s3Bucket", 5);
        properties.put("s3Endpoint", 4);
        properties.put("s3CannedAclName", 7);

        s3StorageProvider.refresh(properties);
        assertThat(s3StorageProvider.getObjectTemplate(), is(OBJECT_TEMPLATE));
        assertThat(s3StorageProvider.getS3AccessKey(), is(ACCESS_KEY));
        assertThat(s3StorageProvider.getS3SecretKey(), is(SECRET_KEY));
        assertThat(s3StorageProvider.getS3Bucket(), is(BUCKET));
        assertThat(s3StorageProvider.getS3Endpoint(), is(ENDPOINT));
        assertThat(s3StorageProvider.getS3CannedAclName(), is(CANNED_ACL));
    }

    @Test
    public void testEmptyAccessKeys() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("s3AccessKey", "");
        properties.put("s3SecretKey", "");
        s3StorageProvider.refresh(properties);
        assertThat(s3StorageProvider.getS3AccessKey(), isEmptyOrNullString());
        assertThat(s3StorageProvider.getS3SecretKey(), isEmptyOrNullString());
    }

    @Test
    public void testRemovePrecedingSlash() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("objectTemplate", "/slash/to/remove");
        s3StorageProvider.refresh(properties);
        assertThat(s3StorageProvider.getObjectTemplate(), is("slash/to/remove"));
    }
}
