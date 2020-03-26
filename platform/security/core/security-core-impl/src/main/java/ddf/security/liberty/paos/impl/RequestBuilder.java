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
package ddf.security.liberty.paos.impl;

import ddf.security.liberty.paos.Request;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import org.opensaml.saml.common.AbstractSAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;

public class RequestBuilder extends AbstractSAMLObjectBuilder<RequestImpl> {
  @Nonnull
  @Override
  public RequestImpl buildObject() {
    return new RequestImpl(
        SAMLConstants.PAOS_NS, Request.DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.PAOS_PREFIX);
  }

  @Nonnull
  @Override
  public RequestImpl buildObject(
      @Nullable String uri, @Nonnull @NotEmpty String localName, @Nullable String prefix) {
    return new RequestImpl(uri, localName, prefix);
  }
}
