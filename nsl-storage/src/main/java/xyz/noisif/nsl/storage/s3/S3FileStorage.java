/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
 *
 * NOTICE: This source code is publicly available for reference
 * and educational purposes only. It is NOT open-source software.
 *
 * You are granted permission to view this code. However, you are strictly
 * PROHIBITED from copying, modifying, or merging this code into other software,
 * distributing, publishing, or sublicensing this code, using this code for
 * commercial purposes or in production environments.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Please refer to the LICENSE file in the root directory for full restrictions.
 */
package xyz.noisif.nsl.storage.s3;

import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.io.IoUtil;
import xyz.noisif.nsl.storage.GenericFileStorage;
import xyz.noisif.nsl.storage.StorageNamespace;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

public class S3FileStorage extends GenericFileStorage {
  private final StorageRegion storageRegion;
  private final URI endpoint;
  private final String accessKey;
  private final String secretKey;
  private final boolean forcePathStyle;
  private final StorageNamespace healthCheckBucket;

  private S3Client s3Client;

  private S3FileStorage(Builder builder) {
    super(builder);
    storageRegion = builder.storageRegion;
    endpoint = builder.endpoint;
    accessKey = builder.accessKey;
    secretKey = builder.secretKey;
    forcePathStyle = builder.forcePathStyle;
    healthCheckBucket = builder.healthCheckBucket;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected void onStart() {
    if (storageRegion.isLocal()) {
      log.warn(
          "Current S3 region is selected to: {}, make sure that is correct",
          storageRegion.getRegionCode());
    }
    final S3ClientBuilder clientBuilder =
        S3Client.builder()
            .region(Region.of(storageRegion.getRegionCode()))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .forcePathStyle(forcePathStyle);
    if (endpoint != null) {
      clientBuilder.endpointOverride(endpoint);
    }
    s3Client = clientBuilder.build();
    if (healthCheckBucket != null) {
      verifyConnection();
    } else {
      log.warn("No health-check bucket configured, skipping S3 connection verification on startup");
    }
    fileStorageAction = new S3FileStorageAction(s3Client, fileTypeRegistry);
    log.info(
        "Initialized S3 client for region: {} (region type: {})",
        storageRegion.getRegionCode(),
        storageRegion.getClass().getSimpleName());
  }

  @Override
  protected void onStop() {
    IoUtil.closeQuietly(s3Client);
  }

  private void verifyConnection() {
    final String healthCheckBucketName = healthCheckBucket.getNamespaceName();
    try {
      log.debug("Verifying connection to S3 server");
      s3Client.headBucket(
          HeadBucketRequest.builder().bucket(healthCheckBucket.getNamespaceName()).build());
      log.debug("S3 connection successfully verified");
    } catch (S3Exception ex) {
      IoUtil.closeQuietly(s3Client);
      final String exceptionMessage =
          switch (ex.statusCode()) {
            case 403 ->
                "HTTP 403: access denied, your IAM user/role does not have permission to "
                    + "access the health-check bucket: '"
                    + healthCheckBucketName
                    + "'";
            case 404 ->
                "HTTP 404: health-check bucket '" + healthCheckBucketName + "' does not exist";
            default -> "Failed to connect to S3 server or verify credentials";
          };
      throw new IllegalStateException(exceptionMessage, ex);
    } catch (Exception ex) {
      IoUtil.closeQuietly(s3Client);
      throw new IllegalStateException("Unexpected error during S3 verification.", ex);
    }
  }

  public static class Builder extends GenericFileStorage.AbstractBuilder<Builder> {
    private StorageRegion storageRegion = StorageRegion.LOCAL;
    private URI endpoint;
    private String accessKey;
    private String secretKey;
    private boolean forcePathStyle = false;
    private StorageNamespace healthCheckBucket;

    private Builder() {}

    @Override
    protected Builder self() {
      return this;
    }

    public Builder storageRegion(StorageRegion storageRegion) {
      this.storageRegion = storageRegion;
      return self();
    }

    public Builder endpoint(String endpoint) {
      this.endpoint = URI.create(endpoint);
      return self();
    }

    public Builder accessKey(String accessKey) {
      this.accessKey = accessKey;
      return self();
    }

    public Builder secretKey(String secretKey) {
      this.secretKey = secretKey;
      return self();
    }

    // true for minio self-hosted S3 storage
    public Builder forcePathStyle(boolean forcePathStyle) {
      this.forcePathStyle = forcePathStyle;
      return self();
    }

    public Builder healthCheckBucket(StorageNamespace healthCheckBucket) {
      this.healthCheckBucket = healthCheckBucket;
      return self();
    }

    @Override
    public GenericFileStorage build() {
      super.validate();
      Assert.notNull(storageRegion, "storageRegion");
      Assert.notNull(accessKey, "accessKey");
      Assert.notNull(secretKey, "secretKey");
      return new S3FileStorage(this);
    }
  }
}
