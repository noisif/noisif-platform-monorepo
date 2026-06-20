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
package xyz.noisif.nsl.netclient.group;

import xyz.noisif.nsl.common.util.Assert;
import xyz.noisif.nsl.common.util.io.IoUtil;

public class GenericClientGroupConfig implements ClientGroupConfig {
  protected final String url;
  protected final String principalId;

  protected GenericClientGroupConfig(AbstractBuilder<?, ?> builder) {
    url = builder.url;
    principalId = builder.principalId;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getPrincipalId() {
    return principalId;
  }

  public abstract static class AbstractBuilder<
      B extends AbstractBuilder<B, C>, C extends GenericClientGroupConfig> {
    private String url;
    private String principalId;

    protected AbstractBuilder() {}

    protected abstract B self();

    public B url(String url) {
      this.url = IoUtil.removeTrailingSlash(url);
      return self();
    }

    public B principalId(String principalId) {
      this.principalId = principalId;
      return self();
    }

    protected void validate() {
      Assert.notNull(url, "url");
      Assert.notNull(principalId, "principalId");
    }

    public abstract C build();
  }
}
