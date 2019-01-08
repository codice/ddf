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
package org.codice.ddf.catalog.ui.forms.commands;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

import com.google.common.collect.ImmutableList;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.commands.catalog.SubjectCommands;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
  scope = "forms",
  name = "manage",
  description = "Provides the capability to view and delete system templates."
)
public class SearchFormsManageCommand extends SubjectCommands implements Action {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsManageCommand.class);

  private final CatalogFramework catalogFramework;

  private final EndpointUtil endpointUtil;

  @Option(
    name = "--list",
    aliases = {"-l"},
    description = "List all the system forms"
  )
  protected boolean viewAll = false;

  @Option(
    name = "--remove-single",
    aliases = {"-s"},
    description = "Remove a single system template by metacard ID"
  )
  protected String singleDeletion = null;

  @Option(
    name = "--remove-all",
    aliases = {"-r"},
    description = "Removes all the system templates"
  )
  protected boolean massDeletion = false;

  public SearchFormsManageCommand() {
    this(get(CatalogFramework.class), get(EndpointUtil.class));
  }

  public SearchFormsManageCommand(CatalogFramework catalogFramework, EndpointUtil endpointUtil) {
    this.catalogFramework = catalogFramework;
    this.endpointUtil = endpointUtil;
  }

  @Override
  protected Object executeWithSubject() {

    if (viewAll) {
      printSystemTemplates();
      return null;
    }

    if (massDeletion || !isEmpty(singleDeletion)) {
      executeDeletion();
      return null;
    }

    return null;
  }

  private void printSystemTemplates() {
    for (Metacard mc : retrieveSystemMetacards()) {
      console.println(
          ansi()
              .fgBrightYellow()
              .a("Title: " + mc.getTitle())
              .newline()
              .fgGreen()
              .a("\t- " + mc.getId())
              .reset());
    }
  }

  private void executeDeletion() {

    List<Metacard> allSystemTemplates = retrieveSystemMetacards();
    if (allSystemTemplates.isEmpty()) {
      printColor(YELLOW, "No system forms exist.");
      return;
    }

    try {
      List<String> systemTemplateIds =
          singleDeletion != null
              ? ImmutableList.of(singleDeletion)
              : retrieveMetacardIds(allSystemTemplates);

      List<String> deletedMetacardIds = this.executeSystemTemplateRemoval(systemTemplateIds);

      if (deletedMetacardIds.isEmpty()) {
        printColor(YELLOW, "No system forms were deleted");
      } else {
        for (String id : deletedMetacardIds) {
          console.println(ansi().fgBrightYellow().a("Deleted: ").fgGreen().a(id).reset());
        }
      }
    } catch (IngestException | SourceUnavailableException e) {
      LOGGER.debug("Error removing system forms", e);
      printErrorMessage("Error removing system forms: " + e.getMessage());
    }
  }

  private List<String> executeSystemTemplateRemoval(List<String> metacardList)
      throws IngestException, SourceUnavailableException {

    LOGGER.debug("Preparing to remove the following system form metacards: {}", metacardList);

    DeleteResponse deleteResponse =
        catalogFramework.delete(new DeleteRequestImpl(metacardList.toArray(new String[0])));

    if (deleteResponse.getProcessingErrors() != null
        && !deleteResponse.getProcessingErrors().isEmpty()) {
      for (ProcessingDetails details : deleteResponse.getProcessingErrors()) {
        if (details.hasException()) {
          throw new IngestException(
              "Failed to delete list of system form metacards", details.getException());
        }
      }

      throw new IngestException("Failed to delete list of system form metacards");
    }

    List<String> deletedMetacardIds = retrieveMetacardIds(deleteResponse.getDeletedMetacards());

    LOGGER.debug("System forms deleted successfully {}", deletedMetacardIds);

    return deletedMetacardIds;
  }

  private List<String> retrieveMetacardIds(List<Metacard> metacards) {
    return metacards
        .stream()
        .map(Metacard::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private List<Metacard> retrieveSystemMetacards() {
    return endpointUtil.getMetacardListByTag(SYSTEM_TEMPLATE);
  }

  private static <T> T get(Class<T> type) {
    BundleContext context =
        FrameworkUtil.getBundle(SearchFormsManageCommand.class).getBundleContext();
    return context.getService(context.getServiceReference(type));
  }
}
