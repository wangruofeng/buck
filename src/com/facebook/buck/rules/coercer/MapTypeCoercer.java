/*
 * Copyright 2013-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.rules.coercer;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.rules.CellPathResolver;
import com.facebook.buck.rules.TargetNode;
import com.google.common.collect.ImmutableMap;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class MapTypeCoercer<K, V> extends TypeCoercer<ImmutableMap<K, V>> {
  private final TypeCoercer<K> keyTypeCoercer;
  private final TypeCoercer<V> valueTypeCoercer;

  MapTypeCoercer(TypeCoercer<K> keyTypeCoercer, TypeCoercer<V> valueTypeCoercer) {
    this.keyTypeCoercer = keyTypeCoercer;
    this.valueTypeCoercer = valueTypeCoercer;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<ImmutableMap<K, V>> getOutputClass() {
    return (Class<ImmutableMap<K, V>>) (Class<?>) ImmutableMap.class;
  }

  @Override
  public boolean hasElementClass(Class<?>... types) {
    return keyTypeCoercer.hasElementClass(types) || valueTypeCoercer.hasElementClass(types);
  }

  @Override
  public void traverse(ImmutableMap<K, V> object, Traversal traversal) {
    traversal.traverse(object);
    for (Map.Entry<K, V> element : object.entrySet()) {
      keyTypeCoercer.traverse(element.getKey(), traversal);
      valueTypeCoercer.traverse(element.getValue(), traversal);
    }
  }

  @Override
  public ImmutableMap<K, V> coerce(
      CellPathResolver cellRoots,
      ProjectFilesystem filesystem,
      Path pathRelativeToProjectRoot,
      Object object)
      throws CoerceFailedException {
    if (object instanceof Map) {
      ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

      for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
        K key = keyTypeCoercer.coerce(
            cellRoots,
            filesystem,
            pathRelativeToProjectRoot,
            entry.getKey());
        V value = valueTypeCoercer.coerce(
            cellRoots,
            filesystem,
            pathRelativeToProjectRoot,
            entry.getValue());
        builder.put(key, value);
      }

      return builder.build();
    } else {
      throw CoerceFailedException.simple(object, getOutputClass());
    }
  }

  @Override
  protected <U> ImmutableMap<K, V> mapAllInternal(
      Function<U, U> function,
      Class<U> targetClass,
      ImmutableMap<K, V> object) throws CoerceFailedException {
    boolean keysHaveTargetNode = keyTypeCoercer.hasElementClass(TargetNode.class);
    boolean valuesHaveTargetNode = valueTypeCoercer.hasElementClass(TargetNode.class);
    if (!keysHaveTargetNode && !valuesHaveTargetNode) {
      return object;
    }
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    for (Map.Entry<K, V> entry : object.entrySet()) {
      K key = entry.getKey();
      if (keysHaveTargetNode) {
        key = keyTypeCoercer.mapAll(function, targetClass, key);
      }
      V value = entry.getValue();
      if (valuesHaveTargetNode) {
        value = valueTypeCoercer.mapAll(function, targetClass, value);
      }
      builder.put(key, value);
    }
    return builder.build();
  }
}
