/*
 * Copyright 2016-present Facebook, Inc.
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

package com.facebook.buck.jvm.scala;

import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.jvm.java.ConfiguredCompiler;
import com.facebook.buck.jvm.java.ConfiguredCompilerFactory;
import com.facebook.buck.jvm.java.ExtraClasspathProvider;
import com.facebook.buck.jvm.java.JavaBuckConfig;
import com.facebook.buck.jvm.java.Javac;
import com.facebook.buck.jvm.java.JavacFactory;
import com.facebook.buck.jvm.java.JavacOptions;
import com.facebook.buck.jvm.java.JvmLibraryArg;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.SourcePathRuleFinder;
import com.facebook.buck.rules.Tool;
import com.facebook.buck.util.Optionals;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import javax.annotation.Nullable;

public class ScalaConfiguredCompilerFactory extends ConfiguredCompilerFactory {
  private final ScalaBuckConfig scalaBuckConfig;
  private final JavaBuckConfig javaBuckConfig;
  private final ExtraClasspathProvider extraClasspathProvider;
  private @Nullable Tool scalac;

  public ScalaConfiguredCompilerFactory(ScalaBuckConfig config, JavaBuckConfig javaBuckConfig) {
    this(config, javaBuckConfig, ExtraClasspathProvider.EMPTY);
  }

  public ScalaConfiguredCompilerFactory(
      ScalaBuckConfig config,
      JavaBuckConfig javaBuckConfig,
      ExtraClasspathProvider extraClasspathProvider) {
    this.scalaBuckConfig = config;
    this.javaBuckConfig = javaBuckConfig;
    this.extraClasspathProvider = extraClasspathProvider;
  }

  private Tool getScalac(BuildRuleResolver resolver) {
    if (scalac == null) {
      scalac = scalaBuckConfig.getScalac(resolver);
    }
    return scalac;
  }

  @Override
  public ConfiguredCompiler configure(
      SourcePathResolver sourcePathResolver,
      SourcePathRuleFinder ruleFinder,
      ProjectFilesystem projectFilesystem,
      @Nullable JvmLibraryArg arg,
      JavacOptions javacOptions,
      BuildRuleResolver buildRuleResolver) {

    return new ScalacToJarStepFactory(
        sourcePathResolver,
        ruleFinder,
        projectFilesystem,
        getScalac(buildRuleResolver),
        buildRuleResolver.getRule(scalaBuckConfig.getScalaLibraryTarget()),
        scalaBuckConfig.getCompilerFlags(),
        Preconditions.checkNotNull(arg).getExtraArguments(),
        buildRuleResolver.getAllRules(scalaBuckConfig.getCompilerPlugins()),
        getJavac(buildRuleResolver, arg),
        javacOptions,
        extraClasspathProvider);
  }

  @Override
  public void addTargetDeps(
      ImmutableCollection.Builder<BuildTarget> extraDepsBuilder,
      ImmutableCollection.Builder<BuildTarget> targetGraphOnlyDepsBuilder) {

    extraDepsBuilder
        .add(scalaBuckConfig.getScalaLibraryTarget())
        .addAll(scalaBuckConfig.getCompilerPlugins());
    Optionals.addIfPresent(scalaBuckConfig.getScalacTarget(), extraDepsBuilder);
  }

  private Javac getJavac(BuildRuleResolver resolver, @Nullable JvmLibraryArg arg) {
    return JavacFactory.create(new SourcePathRuleFinder(resolver), javaBuckConfig, arg);
  }
}
