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

/**
 * Provides a class for defining messages or message formats for migration exceptions, warnings and
 * informational messages.
 */
public final class Messages {

  public static final String MAKE_SURE_IT_EXISTS =
      "Make sure it exists on the system you're migrating to or update the path and export again.";

  public static final String EXPORT_FAILURE = "Failed to export to file [%s].";

  public static final String IMPORT_FAILURE = "Failed to import from file [%s].";

  public static final String DECRYPT_FAILURE = "Failed to decrypt file [%s].";

  public static final String EXPORT_SUCCESS_WITH_WARNINGS =
      "Successfully exported to file [%s] with warnings; make sure to review.";

  public static final String IMPORT_SUCCESS_WITH_WARNINGS =
      "Successfully imported from file [%s] with warnings; make sure to review.";

  public static final String DECRYPT_SUCCESS_WITH_WARNINGS =
      "Successfully decrypted file [%s] to [%s] with warnings; make sure to review.";

  public static final String RESTART_SYSTEM_WHEN_WARNINGS =
      "Please restart the system for changes to take effect after addressing all reported warnings.";

  public static final String EXPORT_SUCCESS = "Successfully exported to file [%s].";

  public static final String IMPORT_SUCCESS = "Successfully imported from file [%s].";

  public static final String DECRYPT_SUCCESS = "Successfully decrypted file [%s] to [%s].";

  public static final String RESTARTING_SYSTEM =
      "Restarting the system for changes to take effect.";

  public static final String RESTART_SYSTEM =
      "Please restart the system for changes to take effect.";

  public static final String DIRECTORY_CREATE_ERROR =
      "Unexpected error: unable to create directory [%s]; %s.";

  public static final String EXPORT_FILE_CLOSE_ERROR =
      "Export error: failed to close export file [%s]; %s.";

  public static final String DECRYPT_FILE_CLOSE_ERROR =
      "Decrypt error: failed to close decrypted file [%s]; %s.";

  public static final String IMPORT_FILE_MISSING_ERROR =
      "Import error: missing export file [%s]; %s.";

  public static final String IMPORT_FILE_MULTIPLE_ERROR =
      "Import error: more than one export file [%s].";

  public static final String IMPORT_FILE_OPEN_ERROR =
      "Import error: failed to open export file [%s]; %s.";

  public static final String EXPORT_FILE_CREATE_ERROR =
      "Export error: failed to create export file [%s]; %s.";

  public static final String DECRYPT_FILE_CREATE_ERROR =
      "Decrypt error: failed to create decrypted file [%s]; %s.";

  public static final String IMPORT_FILE_READ_ERROR =
      "Import error: failed to read export file [%s]; %s.";

  public static final String IMPORT_UNSUPPORTED_VERSION_ERROR =
      "Import error: unsupported exported version [%s]; currently supporting [%s].";

  public static final String IMPORT_MISMATCH_PRODUCT_ERROR =
      "Import error: mismatched product [%s]; expecting [%s].";

  public static final String IMPORT_UNSUPPORTED_PRODUCT_VERSION_ERROR =
      "Import error: unsupported product version [%s]; currently supporting %s.";

  public static final String IMPORT_MISMATCH_DDF_HOME_ERROR =
      "Import error: mismatched ddf.home [%s]; expecting local system to be installed under [%s].";

  public static final String IMPORT_ZIP_CHECKSUM_INVALID =
      "Import error: incorrect checksum for export file [%s].";

  public static final String DECRYPT_ZIP_CHECKSUM_INVALID =
      "Decrypt error: incorrect checksum for export file [%s].";

  public static final String EXPORT_METADATA_CREATE_ERROR =
      "Export error: failed to create metadata; %s.";

  public static final String IMPORT_METADATA_MISSING_ERROR = "Import error: missing metadata.";

  public static final String IMPORT_METADATA_FORMAT_ERROR = "Import error: invalid metadata; %s.";

  public static final String EXPORT_INTERNAL_ERROR =
      "Unexpected internal error: failed to export to file [%s]; %s.";

  public static final String IMPORT_INTERNAL_ERROR =
      "Unexpected internal error: failed to import from file [%s]; %s.";

  public static final String DECRYPT_INTERNAL_ERROR =
      "Unexpected internal error: failed to decrypt file [%s]; %s.";

  public static final String EXPORT_SECURITY_ERROR =
      "Export security error: failed to export to file [%s]; %s.";

  public static final String IMPORT_SECURITY_ERROR =
      "Import security error: failed to import from file [%s]; %s.";

  public static final String DECRYPT_SECURITY_ERROR =
      "Decrypt security error: failed to decrypt file [%s]; %s.";

  public static final String EXPORTING_DATA = "Exporting %s data to file [%s].";

  public static final String IMPORTING_DATA = "Importing %s data from file [%s].";

  public static final String DECRYPTING_DATA = "Decrypting %s data from file [%s] to [%s].";

  public static final String EXPORT_SYSTEM_PROPERTY_ERROR =
      "Export error: system property [%s] is set to [%s] that %s; %s.";

  public static final String IMPORT_SYSTEM_PROPERTY_ERROR =
      "Import error: system property [%s] which was originally set to [%s] %s.";

