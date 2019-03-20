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
import org.codice.ddf.dominion.commons.options.DDFCommonOptions;
import org.codice.dominion.interpolate.Interpolate;
import org.codice.dominion.options.Option;
import org.codice.dominion.options.karaf.KarafOptions.DistributionConfiguration;

/**
 * This class defines annotations that can be used to configure Dominion containers. It is solely
 * used for scoping.
 */
public class DDFOptions {
  /** System property specifying which profile to install. */
  public static final String INSTALL_PROFILE_PROPERTY = "ddf.install.profile";

  /** System property specifying which security profile to activate. */
  public static final String SECURITY_PROFILE_PROPERTY = "ddf.security.profile";

  /** Dominion option for installing the DDF kernel. */
  @DDFCommonOptions.Install
  @Option.Annotation
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Documented
  public @interface InstallKernel {
    /**
     * Specifies whether Solr should be started alongside DDF (defaults to <code>false</code>).
     *
     * @return <code>true</code> if Solr should be started; <code>false</code> if not
     */
    boolean solr() default false;
  }

  /**
   * Dominion option for installing the DDF distribution.
   *
   * <p>Downstream distributions can register additional extensions for this option as Dominion will
   * automatically retrieve options from all registered extensions. For PaxExam containers, only the
   * distribution configuration options that have names (see {@link
   * DistributionConfiguration#name()}) that matches the {@link
   * org.codice.dominion.Dominion#DISTRIBUTION_PROPERTY} system property will be retained; thus
   * allowing a particular test written for DDF a chance to run with any downstream distributions.
   */
  @DDFCommonOptions.Install
  @Option.Annotation
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Documented
  public @interface InstallDistribution {
    /**
     * Specifies the profile to install via the <code>profile:install</code> command (defaults to
     * the standard profile unless the system property {@link #INSTALL_PROFILE_PROPERTY} is defined
     * in which case it will default to the specified profile).
     */
    @Interpolate
    String profile() default "{" + DDFOptions.INSTALL_PROFILE_PROPERTY + ":-standard}";

    /**
     * Specifies the security profile to activate (defaults to none unless the system property
     * {@link #SECURITY_PROFILE_PROPERTY} is defined in which case it will default to the specified
     * profile). Security profiles are retrieved from the installed <code>
     * etc/wf-security/profiles.json</code> file.
     */
    @Interpolate
    String security() default "{" + DDFOptions.SECURITY_PROFILE_PROPERTY + ":-}";

    /**
     * Specifies whether Solr should be started alongside DDF (defaults to <code>false</code>).
     *
     * @return <code>true</code> if Solr should be started; <code>false</code> if not
     */
    boolean solr() default false;
  }

  private DDFOptions() {}
}
