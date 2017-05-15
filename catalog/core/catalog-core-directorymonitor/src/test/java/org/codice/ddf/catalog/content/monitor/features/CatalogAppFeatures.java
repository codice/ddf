/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.monitor.features;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.util.Arrays;

import org.ops4j.pax.exam.Option;

public class CatalogAppFeatures {
    public enum CatalogFeature {
        CATALOG_CORE_API("catalog-core-api"), CATALOG_CORE("catalog-core"), CATALOG_CORE_CONTENT(
                "catalog-core-content"), CATALOG_CORE_DIRECTORYMONITOR(
                "catalog-core-directorymonitor"), CATALOG_CORE_MIGRATABLE("catalog-core-migratable"), CATALOG_CORE_DOWNLOADACTION(
                "catalog-core-downloadaction"), CATALOG_METRICS("catalog-metrics"), CATALOG_CORE_BACKUPPLUGIN(
                "catalog-core-backupplugin"), CATALOG_PLUGIN_EXPIRATIONDATE(
                "catalog-plugin-expirationdate"), CATALOG_SCHEMATRON_PLUGIN(
                "catalog-schematron-plugin"), CATALOG_REST("catalog-rest"), CATALOG_REST_ENDPOINT(
                "catalog-rest-endpoint"), CATALOG_OPENSEARCH_ENDPOINT("catalog-opensearch-endpoint"), CATALOG_OPENSEARCH_SOURCE(
                "catalog-opensearch-source"), CATALOG_CONFLUENCE_SOURCE("catalog-confluence-source"), ABDERA(
                "abdera"), CATALOG_PLUGIN_FEDERATIONREPLICATION(
                "catalog-plugin-federationreplication"), CATALOG_PLUGIN_JPEG2000(
                "catalog-plugin-jpeg2000"), CATALOG_TRANSFORMER_METADATA(
                "catalog-transformer-metadata"), CATALOG_TRANSFORMER_THUMBNAIL(
                "catalog-transformer-thumbnail"), CATALOG_TRANSFORMER_XSLTENGINE(
                "catalog-transformer-xsltengine"), CATALOG_TRANSFORMER_RESOURCE(
                "catalog-transformer-resource"), CATALOG_TRANSFORMER_TIKA("catalog-transformer-tika"), CATALOG_TRANSFORMER_VIDEO(
                "catalog-transformer-video"), CATALOG_TRANSFORMER_JSON("catalog-transformer-json"), CATALOG_TRANSFORMER_ATOM(
                "catalog-transformer-atom"), CATALOG_TRANSFORMER_GEOFORMATTER(
                "catalog-transformer-geoformatter"), CATALOG_TRANSFORMER_XML(
                "catalog-transformer-xml"), CATALOG_TRANSFORMER_ZIP("catalog-transformer-zip"), CATALOG_SECURITY_FILTER(
                "catalog-security-filter"), CATALOG_SECURITY_OPERATIONPLUGIN(
                "catalog-security-operationplugin"), CATALOG_SECURITY_PLUGIN(
                "catalog-security-plugin"), CATALOG_SECURITY_RESOURCEPLUGIN(
                "catalog-security-resourceplugin"), CATALOG_SECURITY_POLICYPLUGIN(
                "catalog-security-policyplugin"), CATALOG_SECURITY_POINTOFCONTACT_READONLY(
                "catalog-security-pointofcontact-readonly"), CATALOG_SECURITY_METACARDATTRIBUTEPLUGIN(
                "catalog-security-metacardattributeplugin"), CATALOG_SECURITY_XMLATTRIBUTEPLUGIN(
                "catalog-security-xmlattributeplugin"), CATALOG_ADMIN_MODULE_SOURCES(
                "catalog-admin-module-sources"), ADMIN_POLLER_SERVICE_BEAN(
                "admin-poller-service-bean"), CATALOG_TRANSFORMER_STREAMING(
                "catalog-transformer-streaming"), CATALOG_PLUGIN_METACARD_VALIDATION(
                "catalog-plugin-metacard-validation"), CATALOG_CORE_VALIDATOR(
                "catalog-core-validator"), CATALOG_CORE_VALIDATIONPARSER(
                "catalog-core-validationparser"), CATALOG_TRANSFORMER_PDF("catalog-transformer-pdf"), CATALOG_VERSIONING_PLUGIN(
                "catalog-versioning-plugin"), CATALOG_TRANSFORMER_PPTX("catalog-transformer-pptx"), CATALOG_VALIDATOR_METACARDDUPLICATION(
                "catalog-validator-metacardduplication"), CATALOG_TRANSFORMER_OVERLAY(
                "catalog-transformer-overlay"), CATALOG_APP("catalog-app"), CATALOG_CLIENT_INFO(
                "catalog-client-info"), CATALOG_METACARDINGEST_NETWORK(
                "catalog-metacardingest-network"), CATALOG_METACARD_BACKUP_STORAGE_API(
                "catalog-metacard-backup-storage-api"), CATALOG_METACARD_BACKUP(
                "catalog-metacard-backup"), CATALOG_METACARD_BACKUP_FILESTORAGE(
                "catalog-metacard-backup-filestorage"), CATALOG_FTP("catalog-ftp"), CATALOG_ASYNC_INMEMORY(
                "catalog-async-inmemory");

        private String featureName;

        CatalogFeature(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public String toString() {
            return featureName;
        }
    }

    public static Option catalogAppFeatures(CatalogFeature... features) {
        String[] featureStrings = Arrays.stream(features)
                .map(Enum::toString)
                .toArray(String[]::new);

        return features(maven().groupId("ddf.catalog")
                .artifactId("catalog-app")
                .versionAsInProject()
                .classifier("features")
                .type("xml"), featureStrings);
    }
}
