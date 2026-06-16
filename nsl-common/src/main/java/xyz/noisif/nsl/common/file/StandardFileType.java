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
package xyz.noisif.nsl.common.file;

public enum StandardFileType implements FileType {
  // image
  PNG("image/png", "png"),
  JPEG("image/jpeg", "jpg"),
  GIF("image/gif", "gif"),
  WEBP("image/webp", "webp"),
  AVIF("image/avif", "avif"),
  // video
  WEBM("video/webm", "webm"),
  MP4("video/mp4", "mp4"),
  // audio
  MP3("audio/mpeg", "mp3"),
  WAV("audio/wav", "wav"),
  FLAC("audio/flac", "flac"),
  OGG("audio/ogg", "ogg"),
  OPUS("audio/opus", "opus"),
  // other
  RAW("application/octet-stream", "bin"),
  ;

  private final String mimeType;
  private final String ext;

  StandardFileType(String mimeType, String ext) {
    this.mimeType = mimeType;
    this.ext = ext;
  }

  @Override
  public String getMimeType() {
    return mimeType;
  }

  @Override
  public String getExt() {
    return ext;
  }
}
