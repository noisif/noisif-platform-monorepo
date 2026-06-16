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

public enum GcpStorageRegion implements StorageRegion {
  // multi-regions
  ASIA("asia"),
  EU("eu"),
  US("us"),

  // dual-regions
  ASIA1("asia1"),
  EUR4("eur4"),
  NAM4("nam4"),

  // single-regions - Europe
  EUROPE_CENTRAL2("europe-central2"),
  EUROPE_WEST1("europe-west1"),
  EUROPE_WEST2("europe-west2"),
  EUROPE_WEST3("europe-west3"),
  EUROPE_WEST4("europe-west4"),
  EUROPE_WEST6("europe-west6"),
  EUROPE_WEST8("europe-west8"),
  EUROPE_WEST9("europe-west9"),
  EUROPE_WEST10("europe-west10"),
  EUROPE_WEST12("europe-west12"),
  EUROPE_NORTH1("europe-north1"),
  EUROPE_SOUTHWEST1("europe-southwest1"),

  // single-regions - North America
  US_CENTRAL1("us-central1"),
  US_EAST1("us-east1"),
  US_EAST4("us-east4"),
  US_EAST5("us-east5"),
  US_SOUTH1("us-south1"),
  US_WEST1("us-west1"),
  US_WEST2("us-west2"),
  US_WEST3("us-west3"),
  US_WEST4("us-west4"),
  NORTHAMERICA_NORTHEAST1("northamerica-northeast1"),
  NORTHAMERICA_NORTHEAST2("northamerica-northeast2"),

  // single-regions - South America
  SOUTHAMERICA_EAST1("southamerica-east1"),
  SOUTHAMERICA_WEST1("southamerica-west1"),

  // single-regions - Asia and Oceania
  ASIA_EAST1("asia-east1"),
  ASIA_EAST2("asia-east2"),
  ASIA_NORTHEAST1("asia-northeast1"),
  ASIA_NORTHEAST2("asia-northeast2"),
  ASIA_NORTHEAST3("asia-northeast3"),
  ASIA_SOUTH1("asia-south1"),
  ASIA_SOUTH2("asia-south2"),
  ASIA_SOUTHEAST1("asia-southeast1"),
  ASIA_SOUTHEAST2("asia-southeast2"),
  AUSTRALIA_SOUTHEAST1("australia-southeast1"),
  AUSTRALIA_SOUTHEAST2("australia-southeast2"),

  // single-regions - Middle East and Africa
  ME_WEST1("me-west1"),
  ME_CENTRAL1("me-central1"),
  ME_CENTRAL2("me-central2"),
  AFRICA_SOUTH1("africa-south1"),
  ;

  private final String regionCode;

  GcpStorageRegion(String regionCode) {
    this.regionCode = regionCode;
  }

  @Override
  public String getRegionCode() {
    return regionCode;
  }
}
