/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.commands.solr;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;


@Command(scope = SolrCommands.NAMESPACE, name = "backup", description = "Makes a backup of the selected Solr core.")
public class BackupCommand extends SolrCommands {

    @Option(name = "-d", aliases = {"--dir"}, multiValued = false, required = false,
            description = "Full path to location where backups will be written. If none specified, " +
            "backup will be written in the $data directory for the selected core.")
    String backupLocation;

    @Option(name = "-c", aliases = {"--coreName"}, multiValued = false, required = false,
            description = "Name of the Solr core to be backed up.  If not specified, the 'catalog' core will be backed up.")
    String coreName="catalog";

    @Option(name = "-n", aliases = {"--numToKeep"}, multiValued = false, required = false,
            description = "Number of backups to be maintained.  If this backup operation will result" +
                    " in exceeding this threshold, the oldest backup will be deleted.")
    int numberToKeep=0;

    @Override
    public Object doExecute() throws Exception {

        String backupUrl = getBackupUrl(coreName);

        URIBuilder uriBuilder = new URIBuilder(backupUrl);
        uriBuilder.addParameter("command", "backup");

        if (StringUtils.isNotBlank(backupLocation)) {
            uriBuilder.addParameter("location", backupLocation);
        }
        if (numberToKeep > 0) {
            uriBuilder.addParameter("numberToKeep", Integer.toString(numberToKeep));
        }

        LOGGER.debug("Sending request to {}", uriBuilder.build().toString());

        HttpClient httpClient = getHttpClient();
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        HttpResponse response = httpClient.execute(httpGet);

        processResponse(httpGet, response);
        return null;
    }

    private void processResponse(HttpGet httpGet, HttpResponse response) throws Exception {
        HttpEntity entity = response.getEntity();
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            StringBuilder body = new StringBuilder();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                String line;
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                } catch (Exception e) {
                    //do nothing
                } finally {
                    inputStream.close();
                }
            }
            printErrorMessage(String.format("Error backing up Solr core: [%s]", coreName));
            printErrorMessage(String.format("Backup command failed due to: %d - %s \n Request: %s",
                    response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase(), httpGet.getURI().toString()));
        } else {
            printSuccessMessage(String.format("\nBackup of [%s] complete.\n", coreName));
        }
    }

    private String getBackupUrl(String coreName) {

        String protocol = configurationMap.get("protocol");
        String host = configurationMap.get("host");
        String port = configurationMap.get("port");

        //catch this to match the default certs
        if (host.equals("0.0.0.0")) {
            host = "localhost";
        }
        if (StringUtils.isNotBlank(port)) {
            return protocol + host + ":" + port + "/solr/" + coreName + "/replication";
        }
        return protocol + host + "/solr/" + coreName + "/replication";
    }
}
