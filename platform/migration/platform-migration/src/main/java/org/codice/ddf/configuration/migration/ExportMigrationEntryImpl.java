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

import ddf.security.common.audit.SecurityLogger;
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
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.BiThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class provides an implementation of the {@link ExportMigrationEntry}. */
public class ExportMigrationEntryImpl extends MigrationEntryImpl implements ExportMigrationEntry {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationEntryImpl.class);

  private static final String FAILED_TO_BE_EXPORTED = "failed to be exported";

  private static final String ERROR_EXPORTING_FILE = "Error exporting file {}";

  private final Map<String, ExportMigrationJavaPropertyReferencedEntryImpl> properties =
      new HashMap<>(8);

  private final AtomicReference<OutputStream> outputStream = new AtomicReference<>();

  private final ExportMigrationContextImpl context;

  private final Path absolutePath;

  private final Exception absolutePathError;

  private final Path path;

  private final File file;

  private final String name;

  private boolean isFile;

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
   * </code>
   */
  protected ExportMigrationEntryImpl(ExportMigrationContextImpl context, Path path) {
    Validate.notNull(context, "invalid null context");
    Validate.notNull(path, "invalid null path");
    Path apath;
    Exception aerror;

    try {
      // make sure it is resolved against ddf.home and not the current working directory
      apath =
          AccessUtils.doPrivileged(
              () -> {
                final Path p =
                    context
                        .getPathUtils()
                        .resolveAgainstDDFHome(path)
                        .toRealPath(LinkOption.NOFOLLOW_LINKS);

                this.isFile = p.toFile().isFile();
                return p;
              });
      aerror = null;
    } catch (IOException e) {
      apath = path;
      this.isFile =
          true; // since we can't find an absolute path on disk, we got to assume it's a file
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
   * </code>
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
  public boolean isDirectory() {
    return !isFile;
  }

  @Override
  public boolean isFile() {
    return isFile;
  }

  @Override
  public long getLastModifiedTime() {
    return file.lastModified();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    this.isFile =
        true; // force it to be represented as a file since the migratable will be providing the
    // data
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
        this.stored = handleStoreWhenNoSuchFile(required);
      } else if (absolutePathError != null) {
        SecurityLogger.audit(ExportMigrationEntryImpl.ERROR_EXPORTING_FILE, absolutePath);
        getReport().record(newError("cannot be read", absolutePathError));
      } else {
        this.stored = handleStoreWhenFileExist(null);
      }
    }
    return stored;
  }

  @Override
  public boolean store(boolean required, PathMatcher filter) {
    Validate.notNull(filter, "invalid null path filter");
    if (stored == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Exporting {}{} with path filter...", (required ? "required " : ""), toDebugString());
      }
      this.stored = false; // until proven otherwise
      if (isFile && !filter.matches(path)) {
        this.stored = handleStoreWhenFilterMismatch(required);
      } else if (absolutePathError instanceof NoSuchFileException) {
        this.stored = handleStoreWhenNoSuchFile(required);
      } else if (absolutePathError != null) {
        SecurityLogger.audit(ExportMigrationEntryImpl.ERROR_EXPORTING_FILE, absolutePath);
        getReport().record(newError("cannot be read", absolutePathError));
      } else {
        this.stored = handleStoreWhenFileExist(filter);
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
      } catch (ExportIOException e) {
        // special case indicating the I/O error occurred while writing to the zip which
        // would invalidate the zip so we are forced to abort
        throw newError(ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, e.getCause());
      } catch (IOException e) {
        // here it means the error came out of reading/processing the input file/stream
        // where it is safe to continue with the next entry, so don't abort
        getReport().record(newError(ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, e));
      } catch (MigrationException e) {
        throw e;
      }
    }
    return stored;
  }

  @Override
  public Optional<ExportMigrationEntry> getPropertyReferencedEntry(String name) {
    return AccessUtils.doPrivileged(() -> getPropertyReferencedEntry(name, (r, n) -> true));
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

  protected MigrationException newError(String reason, Object cause) {
    return new MigrationException(Messages.EXPORT_PATH_ERROR, path, reason, cause);
  }

  private boolean handleStoreWhenFilterMismatch(boolean required) {
    // we have a filter that doesn't match this entry and the entry represents
    // a file, so treat it as if the file was not there to start with
    if (required) {
      getReport().record(newError("was not copied", "it does not match filter"));
      return false;
    } // else - optional so no warnings/errors - just skip it and treat it as successful
    return true;
  }

  private boolean handleStoreWhenNoSuchFile(boolean required) {
    // we cannot rely on file.exists() here since the path is not valid anyway
    // relying on the exception is much safer and gives us the true story
    if (required) {
      getReport().record(newError("does not exist", absolutePathError));
      return false;
    } // else - optional so no warnings/errors - just skip it so treat it as successful
    return true;
  }

  private boolean handleStoreWhenFileExist(@Nullable PathMatcher filter) {
    return AccessUtils.doPrivileged(
        () -> {
          if (isMigratable()) {
            recordEntry();
            return isFile ? handleStoreFile() : handleStoreDirectory(filter);
          } // else - if it ain't migratable then only a warning occurs so return true
          return true;
        });
  }

  private boolean handleStoreFile() {
    try (final OutputStream os = getOutputStreamWithoutRecordingEntry()) {
      context.getReport().recordFile(this);
      FileUtils.copyFile(file, os);
      SecurityLogger.audit("Exported file {}", absolutePath);
      return true;
    } catch (ExportIOException e) {
      // special case indicating the I/O error occurred while writing to the zip which
      // would invalidate the zip so we are forced to abort
      SecurityLogger.audit(ExportMigrationEntryImpl.ERROR_EXPORTING_FILE, absolutePath);
      throw newError(ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, e.getCause());
    } catch (IOException e) {
      // here it means the error came out of reading/processing the input file/stream
      // where it is safe to continue with the next entry, so don't abort
      SecurityLogger.audit(ExportMigrationEntryImpl.ERROR_EXPORTING_FILE, absolutePath);
      getReport().record(newError(ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, e));
    }
    return false;
  }

  private boolean handleStoreDirectory(@Nullable PathMatcher filter) {
    final Set<String> files = new HashSet<>();
    // all files underneath the directory are optional so pass false to store()
    final boolean dirStored =
        ((filter != null) ? context.entries(path, filter) : context.entries(path))
            .peek(e -> files.add(e.getName()))
            .map(e -> e.store(false))
            .reduce(true, Boolean::logicalAnd);

    if (dirStored) {
      context.getReport().recordDirectory(this, filter, files);
      SecurityLogger.audit("Exported directory {}", absolutePath);
    } else {
      SecurityLogger.audit("Error exporting directory {}", absolutePath);
      getReport()
          .record(
              newError(
                  ExportMigrationEntryImpl.FAILED_TO_BE_EXPORTED, "some directory entries failed"));
    }
    return dirStored;
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

  @SuppressWarnings({ //
    "squid:S2093", /* try-with-resource will throw IOException with InputStream and we do not care to get that exception */
    "squid:S2095" /* stream is closed in finally clause */
  })
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

  /**
   * The superclass implementation is sufficient for our needs.
   *
   * @param o the object to check
   * @return true if equal
   */
  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  /**
   * The superclass implementation is sufficient for our needs.
   *
   * @return the hashcode
   */
  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
