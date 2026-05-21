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
package xyz.jwizard.jwl.netclient.websocket;

import xyz.jwizard.jwl.codec.envelope.OpCode;
import xyz.jwizard.jwl.netclient.group.ClientGroup;

public interface WsClient {
    void send(ClientGroup clientGroup, byte[] message);

    void send(ClientGroup clientGroup, String message);

    void sendObject(ClientGroup clientGroup, Object object);

    void sendEnvelope(ClientGroup clientGroup, OpCode opCode, Object data);

    default void send(byte[] message) {
        send(ClientGroup.GLOBAL, message);
    }

    default void send(String message) {
        send(ClientGroup.GLOBAL, message);
    }

    default void sendEnvelope(OpCode opCode, Object data) {
        sendEnvelope(ClientGroup.GLOBAL, opCode, data);
    }

    default void sendEnvelope(ClientGroup clientGroup, OpCode opCode) {
        sendEnvelope(clientGroup, opCode, null);
    }

    default void sendEnvelope(OpCode opCode) {
        sendEnvelope(ClientGroup.GLOBAL, opCode, null);
    }

    boolean isConnected(ClientGroup group);

    default boolean isConnected() {
        return isConnected(ClientGroup.GLOBAL);
    }
}
