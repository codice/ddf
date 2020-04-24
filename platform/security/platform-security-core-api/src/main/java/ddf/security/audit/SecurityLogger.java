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
package ddf.security.audit;

import org.apache.logging.log4j.util.Supplier;
import org.apache.shiro.subject.Subject;

public interface SecurityLogger {

  void audit(String message, Subject subject);

  void audit(String message);

  void audit(String message, Subject subject, Object... params);

  void audit(String message, Object... params);

  void audit(String message, Subject subject, Supplier... paramSuppliers);

  void audit(String message, Supplier... paramSuppliers);

  void audit(String message, Subject subject, Throwable t);

  void audit(String message, Throwable t);

  void auditWarn(String message, Subject subject);

  void auditWarn(String message);

  void auditWarn(String message, Subject subject, Object... params);

  void auditWarn(String message, Object... params);

  void auditWarn(String message, Subject subject, Supplier... paramSuppliers);

  void auditWarn(String message, Supplier... paramSuppliers);

  void auditWarn(String message, Subject subject, Throwable t);

  void auditWarn(String message, Throwable t);
}
