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
package xyz.jwizard.jwl.codec.serialization.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import xyz.jwizard.jwl.codec.DataType;
import xyz.jwizard.jwl.codec.EncodedPayloadVisitor;

@ExtendWith(MockitoExtension.class)
class JsonTextSerializerTest {
  @Mock private JsonSerializer engineMock;
  @Mock private EncodedPayloadVisitor visitorMock;

  @Test
  @DisplayName("should delegate text serialization and call visitor")
  void shouldDelegateTextOperations() {
    // given
    final JsonTextSerializer serializer = JsonTextSerializer.create(engineMock);
    final Object dummyPayload = new Object();
    final String expectedString = "{\"key\":\"value\"}";
    given(engineMock.serialize(dummyPayload)).willReturn(expectedString);
    given(engineMock.deserialize(expectedString, String.class)).willReturn("parsed");
    // when
    final String parsed = serializer.deserializePayload(expectedString, String.class);
    // when
    serializer.serializeAndAccept(dummyPayload, visitorMock);
    // then
    assertThat(parsed).isEqualTo("parsed");
    assertThat(serializer.getCodecDataType()).isEqualTo(DataType.TEXT);
    verify(visitorMock).accept(expectedString);
  }
}
