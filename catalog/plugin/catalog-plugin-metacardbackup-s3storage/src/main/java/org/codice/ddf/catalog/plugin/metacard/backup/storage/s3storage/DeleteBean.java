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

import com.amazonaws.services.s3.AmazonS3;
import org.apache.camel.Header;
import org.apache.camel.component.aws.s3.S3Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteBean {
  private static final Logger LOGGER = LoggerFactory.getLogger(DeleteBean.class);
  private String s3Bucket;
  private AmazonS3 s3Client;

  public DeleteBean(AmazonS3 s3Client, String s3Bucket) {
    this.s3Client = s3Client;
    this.s3Bucket = s3Bucket;
  }

  public void delete(@Header(S3Constants.KEY) String s3Key) {
    LOGGER.trace("Deleting: {} / {}", s3Bucket, s3Key);
    s3Client.deleteObject(s3Bucket, s3Key);
  }
}
