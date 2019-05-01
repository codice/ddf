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

import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.builder.PredicateBuilder.not;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import ddf.camel.component.catalog.ingest.PostIngestConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.CamelContext;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.impl.CompositeRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.plugin.metacard.backup.common.MetacardStorageRoute;
import org.codice.ddf.catalog.plugin.metacard.backup.common.MetacardTemplate;
import org.codice.ddf.catalog.plugin.metacard.backup.common.ResponseMetacardActionSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a camel route for storing metacards from post-ingest in Amazon S3. This route will
 * transform the metacard using the configured metacard transformer prior to storage.
 */
public class MetacardS3StorageRoute extends MetacardStorageRoute {
  public static final String OBJECT_TEMPLATE = "objectTemplate";

  public static final String S3_ACCESS_KEY_PROP = "s3AccessKey";

  public static final String S3_SECRET_KEY_PROP = "s3SecretKey";

  public static final String S3_ENDPOINT_PROP = "s3Endpoint";

  public static final String S3_REGION_PROP = "s3Region";

  public static final String S3_BUCKET_PROP = "s3Bucket";

  public static final String S3_CANNED_ACL_NAME_PROP = "s3CannedAclName";

  public static final String AWS_S3_CLIENT_PROP = "amazonS3Client";

  public static final String AWS_S3_DELETE_AFTER_WRITE_PROP = "deleteAfterWrite";

  protected String objectTemplate;

  protected String s3Bucket;

  protected String s3AccessKey = "";

  protected String s3SecretKey = "";

  protected String s3Endpoint;

  protected String s3Region;

  protected String s3CannedAclName;

  private List<String> routeIds = new ArrayList<>();

  private final org.apache.camel.impl.SimpleRegistry registry;

  private MetacardTemplate metacardTemplate = null;

