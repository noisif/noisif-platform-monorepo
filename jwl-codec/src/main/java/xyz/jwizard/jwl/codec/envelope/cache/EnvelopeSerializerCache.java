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
package xyz.jwizard.jwl.codec.envelope.cache;

import xyz.jwizard.jwl.codec.envelope.EnvelopeDataType;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializer;
import xyz.jwizard.jwl.codec.envelope.EnvelopeSerializerRegistry;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;

public interface EnvelopeSerializerCache {
    EnvelopeSerializerCache init(EnvelopeSerializerRegistry registry);

    EnvelopeSerializer<?> find(String encoding, String frame);

    default EnvelopeSerializer<?> find(SerializerFormat encoding, EnvelopeDataType dataType) {
        return find(encoding.getFormat(), dataType.getCode());
    }
}
