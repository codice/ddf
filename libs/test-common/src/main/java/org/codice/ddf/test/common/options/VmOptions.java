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
package org.codice.ddf.test.common.options;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;

import java.io.File;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

/** Options for configuring the JVM test environment */
public class VmOptions {

  private VmOptions() {}

  public static Option defaultVmOptions() {
    return new DefaultCompositeOption(
        vmOption("-Xmx6144M"),
        vmOption("-Xms2048M"),

        // avoid integration tests stealing focus on OS X
        vmOption("-Djava.awt.headless=true"),
        vmOption("-Dfile.encoding=UTF8"));
  }

  public static Option javaModuleVmOptions() {
    String karafVersion = MavenUtils.getArtifactVersion("org.apache.karaf", "karaf");
    return new DefaultCompositeOption(
        systemProperty("pax.exam.osgi.`unresolved.fail").value("true"),
        vmOption("--add-reads=java.xml=java.logging"),
        vmOption("--add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
        vmOption("--patch-module"),
        vmOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" + karafVersion + ".jar"),
        vmOption("--patch-module"),
        vmOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + karafVersion + ".jar"),
        vmOption("--add-opens"),
        vmOption("java.base/java.security=ALL-UNNAMED"),
        vmOption("--add-opens"),
        vmOption("java.base/java.io=ALL-UNNAMED"),
        vmOption("--add-opens"),
        vmOption("java.base/java.net=ALL-UNNAMED"),
        vmOption("--add-opens"),
        vmOption("java.base/java.lang=ALL-UNNAMED"),
        vmOption("--add-opens"),
        vmOption("java.base/java.util=ALL-UNNAMED"),
        vmOption("--add-opens"),
        vmOption("java.base/jdk.internal.reflect=ALL-UNNAMED"),
        vmOption("--add-opens"),
        vmOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
        vmOption("--add-opens"),
        vmOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
        vmOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
        vmOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
        vmOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
        vmOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
        vmOption("--add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED"),
        vmOption("-classpath"),
        vmOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*"));
  }
}
