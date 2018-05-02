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
package org.codice.ddf.security.idp.binding.soap;

import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;
import java.util.Map;
import org.codice.ddf.security.idp.binding.api.Validator;
import org.codice.ddf.security.idp.binding.api.impl.ValidatorImpl;

public class SoapValidator extends ValidatorImpl implements Validator {
  public SoapValidator(SystemCrypto systemCrypto, Map<String, EntityInformation> serviceProviders) {
    super(systemCrypto, serviceProviders);
  }
}
