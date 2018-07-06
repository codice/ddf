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
package org.codice.ddf.config.reader.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.codice.ddf.config.Config;
import org.codice.ddf.config.model.impl.CswFederationProfileConfigImpl;
import org.codice.ddf.config.model.impl.MimeTypeConfigImpl;
import org.codice.ddf.config.model.impl.RegistryConfigImpl;
import org.codice.ddf.config.model.impl.SchemaMimeTypeConfigImpl;
import org.codice.ddf.config.reader.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

public class YamlConfigReaderImpl implements ConfigReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(YamlConfigReaderImpl.class);

  private static final Map<Class<?>, String> TYPES =
      ImmutableMap.<Class<?>, String>builder()
          .put(CswFederationProfileConfigImpl.class, "!cswFederationProfile")
          .put(RegistryConfigImpl.class, "!registry")
          .put(MimeTypeConfigImpl.class, "!mime")
          .put(SchemaMimeTypeConfigImpl.class, "!schemaMime")
          .build();

  private final Constructor constructor =
      new CustomClassLoaderConstructor(YamlConfigReaderImpl.class.getClassLoader());

  public YamlConfigReaderImpl() {
    TYPES.forEach(this::addType);
  }

  @Override
  public Set<Config> read(File config) throws IOException {
    Yaml yaml = new Yaml(constructor);

    try (InputStream is = new FileInputStream(config.getAbsoluteFile())) {
      LOGGER.error("##### Reading file: {}", config.getAbsoluteFile());
      Iterable<Object> iterable = yaml.loadAll(is);
      LOGGER.error("##### Done reading file. ");
      Set<Config> configs =
          StreamSupport.stream(iterable.spliterator(), false)
              .filter(e -> e instanceof Config)
              .map(e -> (Config) e)
              .collect(Collectors.toSet());
      return ImmutableSet.<Config>builder().addAll(configs).build();
    }
  }

  private void addType(Class<?> typeClass, String tag) {
    constructor.addTypeDescription(new TypeDescription(typeClass, tag));
  }
}
