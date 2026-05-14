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
package xyz.jwizard.jwl.codec.envelope.json;

import java.nio.charset.StandardCharsets;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.envelope.EncodedPayloadVisitor;
import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.codec.serialization.json.JsonSerializer;

public class JsonTextEnvelopeSerializer extends JsonEnvelopeSerializer<String> {
    private JsonTextEnvelopeSerializer(JsonSerializer serializer) {
        super(serializer);
    }

    public static JsonTextEnvelopeSerializer createDefault(JsonSerializer serializer) {
        return new JsonTextEnvelopeSerializer(serializer);
    }

    @Override
    public DataType getCodecDataType() {
        return DataType.TEXT;
    }

    @Override
    public String serializeForSession(OpCode opCode, Object payload) {
        return serializeEnvelopeAsString(opCode, payload);
    }

    @Override
    public void serializeAndAccept(OpCode opCode, Object payload, EncodedPayloadVisitor visitor) {
        visitor.accept(serializeForSession(opCode, payload));
    }

    @Override
    public void acceptRaw(byte[] rawPayload, EncodedPayloadVisitor visitor) {
        visitor.accept(new String(rawPayload, StandardCharsets.UTF_8));
    }
}
