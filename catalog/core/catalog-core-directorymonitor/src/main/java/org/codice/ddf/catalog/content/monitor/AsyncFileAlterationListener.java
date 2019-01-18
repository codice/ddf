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
package org.codice.ddf.catalog.content.monitor;

import java.io.File;
import org.apache.camel.spi.Synchronization;

/**
 * AsyncFileAlterationListener
 *
 * <p>Based upon Apache's {@link org.apache.commons.io.monitor.FileAlterationListener}. Used by the
 * {@link AsyncFileAlterationObserver} to inform when a file has been changed.
 *
 * @implNote The {@link Synchronization} cb must be passed into a {@link org.apache.camel.Exchange}
 *     or called manually by {@code cb.onSuccess} / {@code cb.onFailure} depending if a success or
 *     failure occurred.
 */
public interface AsyncFileAlterationListener {

  void onStart(final AsyncFileAlterationObserver observer);

  void onFileCreate(File file, Synchronization cb);

  void onFileChange(File file, Synchronization cb);

  void onFileDelete(File file, Synchronization cb);

  void onStop(final AsyncFileAlterationObserver observer);
}
