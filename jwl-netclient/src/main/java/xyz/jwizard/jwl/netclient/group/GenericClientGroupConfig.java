/*
 * Copyright 2026 by JWizard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jwizard.jwl.netclient.group;

import xyz.jwizard.jwl.common.util.Assert;
import xyz.jwizard.jwl.common.util.io.IoUtil;

public class GenericClientGroupConfig implements ClientGroupConfig {
    protected final String url;
    protected final String principalName;

    protected GenericClientGroupConfig(AbstractBuilder<?, ?> builder) {
        url = builder.url;
        principalName = builder.principalName;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getPrincipalName() {
        return principalName;
    }

    public abstract static class AbstractBuilder<B extends AbstractBuilder<B, C>,
        C extends GenericClientGroupConfig> {
        private String url;
        private String principalName;

        protected AbstractBuilder() {
        }

        protected abstract B self();

        public B url(String url) {
            this.url = IoUtil.removeTrailingSlash(url);
            return self();
        }

        public B principalName(String principalName) {
            this.principalName = principalName;
            return self();
        }

        protected void validate() {
            Assert.notNull(url, "Url cannot be null");
            Assert.notNull(principalName, "PrincipalName cannot be null");
        }

        public abstract C build();
    }
}
