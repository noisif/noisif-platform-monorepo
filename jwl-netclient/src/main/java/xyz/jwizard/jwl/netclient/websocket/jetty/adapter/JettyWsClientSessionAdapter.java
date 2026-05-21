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
package xyz.jwizard.jwl.netclient.websocket.jetty.adapter;

import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;

import xyz.jwizard.jwl.common.util.concurrent.ConcurrentUtil;
import xyz.jwizard.jwl.net.ws.GenericWsSessionAdapter;
import xyz.jwizard.jwl.netclient.group.ClientGroup;
import xyz.jwizard.jwl.netclient.websocket.WsClientSession;
import xyz.jwizard.jwl.netclient.websocket.group.codec.WsSessionCodec;

public class JettyWsClientSessionAdapter extends GenericWsSessionAdapter implements WsClientSession {
    private final ClientGroup clientGroup;
    private final Session session;
    private final WsSessionCodec sessionCodec;

    public JettyWsClientSessionAdapter(ClientGroup clientGroup, Session session, String principalId,
                                       WsSessionCodec sessionCodec) {
        super(principalId, sessionCodec);
        this.clientGroup = clientGroup;
        this.session = session;
        this.sessionCodec = sessionCodec;
    }

    @Override
    public ClientGroup getGroup() {
        return clientGroup;
    }

    @Override
    public void sendObject(Object payload) {
        sessionCodec.sendObject(payload, this);
    }

    @Override
    public <T> T parse(byte[] payload, Class<T> type) {
        return sessionCodec.parse(payload, type);
    }

    @Override
    public <T> T parse(String payload, Class<T> type) {
        return sessionCodec.parse(payload, type);
    }

    @Override
    protected void onSend(String message) {
        ConcurrentUtil.await(cb -> session.sendText(message, new JettyCallbackAdapter(cb)));
    }

    @Override
    protected void onSend(byte[] message) {
        ConcurrentUtil.await(cb ->
            session.sendBinary(ByteBuffer.wrap(message), new JettyCallbackAdapter(cb))
        );
    }

    @Override
    protected void onClose(int code, String reason) {
        ConcurrentUtil.await(cb -> session.close(code, reason, new JettyCallbackAdapter(cb)));
    }

    @Override
    public boolean isClosed() {
        return !session.isOpen();
    }
}