  public static final String EXPORT_JAVA_PROPERTY_ERROR =
      "Export error: Java property [%s] in file [%s] is set to [%s] that %s; %s.";

  public static final String IMPORT_JAVA_PROPERTY_ERROR =
      "Import error: Java property [%s] in file [%s] which was originally set to [%s] %s.";

  public static final String EXPORT_SYSTEM_PROPERTY_NOT_DEFINED_ERROR =
      "Export error: system property [%s] is not defined.";

  public static final String IMPORT_SYSTEM_PROPERTY_NOT_DEFINED_ERROR =
      "Import error: system property [%s] which was originally set to [%s] is no longer defined.";

  public static final String EXPORT_JAVA_PROPERTY_NOT_DEFINED_ERROR =
      "Export error: Java property [%s] from file [%s] is not defined.";

  public static final String IMPORT_JAVA_PROPERTY_NOT_DEFINED_ERROR =
      "Import error: Java property [%s] from file [%s] which was originally set to [%s] is no longer defined.";

  public static final String EXPORT_SYSTEM_PROPERTY_IS_EMPTY_ERROR =
      "Export error: system property [%s] is empty or blank.";

  public static final String IMPORT_SYSTEM_PROPERTY_IS_EMPTY_ERROR =
      "Import error: system property [%s] which was originally set to [%s] is now empty or blank.";

  public static final String EXPORT_JAVA_PROPERTY_IS_EMPTY_ERROR =
      "Export error: Java property [%s] from file [%s] is empty or blank.";

  public static final String IMPORT_JAVA_PROPERTY_IS_EMPTY_ERROR =
      "Import error: Java property [%s] from file [%s] which was originally set to [%s] is now empty or blank.";

  public static final String EXPORT_PATH_DOES_NOT_EXIST_ERROR =
      "Export error: path [%s] does not exist.";

  public static final String EXPORT_PATH_NOT_A_DIRECTORY_ERROR =
      "Export error: path [%s] is not a directory.";

  public static final String EXPORT_JAVA_PROPERTY_LOAD_ERROR =
      "Export error: Java property [%s] from file [%s] could not be retrieved; %s.";

  public static final String IMPORT_JAVA_PROPERTY_LOAD_ERROR =
      "Import error: Java property [%s] from file [%s] which was originally set to [%s] could not be retrieved; %s.";

  public static final String IMPORT_PATH_WARNING =
      "Path [%s] %s. Make sure it matches the file from the original system.";

  public static final String IMPORT_OPTIONAL_PATH_DELETE_WARNING =
      "Optional path [%s], which was not exported, %s; therefore, it cannot be deleted. "
          + "Manually clean it if it is no longer required.";

  public static final String IMPORT_PATH_DELETE_WARNING =
      "Path [%s], which did not exist on original system, could not be deleted. "
          + "Manually clean it if it is no longer required.";

  public static final String EXPORT_PATH_ERROR = "Export error: path [%s] %s; %s.";

  public static final String IMPORT_PATH_ERROR = "Import error: path [%s] %s.";

  public static final String DECRYPT_PATH_ERROR =
      "Decrypt error: path [%s] could not be decrypted; %s.";

  public static final String IMPORT_PATH_NOT_EXPORTED_ERROR =
      "Import error: path [%s] was not exported.";

  public static final String IMPORT_OPTIONAL_PATH_DELETE_ERROR =
      "Import error: optional path [%s], which was not exported, could not be deleted.";

  public static final String IMPORT_PATH_COPY_ERROR =
      "Import error: path [%s] could not be copied to [%s]; %s.";

  public static final String EXPORT_CHECKSUM_COMPUTE_WARNING =
      "Failed to compute checksum for file [%s] (%s); therefore, it will not be possible to verify the exitence of the file on the destination system.  "
          + "Make sure the same file exist on the system you're migrating to.";

  public static final String IMPORT_CHECKSUM_MISMATCH_WARNING =
      "Checksum for file [%s] doesn't match the one from the original system.  "
          + "Make sure the file matches the one from the system you migrated from.";

  public static final String IMPORT_CHECKSUM_COMPUTE_WARNING =
      "Failed to compute checksum for file [%s] (%s); therefore, it will not be possible to verify the integrity of the file on the local system.  "
          + "Make sure the file matches the one from the system you migrated from.";

  public static final String IMPORT_UNKNOWN_DATA_FOUND_ERROR =
      "Import error: unknown data found in exported file.";

  public static final String EXPORT_PATH_WARNING =
      "Path [%s] %s; therefore, it will not be included in the export. "
          + Messages.MAKE_SURE_IT_EXISTS;

  public static final String EXPORT_SYSTEM_PROPERTY_WARNING =
      "System property [%s] is set to path [%s] that %s; therefore, it will not be included in the export. "
          + Messages.MAKE_SURE_IT_EXISTS;

  public static final String EXPORT_JAVA_PROPERTY_WARNING =
      "Java property [%s] in file [%s] is set to path [%s] that %s; therefore, it will not be included in the export. "
          + Messages.MAKE_SURE_IT_EXISTS;

  private Messages() {}
}
