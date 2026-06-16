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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import xyz.noisif.nsl.common.file.FileTypeRegistry;
import xyz.noisif.nsl.common.file.StandardFileType;
import xyz.noisif.nsl.common.util.StringUtil;
import xyz.noisif.nsl.common.util.io.IoUtil;
import xyz.noisif.nsl.storage.FileContent;
import xyz.noisif.nsl.storage.FileStorageAction;
import xyz.noisif.nsl.storage.GenericFileStorage;
import xyz.noisif.nsl.storage.ResolvablePath;
import xyz.noisif.nsl.storage.StorageOperationException;
import xyz.noisif.nsl.storage.TestStorageNamespace;
import xyz.noisif.nsl.storage.TestStoragePath;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Testcontainers
class S3FileStorageIntegrationTest {
  private static final TestStorageNamespace BUCKET = TestStorageNamespace.MAIN_BUCKET;

  @Container
  private static final MinIOContainer minio =
      new MinIOContainer(DockerImageName.parse("minio/minio:latest"));

  private GenericFileStorage s3FileStorage;
  private FileStorageAction action;

  @BeforeAll
  static void setupBucket() {
    try (S3Client setupClient =
        S3Client.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create(minio.getS3URL()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(minio.getUserName(), minio.getPassword())))
            .forcePathStyle(true)
            .build()) {
      setupClient.createBucket(
          CreateBucketRequest.builder().bucket(BUCKET.getNamespaceName()).build());
    }
  }

  @BeforeEach
  void setUp() {
    s3FileStorage =
        S3FileStorage.builder()
            .storageRegion(StorageRegion.LOCAL)
            .endpoint(minio.getS3URL())
            .accessKey(minio.getUserName())
            .secretKey(minio.getPassword())
            .forcePathStyle(true)
            .healthCheckBucket(BUCKET)
            .fileTypeRegistry(
                FileTypeRegistry.createWithOverwrite(StandardFileType.RAW).withDefaults())
            .build();
    s3FileStorage.start();
    action = s3FileStorage.getFileStorageAction();
  }

  @AfterEach
  void tearDown() {
    IoUtil.closeQuietly(s3FileStorage);
  }

  @Test
  @DisplayName("should save file and confirm it exists")
  void shouldSaveAndCheckExistence() {
    // given
    final String path = TestStoragePath.USER_AVATAR.buildWithArgs("123");
    final byte[] content = "fake-png-content".getBytes(StandardCharsets.UTF_8);
    // when
    action.save(BUCKET, path, content, StandardFileType.PNG);
    // then
    assertTrue(action.exists(BUCKET, path), "File should exist in S3 after saving");
  }

  @Test
  @DisplayName("should retrieve file as byte array with correct content and type")
  void shouldRetrieveFileAsByteArray() {
    // given
    final String path = TestStoragePath.SIMPLE_FILE.buildWithArgs();
    final byte[] originalContent = "Hello NSL".getBytes(StandardCharsets.UTF_8);
    action.save(BUCKET, path, originalContent, StandardFileType.RAW);
    // when
    final FileContent<byte[]> result = action.get(BUCKET, path);
    // then
    assertArrayEquals(
        originalContent, result.data(), "Retrieved content should match the original");
    assertEquals(StandardFileType.RAW.getMimeType(), result.fileType().getMimeType());
  }

  @Test
  @DisplayName("should retrieve file as InputStream")
  void shouldRetrieveFileAsInputStream() throws Exception {
    // given
    final String path = TestStoragePath.DOCUMENT.buildWithArgs("user-99", "invoice");
    final byte[] originalContent = "PDF-CONTENT".getBytes(StandardCharsets.UTF_8);
    action.save(BUCKET, path, originalContent, StandardFileType.RAW);
    // when
    final FileContent<InputStream> result = action.getAsInputStream(BUCKET, path);
    // then
    try (final InputStream is = result.data()) {
      final byte[] retrievedBytes = is.readAllBytes();
      assertArrayEquals(originalContent, retrievedBytes);
    }
  }

  @Test
  @DisplayName("should delete existing file")
  void shouldDeleteFile() {
    // given
    final String path = "temp/to-be-deleted.txt";
    action.save(BUCKET, path, StringUtil.getBytes("content"), StandardFileType.RAW);
    assertTrue(action.exists(BUCKET, path));
    // when
    action.delete(BUCKET, path);
    // then
    assertFalse(action.exists(BUCKET, path), "File should not exist after deletion");
  }

  @Test
  @DisplayName("should copy file from one path to another")
  void shouldCopyFile() {
    // given
    final String source = "source/copy-test.txt";
    final String target = "target/copy-test.txt";
    action.save(BUCKET, source, StringUtil.getBytes("data"), StandardFileType.RAW);
    // when
    action.copy(BUCKET, source, target);
    // then
    assertTrue(action.exists(BUCKET, source), "Source file should still exist");
    assertTrue(action.exists(BUCKET, target), "Target file should be created");
  }

  @Test
  @DisplayName("should move file (copy and delete original)")
  void shouldMoveFile() {
    // given
    final String source = "source/move-test.txt";
    final String target = "target/move-test.txt";
    action.save(BUCKET, source, StringUtil.getBytes("data"), StandardFileType.RAW);
    // when
    action.move(BUCKET, source, target);
    // then
    assertFalse(action.exists(BUCKET, source), "Source file should be deleted after move");
    assertTrue(action.exists(BUCKET, target), "Target file should exist after move");
  }

  @Test
  @DisplayName("should list files based on prefix")
  void shouldListFilesWithPrefix() {
    // given
    action.save(BUCKET, "folderA/file1.txt", StringUtil.getBytes("data"), StandardFileType.RAW);
    action.save(BUCKET, "folderA/file2.txt", StringUtil.getBytes("data"), StandardFileType.RAW);
    action.save(BUCKET, "folderB/file3.txt", StringUtil.getBytes("data"), StandardFileType.RAW);
    // when
    final List<String> folderAFiles = action.listFiles(BUCKET, "folderA/");
    // then
    assertEquals(2, folderAFiles.size(), "Should only find files in folderA");
    assertTrue(folderAFiles.contains("folderA/file1.txt"));
    assertTrue(folderAFiles.contains("folderA/file2.txt"));
  }

  @Test
  @DisplayName("should throw StorageOperationException when file does not exist")
  void shouldThrowExceptionOnNonExistentFile() {
    // when & then
    assertThrows(
        StorageOperationException.class,
        () -> action.get(BUCKET, "non-existent-path/file.txt"),
        "Should throw StorageOperationException when trying to get a non-existent file");
  }

  @Test
  @DisplayName("should save and check existence using StoragePath with path arguments")
  void shouldSaveAndCheckExistsWithPathArgs() {
    // given
    final String userId = "user-77";
    final String docName = "financial-report";
    final byte[] content = "Report Data".getBytes(StandardCharsets.UTF_8);
    // when
    action.save(BUCKET, TestStoragePath.DOCUMENT, content, StandardFileType.RAW, userId, docName);
    // then
    assertTrue(
        action.exists(BUCKET, TestStoragePath.DOCUMENT, userId, docName),
        "File should exist when checked via path arguments");
  }

  @Test
  @DisplayName("should retrieve file and stream using StoragePath with path arguments")
  void shouldGetFileWithPathArgs() throws Exception {
    // given
    final String userId = "user-88";
    final String docName = "contract";
    final byte[] originalContent = StringUtil.getBytes("Contract Data");
    action.save(
        BUCKET, TestStoragePath.DOCUMENT, originalContent, StandardFileType.RAW, userId, docName);
    // when
    final FileContent<byte[]> byteResult =
        action.get(BUCKET, TestStoragePath.DOCUMENT, userId, docName);
    final FileContent<InputStream> streamResult =
        action.getAsInputStream(BUCKET, TestStoragePath.DOCUMENT, userId, docName);
    // then
    assertArrayEquals(originalContent, byteResult.data(), "Byte array content should match");
    try (InputStream is = streamResult.data()) {
      assertArrayEquals(originalContent, is.readAllBytes(), "InputStream content should match");
    }
  }

  @Test
  @DisplayName("should delete file using StoragePath with path arguments")
  void shouldDeleteFileWithPathArgs() {
    // given
    final String userId = "user-99";
    final String docName = "temp-doc";
    action.save(
        BUCKET,
        TestStoragePath.DOCUMENT,
        StringUtil.getBytes("data"),
        StandardFileType.RAW,
        userId,
        docName);
    assertTrue(action.exists(BUCKET, TestStoragePath.DOCUMENT, userId, docName));
    // when
    action.delete(BUCKET, TestStoragePath.DOCUMENT, userId, docName);
    // then
    assertFalse(
        action.exists(BUCKET, TestStoragePath.DOCUMENT, userId, docName),
        "File should not exist after deletion via path arguments");
  }

  @Test
  @DisplayName("should copy and move files using ResolvablePath instances")
  void shouldCopyAndMoveWithResolvablePath() {
    // given
    final byte[] content = StringUtil.getBytes("resolvable-data");
    final var sourcePath = new ResolvablePath(TestStoragePath.DOCUMENT, "system", "original");
    final var copyTargetPath = new ResolvablePath(TestStoragePath.DOCUMENT, "system", "copy");
    final var moveTargetPath = new ResolvablePath(TestStoragePath.DOCUMENT, "system", "final-move");
    action.save(BUCKET, sourcePath.buildFromArgs(), content, StandardFileType.RAW);
    // when
    action.copy(BUCKET, sourcePath, copyTargetPath);
    // then
    assertTrue(
        action.exists(BUCKET, sourcePath.buildFromArgs()),
        "Original source should still exist after copy");
    assertTrue(action.exists(BUCKET, copyTargetPath.buildFromArgs()), "Copied target should exist");
    // when
    action.move(BUCKET, sourcePath, moveTargetPath);
    // then
    assertFalse(
        action.exists(BUCKET, sourcePath.buildFromArgs()), "Source should be deleted after move");
    assertTrue(action.exists(BUCKET, moveTargetPath.buildFromArgs()), "Moved target should exist");
  }
}
