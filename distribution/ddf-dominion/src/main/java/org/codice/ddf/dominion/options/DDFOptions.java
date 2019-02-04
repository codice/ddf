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
package org.codice.ddf.dominion.options;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.codice.ddf.dominion.commons.options.CommonsOptions;
import org.codice.dominion.options.Option;

/**
 * This class defines annotations that can be used to configure PaxExam containers. It is solely
 * used for scoping.
 */
public class DDFOptions {
  @CommonsOptions.Install
  @Option.Annotation
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Documented
  public @interface Install {
    /**
     * Specifies whether Solr should be started alongside DDF (defaults to <code>false</code>).
     *
     * @return <code>true</code> if Solr should be started; <code>false</code> if not
     */
    boolean solr() default false;
  }

  private DDFOptions() {}
}
