package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api;

import ddf.catalog.transform.QueryFilterTransformerProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;

import javax.xml.namespace.QName;
import java.util.List;

public interface Validator {
    /**
     * Verifies that that if types are passed, then they are fully qualified
     *
     * @param types List of QNames representing types
     */
    void validateFullyQualifiedTypes(List<QName> types) throws CswException;

    void validateOutputSchema(String schema, TransformerManager schemaTransformerManager)
            throws CswException;

    void validateVersion(String versions) throws CswException;

    void validateOutputFormat(String format, TransformerManager mimeTypeTransformerManager)
            throws CswException;

    void validateSchemaLanguage(String schemaLanguage) throws CswException;

    void setQueryFilterTransformerProvider(
            QueryFilterTransformerProvider queryFilterTransformerHelper);
}
