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
package xyz.jwizard.jwl.codec.envelope;

import org.jspecify.annotations.NonNull;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.serialization.SerializerFormat;

public record EnvelopeSerializerFormat(
    SerializerFormat baseFormat,
    DataType dataType
) implements SerializerFormat {
    public static EnvelopeSerializerFormat from(SerializerFormat format,
                                                DataType dataType) {
        return new EnvelopeSerializerFormat(format, dataType);
    }

    @Override
    public String getFormat() {
        return baseFormat.getFormat() + "+" + dataType.getCode();
    }

    @Override
    public String getMimeType() {
        // application/x-jwl-[envelope]+[base]
        return "application/x-jwl-" + dataType.getCode() + "+" + baseFormat.getFormat();
    }

    @Override
    @NonNull
    public String toString() {
        return getFormat();
    }
}
