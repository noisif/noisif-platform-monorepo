/*
 * Copyright (c) 2022-2026 JWizard. All Rights Reserved.
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
package xyz.jwizard.jwl.netclient.group;

import xyz.jwizard.jwl.common.registry.GenericConcurrentRegistry;

public class InMemoryClientRegistry<T extends ClientGroupConfig>
    extends GenericConcurrentRegistry<ClientGroup, T> implements ClientRegistry<T> {
  private InMemoryClientRegistry() {
    super();
  }

  public static <T extends ClientGroupConfig> ClientRegistry<T> createDefault() {
    return new InMemoryClientRegistry<>();
  }

  @Override
  public void register(ClientGroup clientGroup, T config) {
    super.register(clientGroup, config);
  }

  @Override
  public void register(T config) {
    if (log.isDebugEnabled()) {
      log.debug("Registering configuration under default GLOBAL client group");
    }
    super.register(ClientGroup.GLOBAL, config);
  }
}
