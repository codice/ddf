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
package org.codice.ddf.config;

import java.io.File;
import java.net.URL;
import java.util.Set;
import org.codice.ddf.config.model.impl.RegistryConfigImpl;
import org.codice.ddf.config.reader.impl.YamlConfigReaderImpl;
import org.junit.Test;

public class YamlConfigReaderTest {

  @Test
  public void test12() throws Exception {
    RegistryConfigImpl rc = new RegistryConfigImpl();
    rc.setName("my_reg");
    rc.setId("abc");
    rc.setPublish(true);
    rc.setPull(true);
    rc.setPush(true);
    rc.setUrl(new URL("http://localhost:8993/registry"));
    rc.setVersion(456);
    System.out.println("Id: " + rc.getId());
    YamlConfigReaderImpl yc = new YamlConfigReaderImpl();
    File config = new File(getClass().getClassLoader().getResource("proxy.yml").getFile());
    Set<Config> configs = yc.read(config);
    System.out.println(configs);
    for (Config c : configs) {
      System.out.println("class: " + c.getClass());
    }
  }
}