  private AmazonS3 s3Client = null;

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardS3StorageRoute.class);

  public MetacardS3StorageRoute(CamelContext camelContext) {
    super(camelContext);
    registry = new org.apache.camel.impl.SimpleRegistry();
    CompositeRegistry compositeRegistry = new CompositeRegistry();
    compositeRegistry.addRegistry(camelContext.getRegistry());
    compositeRegistry.addRegistry(registry);
    ((DefaultCamelContext) camelContext).setRegistry(compositeRegistry);
  }

  public String getObjectTemplate() {
    return objectTemplate;
  }

  public void setObjectTemplate(String objectTemplate) {
    this.objectTemplate = StringUtils.removeStart(objectTemplate, "/");
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public void setS3Bucket(String s3Bucket) {
    this.s3Bucket = s3Bucket;
  }

  public String getS3AccessKey() {
    return s3AccessKey;
  }

  public void setS3AccessKey(String s3AccessKey) {
    this.s3AccessKey = s3AccessKey;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public void setS3SecretKey(String s3SecretKey) {
    this.s3SecretKey = s3SecretKey;
  }

  public String getS3Endpoint() {
    return s3Endpoint;
  }

  public void setS3Endpoint(String s3Endpoint) {
    this.s3Endpoint = s3Endpoint;
  }

  public String getS3Region() {
    return s3Region;
  }

  public void setS3Region(String s3Region) {
    this.s3Region = s3Region;
  }

  public String getS3CannedAclName() {
    return s3CannedAclName;
  }

  public void setS3CannedAclName(String s3CannedAclName) {
    this.s3CannedAclName = s3CannedAclName;
  }

  @Override
  public void configure() throws Exception {
    routeIds.clear();

    StringBuilder options = new StringBuilder();
    metacardTemplate = new MetacardTemplate(objectTemplate);
    s3Client = getS3Client();

    DeleteBean deleteBean = new DeleteBean(s3Client, s3Bucket);

    registry.put("s3Client", s3Client);
    registry.put("deleteBean", deleteBean);
    addOption(options, AWS_S3_CLIENT_PROP, "#s3Client");

    addOption(options, AWS_S3_DELETE_AFTER_WRITE_PROP, "false");

    String s3Uri = "aws-s3://" + getS3Bucket();
    if (options.length() > 0) {
      s3Uri += "?" + options.toString();
    }

    LOGGER.trace("S3 storage URI: {}", s3Uri);

    String metacardRouteId = "metacard-" + UUID.randomUUID().toString();
    String route1Id = metacardRouteId + "1";

    from("catalog:postingest")
        .routeId(route1Id)
        .autoStartup(true)
        .split(method(ResponseMetacardActionSplitter.class, "split(${body})"))
        .to("direct:" + metacardRouteId);
    routeIds.add(route1Id);

    String route2Id = metacardRouteId + "2";
    from("direct:" + metacardRouteId + "?block=true")
        .routeId(route2Id)
        .setHeader(METACARD_TRANSFORMER_ID_RTE_PROP, simple(metacardTransformerId, String.class))
        .setHeader(
            METACARD_BACKUP_INVALID_RTE_PROP,
            simple(String.valueOf(backupInvalidMetacards), Boolean.class))
        .setHeader(
            METACARD_BACKUP_KEEP_DELETED_RTE_PROP,
            simple(String.valueOf(keepDeletedMetacards), Boolean.class))
        .setHeader(S3Constants.KEY, method(metacardTemplate, "applyTemplate(${body})"))
        .choice()
        .when(
            and(
                header(PostIngestConsumer.ACTION).isEqualTo(PostIngestConsumer.DELETE),
                getCheckDeletePredicate()))
        .bean(deleteBean, "delete")
        .stop()
        .otherwise()
        .choice()
        .when(not(getShouldBackupPredicate()))
        .stop()
        .otherwise()
        .to("catalog:metacardtransformer")
        .setHeader(S3Constants.CANNED_ACL, simple(s3CannedAclName))
        .setHeader(S3Constants.CONTENT_LENGTH, simple("${body.length}"))
        .to(s3Uri);
    routeIds.add(route2Id);

    LOGGER.trace("Starting metacard S3 storage route: {}", this);
  }

  @Override
  public void refresh(Map<String, Object> properties) throws Exception {
    Object outputPathTemplateProp = properties.get(OBJECT_TEMPLATE);
    if (outputPathTemplateProp instanceof String) {
      setObjectTemplate((String) outputPathTemplateProp);
    }

    Object s3AccessKeyValue = properties.get(S3_ACCESS_KEY_PROP);
    if (s3AccessKeyValue instanceof String) {
      String accessKey = (String) s3AccessKeyValue;
      if (StringUtils.isNotBlank(accessKey)) {
        setS3AccessKey((String) s3AccessKeyValue);
      } else {
        setS3AccessKey(null);
      }
    }

    Object s3SecreKeyValue = properties.get(S3_SECRET_KEY_PROP);
    if (s3SecreKeyValue instanceof String) {
      String secretKey = (String) s3SecreKeyValue;
      if (StringUtils.isNotBlank(secretKey)) {
        setS3SecretKey((String) s3SecreKeyValue);
      } else {
        setS3SecretKey(null);
      }
    }

    Object s3EndpointValue = properties.get(S3_ENDPOINT_PROP);
    if (s3EndpointValue instanceof String) {
      setS3Endpoint((String) s3EndpointValue);
    }

    Object s3RegionValue = properties.get(S3_REGION_PROP);
    if (s3RegionValue instanceof String) {
      setS3Region((String) s3RegionValue);
    }

    Object s3BucketValue = properties.get(S3_BUCKET_PROP);
    if (s3BucketValue instanceof String) {
      setS3Bucket((String) s3BucketValue);
    }

    Object s3CannedAclValue = properties.get(S3_CANNED_ACL_NAME_PROP);
    if (s3CannedAclValue instanceof String) {
      setS3CannedAclName((String) s3CannedAclValue);
    }

    super.refresh(properties);
  }

  @Override
  public List<String> getRouteIds() {
    return routeIds;
  }

  private void addOption(StringBuilder options, String key, String value) {
    if (options.length() > 0) {
      options.append("&");
    }
    options.append(key).append("=").append(value);
  }

  private AmazonS3 getS3Client() {
    AwsClientBuilder.EndpointConfiguration endpointConfiguration =
        new AwsClientBuilder.EndpointConfiguration(s3Endpoint, getS3Region());
    if (StringUtils.isNotBlank(s3AccessKey)) {
      AWSCredentials awsCredentials = new BasicAWSCredentials(s3AccessKey, s3SecretKey);
      AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
      return AmazonS3ClientBuilder.standard()
          .withCredentials(credentialsProvider)
          .withEndpointConfiguration(endpointConfiguration)
          .build();
    }
    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(endpointConfiguration)
        .build();
  }
}
