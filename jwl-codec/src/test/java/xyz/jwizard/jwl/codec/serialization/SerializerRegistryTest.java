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
package xyz.jwizard.jwl.codec.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

@ExtendWith(MockitoExtension.class)
class SerializerRegistryTest {
  private SerializerRegistry<Serializer> registry;

  @Mock private Serializer jsonSerializer;
  @Mock private Serializer protobufSerializer;

  @BeforeEach
  void setUp() {
    registry = SerializerRegistry.create();
  }

  @Test
  @DisplayName("should register and retrieve serializer by format")
  void shouldRegisterAndGetSerializer() {
    // given
    when(jsonSerializer.getFormat()).thenReturn(StandardSerializerFormat.JSON);
    when(protobufSerializer.getFormat()).thenReturn(StandardSerializerFormat.PROTOBUF);
    // when
    registry.register(jsonSerializer);
    registry.register(protobufSerializer);
    // then
    assertThat(registry.get(StandardSerializerFormat.JSON)).isEqualTo(jsonSerializer);
    assertThat(registry.get(StandardSerializerFormat.PROTOBUF)).isEqualTo(protobufSerializer);
  }

  @Test
  @DisplayName("should throw exception when serializer is not found")
  void shouldThrowExceptionWhenNotFound() {
    // then
    assertThatThrownBy(() -> registry.get(StandardSerializerFormat.RAW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No registered element found for key:");
  }

  @Test
  @DisplayName("should overwrite serializer when registering with same format")
  void shouldOverwriteOnDuplicateRegistration() {
    // given
    when(jsonSerializer.getFormat()).thenReturn(StandardSerializerFormat.JSON);
    registry.register(jsonSerializer);
    final Serializer newJsonSerializer = mock(Serializer.class);
    when(newJsonSerializer.getFormat()).thenReturn(StandardSerializerFormat.JSON);
    // when & then
    assertThatThrownBy(() -> registry.register(newJsonSerializer))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("is already registered");
  }

  @Test
  @DisplayName("should return all registered serializers")
  void shouldReturnAllSerializers() {
    // given
    when(jsonSerializer.getFormat()).thenReturn(StandardSerializerFormat.JSON);
    when(protobufSerializer.getFormat()).thenReturn(StandardSerializerFormat.PROTOBUF);
    registry.register(jsonSerializer);
    registry.register(protobufSerializer);
    // when
    final Collection<Serializer> all = registry.getAll();
    // then
    assertThat(all).hasSize(2).containsExactlyInAnyOrder(jsonSerializer, protobufSerializer);
  }

  @Test
  @DisplayName("should support fluent api for registration")
  void shouldSupportFluentApi() {
    // given
    when(jsonSerializer.getFormat()).thenReturn(StandardSerializerFormat.JSON);
    // when
    final SerializerRegistry<Serializer> returnedRegistry = registry.register(jsonSerializer);
    // then
    assertThat(returnedRegistry).isSameAs(registry);
  }
}
