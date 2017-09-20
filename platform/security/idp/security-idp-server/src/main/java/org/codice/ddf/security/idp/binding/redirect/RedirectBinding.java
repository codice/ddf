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
package org.codice.ddf.security.idp.binding.redirect;

import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;
import java.util.Map;
import org.codice.ddf.security.idp.binding.api.Binding;
import org.codice.ddf.security.idp.binding.api.RequestDecoder;
import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.binding.api.Validator;

public class RedirectBinding implements Binding {

  private final RedirectRequestDecoder decoder;

  private final RedirectResponseCreator creator;

  private final RedirectValidator validator;

  public RedirectBinding(
      SystemCrypto systemCrypto, Map<String, EntityInformation> serviceProviders) {
    decoder = new RedirectRequestDecoder();
    creator = new RedirectResponseCreator(systemCrypto, serviceProviders);
    validator = new RedirectValidator(systemCrypto, serviceProviders);
  }

  @Override
  public RequestDecoder decoder() {
    return decoder;
  }

  @Override
  public ResponseCreator creator() {
    return creator;
  }

  @Override
  public Validator validator() {
    return validator;
  }
}
