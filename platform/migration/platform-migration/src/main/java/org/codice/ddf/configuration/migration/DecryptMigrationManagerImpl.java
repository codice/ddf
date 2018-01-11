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
package org.codice.ddf.configuration.migration;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The decrypt migration manager decrypts an exported file. */
public class DecryptMigrationManagerImpl implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DecryptMigrationManagerImpl.class);

  private final MigrationReport report;

  private final MigrationZipFile zip;

  private final ZipOutputStream zipOutputStream;

  private final Path exportFile;

  private final Path decryptFile;

  private boolean close = false;

  @SuppressWarnings(
      "squid:S2095" /* up to the caller of this method to close it; exception is thrown if the stream cannot be created in the first place */)
  private static ZipOutputStream newZipOutputStreamFor(Path decryptFile) {
    Validate.notNull(decryptFile, "invalid null decrypt file");
    try {
      return new ZipOutputStream(
          new BufferedOutputStream(new FileOutputStream(decryptFile.toFile())));
    } catch (FileNotFoundException e) {
      throw new MigrationException(Messages.DECRYPT_FILE_CREATE_ERROR, decryptFile, e);
    }
  }

  /**
   * Creates a new migration manager for a decrypt operation.
   *
   * @param report the migration report where warnings and errors can be recorded
   * @param zip the exported zip file
   * @param decryptFile the file where to decrypt the exorted zip
   * @throws MigrationException if a failure occurs while processing the zip file (the error will
   *     not be recorded with the report)
   * @throws IllegalArgumentException if <code>report</code> is <code>null</code> or if it is not
   *     for a decrypt migration operation or if <code>zip</code> or <code>decryptFile</code> is
   *     <code>null</code>
   */
  public DecryptMigrationManagerImpl(
      MigrationReport report, MigrationZipFile zip, Path decryptFile) {
    this(report, zip, decryptFile, DecryptMigrationManagerImpl.newZipOutputStreamFor(decryptFile));
  }

  @VisibleForTesting
  DecryptMigrationManagerImpl(
      MigrationReport report, MigrationZipFile zip, Path decryptFile, ZipOutputStream zos) {
    Validate.notNull(report, "invalid null report");
    Validate.notNull(zip, "invalid null zip file");
    Validate.notNull(decryptFile, "invalid decrypt file");
    Validate.isTrue(
        report.getOperation() == MigrationOperation.DECRYPT, "invalid migration operation");
    this.report = report;
    this.exportFile = zip.getZipPath();
    this.zip = zip;
    this.decryptFile = decryptFile;
    this.zipOutputStream = zos;
  }

  /**
   * Proceeds with the decryp operation.
   *
   * @param productBranding the product branding being decrypted
   * @param productVersion the product version being decrypted
   * @throws IllegalArgumentException if <code>productBranding</code> or </code><code>productVersion
   *     </code> is <code>null</code>
   * @throws MigrationException to stop the decrypt operation
   */
  public void doDecrypt(String productBranding, String productVersion) {
    Validate.notNull(productBranding, "invalid null product branding");
    Validate.notNull(productVersion, "invalid null product version");
    final String ddfHome = System.getProperty("ddf.home");

    LOGGER.debug(
        "Decrypting {} product [{}] under [{}] from [{}] to [{}]...",
        productBranding,
        productVersion,
        ddfHome,
        exportFile,
        decryptFile);
    zip.stream().forEach(this::copyToOutputZipFile);
  }

  @Override
  public void close() throws IOException {
    if (!close) {
      this.close = true;
      IOUtils.closeQuietly(zip); // we don't care if we cannot close the input zip file or not
      zipOutputStream.close();
    }
  }

  public MigrationReport getReport() {
    return report;
  }

  public Path getExportFile() {
    return exportFile;
  }

  public Path getDecryptedFile() {
    return decryptFile;
  }

  @VisibleForTesting
  void copyToOutputZipFile(ZipEntry entry) {
    InputStream is = null;

    try {
      final ZipEntry ze = new ZipEntry(entry.getName());

      ze.setTime(entry.getTime());
      zipOutputStream.putNextEntry(ze);
      if (!entry.isDirectory()) {
        is = zip.getInputStream(entry);
        IOUtils.copy(is, zipOutputStream);
      }
    } catch (IOException e) {
      getReport().record(new MigrationException(Messages.DECRYPT_PATH_ERROR, entry.getName(), e));
    } finally {
      IOUtils.closeQuietly(is); // we do not care about failing to close this stream
    }
  }
}
