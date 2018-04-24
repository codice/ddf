/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.spock.extension.builtin

import org.codice.spock.extension.ClearInterruptions
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.SpecInfo

/**
 * Provides the extension point for the {@link ClearInterruptions} annotation.
 */
public class ClearInterruptionsExtension extends AbstractAnnotationDrivenExtension<ClearInterruptions> {
  private static def LISTENER = new IMethodInterceptor() {
    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
      try {
        invocation.proceed()
      } finally {
        Thread.interrupted()
      }
    }
  }

  @Override
  void visitSpecAnnotation(ClearInterruptions annotation, SpecInfo spec) {
    spec.features.each {
      it.addInterceptor(LISTENER)
      it.addIterationInterceptor(LISTENER)
    }
  }

  @Override
  void visitFeatureAnnotation(ClearInterruptions annotation, FeatureInfo feature) {
    feature.addInterceptor(LISTENER);
    feature.addIterationInterceptor(LISTENER)
  }
}
