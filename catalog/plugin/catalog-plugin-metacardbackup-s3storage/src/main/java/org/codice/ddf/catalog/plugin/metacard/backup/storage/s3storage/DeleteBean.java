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
package org.codice.ddf.catalog.plugin.metacard.backup.storage.s3storage;

import java.util.ArrayList;
import org.apache.camel.Header;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

public class DeleteBean {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBean.class);
  private String s3Bucket;
  private S3Client s3Client;

  public DeleteBean(S3Client s3Client, String s3Bucket) {
    this.s3Client = s3Client;
    this.s3Bucket = s3Bucket;
  }

  public void delete(@Header(AWS2S3Constants.KEY) String s3Key) {
    LOGGER.trace("Deleting: {} / {}", s3Bucket, s3Key);

    ArrayList<ObjectIdentifier> toDelete = new ArrayList<>();
    toDelete.add(ObjectIdentifier.builder().key(s3Key).build());

    DeleteObjectsRequest deleteRequest =
        DeleteObjectsRequest.builder()
            .bucket(s3Bucket)
            .delete(Delete.builder().objects(toDelete).build())
            .build();
    s3Client.deleteObjects(deleteRequest);
  }
}
