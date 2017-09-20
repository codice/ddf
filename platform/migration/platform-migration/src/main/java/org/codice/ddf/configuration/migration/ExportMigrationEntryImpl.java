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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.BiThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class provides an implementation of the {@link ExportMigrationEntry}. */
// squid:S2160 - the base class equals() is sufficient for our needs. entries are unique based on
// their paths
@SuppressWarnings("squid:S2160")
public class ExportMigrationEntryImpl extends MigrationEntryImpl implements ExportMigrationEntry {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationEntryImpl.class);

  private static final String FAILED_TO_BE_EXPORTED = "failed to be exported";

  private final Map<String, ExportMigrationJavaPropertyReferencedEntryImpl> properties =
      new HashMap<>(8);

  private final AtomicReference<OutputStream> outputStream = new AtomicReference<>();

  private final ExportMigrationContextImpl context;

  private final Path absolutePath;

  private final Throwable absolutePathError;

  private final Path path;

  private final File file;

  private final String name;

  /**
   * Will track if store was attempted along with its result. Will be <code>null</code> until
   * store() is attempted, at which point it will start tracking the first store() result.
   */
  protected Boolean stored = null;

  /**
   * Instantiates a new migration entry given a migratable context and path.
   *
   * <p><i>Note:</i> In this version of the constructor, the path is either absolute or assumed to
   * be relative to ${ddf.home}. It will also be automatically relativized to ${ddf.home}.
   *
   * @param context the migration context associated with this entry
   * @param path the path for this entry
   * @throws IllegalArgumentException if <code>context</code> or <code>path</code> is <code>null
   *     </code>
   */
  protected ExportMigrationEntryImpl(ExportMigrationContextImpl context, Path path) {
    Validate.notNull(context, "invalid null context");
    Validate.notNull(path, "invalid null path");
    Path apath;
    IOException aerror;

    try {
      // make sure it is resolved against ddf.home and not the current working directory
      apath =
          context.getPathUtils().resolveAgainstDDFHome(path).toRealPath(LinkOption.NOFOLLOW_LINKS);
      aerror = null;
    } catch (IOException e) {
      apath = path;
      // remember the error in case the migratable attempts to store the file from disk later
      // instead of providing its own data
      aerror = e;
    }
    this.context = context;
    this.absolutePath = apath;
    this.absolutePathError = aerror;
    this.path = context.getPathUtils().relativizeFromDDFHome(apath);
    this.file = apath.toFile();
    // we keep the entry name in Unix style based on our convention
    this.name = FilenameUtils.separatorsToUnix(this.path.toString());
  }

  /**
   * Instantiates a new migration entry given a migratable context and path.
   *
   * <p><i>Note:</i> In this version of the constructor, the path is either absolute or assumed to
   * be relative to ${ddf.home}. It will also be automatically relativized to ${ddf.home}.
   *
   * @param context the migration context associated with this entry
   * @param pathname the path string for this entry
   * @throws IllegalArgumentException if <code>context</code> or <code>pathname</code> is <code>null
   *     </code>
   */
  protected ExportMigrationEntryImpl(ExportMigrationContextImpl context, String pathname) {
    this(
        context,
        Paths.get(ExportMigrationEntryImpl.validateNotNull(pathname, "invalid null pathname")));
  }

  private static <T> T validateNotNull(T t, String msg) {
    Validate.notNull(t, msg);
    return t;
  }

  @Override
  public ExportMigrationReportImpl getReport() {
    return context.getReport();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public long getLastModifiedTime() {
    return file.lastModified();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    recordEntry();
    return getOutputStreamWithoutRecordingEntry();
  }

  @Override
  public boolean store(boolean required) {
    if (stored == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Exporting {}{}...", (required ? "required " : ""), toDebugString());
      }
      this.stored = false; // until proven otherwise
      if (absolutePathError instanceof NoSuchFileException) {
        this.stored = storeWhenNoSuchFile(required);
      } else if (absolutePathError != null) {
        getReport().record(newError("cannot be read", absolutePathError));
      } else {
        this.stored = storeWhenFileExist();
      }
    }
    return stored;
  }

  @Override
  public boolean store(BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer) {
    Validate.notNull(consumer, "invalid null consumer");
    if (stored == null) {
      this.stored = false; // until proven otherwise
      try (final OutputStream os = getOutputStream()) {
        this.stored = getReport().wasIOSuccessful(() -> consumer.accept(getReport(), os));
      } catch (
          ExportIOException
              e) { // special case indicating the I/O error occurred while writing to the zip which
        // would invalidate the zip so we are forced to abort
        throw newError(ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, e.getCause());
      } catch (
          IOException
              e) { // here it means the error came out of reading/processing the input file/stream
        // where it is safe to continue with the next entry, so don't abort
        getReport().record(newError(ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, e));
      } catch (MigrationException e) {
        throw e;
      }
    }
    return stored;
  }

  @Override
  public Optional<ExportMigrationEntry> getPropertyReferencedEntry(
      String pname, BiPredicate<MigrationReport, String> validator) {
    Validate.notNull(pname, "invalid null java property name");
    Validate.notNull(validator, "invalid null validator");
    final ExportMigrationJavaPropertyReferencedEntryImpl me = properties.get(pname);

    if (me != null) {
      return Optional.of(me);
    }
    try {
      final String val = getJavaPropertyValue(pname);

      if (!validator.test(getReport(), val)) {
        return Optional.empty();
      } else if (val == null) {
        getReport()
            .record(
                new MigrationException(
                    Messages.EXPORT_JAVA_PROPERTY_NOT_DEFINED_ERROR, pname, path));
        return Optional.empty();
      } else if (val.isEmpty()) {
        getReport()
            .record(
                new MigrationException(Messages.EXPORT_JAVA_PROPERTY_IS_EMPTY_ERROR, pname, path));
        return Optional.empty();
      }
      final ExportMigrationJavaPropertyReferencedEntryImpl prop =
          new ExportMigrationJavaPropertyReferencedEntryImpl(context, path, pname, val);

      properties.put(pname, prop);
      return Optional.of(prop);
    } catch (IOException e) {
      getReport()
          .record(new MigrationException(Messages.EXPORT_JAVA_PROPERTY_LOAD_ERROR, pname, path, e));
      return Optional.empty();
    }
  }

  @Override
  protected ExportMigrationContextImpl getContext() {
    return context;
  }

  protected Path getAbsolutePath() {
    return absolutePath;
  }

  protected File getFile() {
    return file;
  }

  /** Called to record that this entry is being processed. */
  protected void recordEntry() { // nothing to record here
  }

  /**
   * Gets a debug string to represent this entry.
   *
   * @return a debug string for this entry
   */
  protected String toDebugString() {
    return String.format("file [%s] to [%s]", absolutePath, path);
  }

  protected MigrationWarning newWarning(String reason) {
    return new MigrationWarning(Messages.EXPORT_PATH_WARNING, path, reason);
  }

  protected MigrationException newError(String reason, Throwable cause) {
    return new MigrationException(Messages.EXPORT_PATH_ERROR, path, reason, cause);
  }

  private boolean storeWhenNoSuchFile(boolean required) {
    // we cannot rely on file.exists() here since the path is not valid anyway
    // relying on the exception is much safer and gives us the true story
    if (required) {
      getReport().record(newError("does not exist", absolutePathError));
      return false;
    } // else - optional so no warnings/errors - just skip it so treat it as successful
    return true;
  }

  private boolean storeWhenFileExist() {
    if (isMigratable()) {
      recordEntry();
      try (final OutputStream os = getOutputStreamWithoutRecordingEntry()) {
        FileUtils.copyFile(file, os);
        return true;
      } catch (
          ExportIOException
              e) { // special case indicating the I/O error occurred while writing to the zip which
        // would invalidate the zip so we are forced to abort
        throw newError(ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, e.getCause());
      } catch (
          IOException
              e) { // here it means the error came out of reading/processing the input file/stream
        // where it is safe to continue with the next entry, so don't abort
        getReport().record(newError(ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, e));
      }
      return false;
    } // else - if it ain't migratable then only a warning occurs so return true
    return true;
  }

  private OutputStream getOutputStreamWithoutRecordingEntry() throws IOException {
    try {
      return outputStream.updateAndGet(os -> (os != null) ? os : context.getOutputStreamFor(this));
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private boolean isMigratable() {
    final ExportMigrationReportImpl report = context.getReport();

    if (path.isAbsolute()) {
      report.recordExternal(this, false);
      report.record(
          newWarning(String.format("is outside [%s]", context.getPathUtils().getDDFHome())));
      return false;
    } else if (Files.isSymbolicLink(absolutePath)) {
      report.recordExternal(this, true);
      report.record(newWarning("is a symbolic link"));
      return false;
    }
    return true;
  }

  // squid:S2093 - try-with-resource will throw IOException with InputStream and we do not care to
  // get that exception
  // squid:S2095 - stream is closed in finally clause
  @SuppressWarnings({"squid:S2093", "squid:S2095"})
  private String getJavaPropertyValue(String pname) throws IOException {
    final Properties props = new Properties();
    InputStream is = null;

    try {
      is = new BufferedInputStream(new FileInputStream(file));
      props.load(is);
    } finally {
      IOUtils.closeQuietly(is);
    }
    return props.getProperty(pname);
  }
}
