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
import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;

/**
 * Based on {@link org.apache.commons.io.monitor.FileAlterationListener}. Used by the {@link
 * AsyncFileAlterationObserver} to handle when a file has been changed.
 *
 * @implNote The {@link AsyncFileAlterationListener} must call {@link
 *     Synchronization#onComplete(Exchange)} or {@link Synchronization#onFailure(Exchange)} with
 *     {@code null} if a success or failure occurred. Alternatively the {@link Synchronization} can
 *     be added to an {@link org.apache.camel.Exchange} via {@link
 *     Exchange#addOnCompletion(Synchronization)}.
 */
public interface AsyncFileAlterationListener {

  void onStart(final AsyncFileAlterationObserver observer);

  void onFileCreate(final File file, final Synchronization callback);

  void onFileChange(final File file, final Synchronization callback);

  void onFileDelete(final File file, final Synchronization callback);

  void onStop(final AsyncFileAlterationObserver observer);
}
