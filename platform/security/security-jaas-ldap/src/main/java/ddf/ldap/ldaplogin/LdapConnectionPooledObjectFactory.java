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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;

public class LdapConnectionPooledObjectFactory extends BasePooledObjectFactory<Connection> {

  private final ConnectionFactory ldapConnectionFactory;

  public LdapConnectionPooledObjectFactory(ConnectionFactory ldapConnectionFactory) {
    this.ldapConnectionFactory = ldapConnectionFactory;
  }

  @Override
  public Connection create() throws Exception {
    return ldapConnectionFactory.getConnection();
  }

  @Override
  public PooledObject<Connection> wrap(Connection connection) {
    return new DefaultPooledObject<>(connection);
  }

  @Override
  public void destroyObject(PooledObject<Connection> p) throws Exception {
    if (p != null && p.getObject() != null && !p.getObject().isClosed()) {
      p.getObject().close();
    }
  }

  @Override
  public boolean validateObject(PooledObject<Connection> p) {
    return p != null
        && p.getObject() != null
        && !p.getObject().isClosed()
        && p.getObject().isValid();
  }
}
