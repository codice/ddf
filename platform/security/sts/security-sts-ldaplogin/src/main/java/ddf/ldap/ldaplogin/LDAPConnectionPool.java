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
package ddf.ldap.ldaplogin;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;

public class LDAPConnectionPool extends GenericObjectPool<Connection> {

  public LDAPConnectionPool(LDAPConnectionFactory factory, String id) {
    super(new LdapConnectionPooledObjectFactory(factory), createGenericObjectPoolConfig(id));
  }

  private static GenericObjectPoolConfig createGenericObjectPoolConfig(String id) {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setJmxNameBase("org.apache.commons.pool2:type=LDAPConnectionPool,name=");
    config.setJmxNamePrefix(id);
    return config;
  }
}
