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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.file.FileType;
import xyz.noisif.nsl.common.file.FileTypeRegistry;
import xyz.noisif.nsl.storage.FileContent;
import xyz.noisif.nsl.storage.FileStorageAction;
import xyz.noisif.nsl.storage.StorageNamespace;
import xyz.noisif.nsl.storage.StorageOperationException;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.List;

class S3FileStorageAction implements FileStorageAction {
  private static final Logger LOG = LoggerFactory.getLogger(S3FileStorageAction.class);

  private final S3Client s3Client;
  private final FileTypeRegistry fileTypeRegistry;

  S3FileStorageAction(S3Client s3Client, FileTypeRegistry fileTypeRegistry) {
    this.s3Client = s3Client;
    this.fileTypeRegistry = fileTypeRegistry;
  }

  @Override
  public void save(StorageNamespace namespace, String path, byte[] content, FileType fileType) {
    LOG.trace(
        "Saving file to namespace [{}] at path [{}] with type [{}]",
        namespace,
        path,
        fileType.getMimeType());
    try {
      final PutObjectRequest req =
          PutObjectRequest.builder()
              .bucket(namespace.getNamespaceName())
              .key(path)
              .contentType(fileType.getMimeType())
              .build();
      s3Client.putObject(req, RequestBody.fromBytes(content));
      LOG.debug("Successfully saved file at path [{}] (size: {} bytes)", path, content.length);
    } catch (S3Exception ex) {
      throw new StorageOperationException("Failed to save file: " + path, ex);
    }
  }

  @Override
  public FileContent<byte[]> get(StorageNamespace namespace, String path) {
    LOG.trace("Fetching file as byte array from namespace [{}] at path [{}]", namespace, path);
    try {
      final GetObjectRequest req =
          GetObjectRequest.builder().bucket(namespace.getNamespaceName()).key(path).build();
      final ResponseBytes<GetObjectResponse> res = s3Client.getObjectAsBytes(req);
      final FileType type = fileTypeRegistry.fromMimeType(res.response().contentType());
      LOG.debug(
          "Successfully fetched file from path [{}] (size: {} bytes, type: {})",
          path,
          res.asByteArray().length,
          type.getMimeType());
      return new FileContent<>(res.asByteArray(), type);
    } catch (NoSuchKeyException ex) {
      throw new StorageOperationException("File not found: " + path, ex);
    } catch (S3Exception ex) {
      throw new StorageOperationException("Failed to retrieve file: " + path, ex);
    }
  }

  @Override
  public FileContent<InputStream> getAsInputStream(StorageNamespace namespace, String path) {
    LOG.trace("Fetching file as InputStream from namespace [{}] at path [{}]", namespace, path);
    try {
      final GetObjectRequest req =
          GetObjectRequest.builder().bucket(namespace.getNamespaceName()).key(path).build();
      final ResponseInputStream<GetObjectResponse> res = s3Client.getObject(req);
      final FileType type = fileTypeRegistry.fromMimeType(res.response().contentType());
      LOG.debug(
          "Successfully opened InputStream for file at path [{}] (type: {})",
          path,
          type.getMimeType());
      return new FileContent<>(res, type);
    } catch (NoSuchKeyException ex) {
      throw new StorageOperationException("File not found: " + path, ex);
    } catch (S3Exception ex) {
      throw new StorageOperationException("Failed to retrieve file stream: " + path, ex);
    }
  }

  @Override
  public void delete(StorageNamespace namespace, String path) {
    LOG.trace("Deleting file from namespace [{}] at path [{}]", namespace, path);
    try {
      final DeleteObjectRequest req =
          DeleteObjectRequest.builder().bucket(namespace.getNamespaceName()).key(path).build();
      s3Client.deleteObject(req);
      LOG.debug("Successfully deleted file at path [{}]", path);
    } catch (S3Exception ex) {
      throw new StorageOperationException("Failed to delete file: " + path, ex);
    }
  }

  @Override
  public boolean exists(StorageNamespace namespace, String path) {
    LOG.trace("Checking existence of file in namespace [{}] at path [{}]", namespace, path);
    try {
      s3Client.headObject(
          HeadObjectRequest.builder().bucket(namespace.getNamespaceName()).key(path).build());
      LOG.debug("File exists at path [{}]", path);
      return true;
    } catch (NoSuchKeyException ex) {
      return false;
    } catch (S3Exception ex) {
      throw new StorageOperationException("Failed to check file existence: " + path, ex);
    }
  }

  @Override
  public void copy(StorageNamespace namespace, String sourcePath, String targetPath) {
    LOG.trace(
        "Copying file in namespace [{}] from [{}] to [{}]", namespace, sourcePath, targetPath);
    try {
      final CopyObjectRequest req =
          CopyObjectRequest.builder()
              .sourceBucket(namespace.getNamespaceName())
              .sourceKey(sourcePath)
              .destinationBucket(namespace.getNamespaceName())
              .destinationKey(targetPath)
              .build();
      s3Client.copyObject(req);
      LOG.debug("Successfully copied file from [{}] to [{}]", sourcePath, targetPath);
    } catch (NoSuchKeyException ex) {
      throw new StorageOperationException("Source file not found for copy: " + sourcePath, ex);
    } catch (S3Exception ex) {
      throw new StorageOperationException(
          "Failed to copy file from " + sourcePath + " to " + targetPath, ex);
    }
  }

  @Override
  public void move(StorageNamespace namespace, String sourcePath, String targetPath) {
    LOG.trace("Moving file in namespace [{}] from [{}] to [{}]", namespace, sourcePath, targetPath);
    copy(namespace, sourcePath, targetPath);
    delete(namespace, sourcePath);
    LOG.debug("Successfully moved file from [{}] to [{}]", sourcePath, targetPath);
  }

  @Override
  public List<String> listFiles(StorageNamespace namespace, String prefix) {
    LOG.trace("Listing files in namespace [{}] with prefix [{}]", namespace, prefix);
    try {
      final ListObjectsV2Request req =
          ListObjectsV2Request.builder()
              .bucket(namespace.getNamespaceName())
              .prefix(prefix)
              .build();
      final ListObjectsV2Response res = s3Client.listObjectsV2(req);
      final List<String> files =
          res.contents().stream().map(S3Object::key).filter(key -> !key.endsWith("/")).toList();
      LOG.debug("Found {} files matching prefix [{}]", files.size(), prefix);
      return files;
    } catch (S3Exception ex) {
      throw new StorageOperationException("Failed to list files with prefix: " + prefix, ex);
    }
  }
}
