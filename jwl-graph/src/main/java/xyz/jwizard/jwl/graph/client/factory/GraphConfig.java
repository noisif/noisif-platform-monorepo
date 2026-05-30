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
package xyz.jwizard.jwl.graph.client.factory;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.graph.GraphProtocol;
import xyz.jwizard.jwl.net.HostPort;

public abstract class GraphConfig {
  protected final GraphProtocol protocol;
  protected final HostPort address;

  protected GraphConfig(AbstractBuilder<?, ?> builder) {
    this.protocol = builder.protocol;
    this.address = builder.address;
  }

  public GraphProtocol getProtocol() {
    return protocol;
  }

  public HostPort getAddress() {
    return address;
  }

  protected abstract static class AbstractBuilder<
      B extends AbstractBuilder<B, C>, C extends GraphConfig> {
    protected GraphProtocol protocol;
    protected HostPort address;

    protected abstract B self();

    public B protocol(GraphProtocol protocol) {
      this.protocol = protocol;
      return self();
    }

    public B address(HostPort address) {
      this.address = address;
      return self();
    }

    protected void validate() {
      Assert.notNull(protocol, "Protocol cannot be null");
      Assert.notNull(address, "Address cannot be null");
    }

    public abstract C build();
  }
}
