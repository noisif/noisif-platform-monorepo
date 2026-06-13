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

rootProject.name = "noisif-platform-monorepo"

include("nsl-ci")
include("nsl-codec")
include("nsl-common")
include("nsl-contracts")
include("nsl-graph")
include("nsl-http")
include("nsl-i18n")
include("nsl-kv")
include("nsl-net")
include("nsl-netclient")
include("nsl-queue")
include("nsl-sql")
include("nsl-websocket")

include("nss-api")
include("nss-cli")
include("nss-gateway")
include("nss-ingestor")
include("nss-ingress")
include("nss-registry")
include("nss-translator")
include("nss-worker")
