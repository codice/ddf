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
package org.codice.ddf.persistence.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.gsonsupport.GsonTypeAdapters.PersistenceMapTypeAdapter;

@Service
@Command(
  scope = "store",
  name = "export",
  description = "Export entries that are available in the persistent store."
)
public class StoreExportCommand extends AbstractStoreCommand {

  @Option(
    name = "User ID",
    aliases = {"-u", "--user"},
    required = false,
    description =
        "User ID to search for the specified persistence type. If an id is not provided, then all of the specified persistence type for all users are displayed.",
    multiValued = false
  )
  String user;

  @Argument(
    name = "Dump directory path",
    description =
        "Directory to export into. Paths are absolute and must be in quotes.  Files in directory will be overwritten if they already exist.",
    index = 0,
    multiValued = false,
    required = true
  )
  String dirPath = null;

  private final Gson gson =
      new GsonBuilder().registerTypeAdapterFactory(PersistenceMapTypeAdapter.FACTORY).create();

  @Override
  public void storeCommand() throws PersistenceException {

    if (dirPath == null) {
      console.println("Export directory is not specified");
      return;
    }

    if (FilenameUtils.getExtension(dirPath).equals("") && !dirPath.endsWith(File.separator)) {
      dirPath += File.separator;
    }
    final File dumpDir = new File(dirPath);

    if (!dumpDir.exists()) {
      console.println(
          "Directory does not exist. If the directory does indeed exist, try putting the path in quotes.");
      return;
    }

    if (!dumpDir.isDirectory()) {
      console.println("Specified path is not a directory.");
      return;
    }

    cql = addUserConstraintToCql(user, cql);

    Function<List<Map<String, Object>>, Integer> exportFunction =
        results -> {
          return results
              .stream()
              .map(gson::toJson)
              .map(json -> writeRecordToFile(json, dumpDir))
              .reduce(0, (a, b) -> a + b);
        };

    long count = getResults(exportFunction);
    console.println("Exported: " + count + " records\n");
  }

  private int writeRecordToFile(String json, final File dumpDir) {
    String fileName = DigestUtils.md5Hex(json).toUpperCase();
    try (FileOutputStream outputStream = new FileOutputStream(new File(dumpDir, fileName))) {
      outputStream.write(json.getBytes("UTF-8"));
    } catch (IOException e) {
      console.println("Unable to write to:" + dumpDir);
      return 0;
    }
    console.println("Exporting : " + fileName);
    return 1;
  }
}
