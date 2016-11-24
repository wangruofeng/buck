/*
 * Copyright 2015-present Facebook, Inc.
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
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.Pair;
import com.facebook.buck.rules.CellPathResolver;
import com.facebook.buck.rules.TargetNode;
import com.facebook.buck.versions.Version;
import com.google.common.collect.ImmutableMap;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class VersionMatchedCollectionTypeCoercer<T>
    extends TypeCoercer<VersionMatchedCollection<T>> {

  TypeCoercer<ImmutableMap<BuildTarget, Version>> versionsTypeCoercer;
  TypeCoercer<T> valueTypeCoercer;

  public VersionMatchedCollectionTypeCoercer(
      TypeCoercer<ImmutableMap<BuildTarget, Version>> versionsTypeCoercer,
      TypeCoercer<T> valueTypeCoercer) {
    this.versionsTypeCoercer = versionsTypeCoercer;
    this.valueTypeCoercer = valueTypeCoercer;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<VersionMatchedCollection<T>> getOutputClass() {
    return (Class<VersionMatchedCollection<T>>) (Class<?>) VersionMatchedCollection.class;
  }

  @Override
  public boolean hasElementClass(Class<?>... types) {
    return versionsTypeCoercer.hasElementClass(types) || valueTypeCoercer.hasElementClass(types);
  }

  @Override
  public void traverse(VersionMatchedCollection<T> object, Traversal traversal) {
    for (Pair<ImmutableMap<BuildTarget, Version>, T> pair : object.getValuePairs()) {
      versionsTypeCoercer.traverse(pair.getFirst(), traversal);
      valueTypeCoercer.traverse(pair.getSecond(), traversal);
    }
  }

  @Override
  public VersionMatchedCollection<T> coerce(
      CellPathResolver cellRoots,
      ProjectFilesystem filesystem,
      Path pathRelativeToProjectRoot,
      Object object) throws CoerceFailedException {
    if (!(object instanceof List)) {
      throw CoerceFailedException.simple(
          object,
          getOutputClass(),
          "input object should be a list of pairs");
    }
    VersionMatchedCollection.Builder<T> builder = VersionMatchedCollection.builder();
    List<?> list = (List<?>) object;
    for (Object element : list) {
      if (!(element instanceof Collection) || ((Collection<?>) element).size() != 2) {
        throw CoerceFailedException.simple(
            object,
            getOutputClass(),
            "input object should be a list of pairs");
      }
      Iterator<?> pair = ((Collection<?>) element).iterator();
      ImmutableMap<BuildTarget, Version> versionsSelector =
          versionsTypeCoercer.coerce(
              cellRoots,
              filesystem,
              pathRelativeToProjectRoot,
              pair.next());
      T value =
          valueTypeCoercer.coerce(
              cellRoots,
              filesystem,
              pathRelativeToProjectRoot,
              pair.next());
      builder.add(versionsSelector, value);
    }
    return builder.build();
  }

  @Override
  protected <U> VersionMatchedCollection<T> mapAllInternal(
      Function<U, U> function,
      Class<U> targetClass,
      VersionMatchedCollection<T> object) throws CoerceFailedException {
    boolean versionsHaveTargetNode = versionsTypeCoercer.hasElementClass(TargetNode.class);
    boolean valuesHaveTargetNode = valueTypeCoercer.hasElementClass(TargetNode.class);
    if (!versionsHaveTargetNode && !valuesHaveTargetNode) {
      return object;
    }
    VersionMatchedCollection.Builder<T> builder = VersionMatchedCollection.builder();
    for (Pair<ImmutableMap<BuildTarget, Version>, T> pair : object.getValuePairs()) {
      ImmutableMap<BuildTarget, Version> versions = pair.getFirst();
      if (versionsHaveTargetNode) {
        versions = versionsTypeCoercer.mapAll(function, targetClass, versions);
      }
      T value = pair.getSecond();
      if (valuesHaveTargetNode) {
        value = valueTypeCoercer.mapAll(function, targetClass, value);
      }
      builder.add(versions, value);
    }
    return builder.build();
  }
}
