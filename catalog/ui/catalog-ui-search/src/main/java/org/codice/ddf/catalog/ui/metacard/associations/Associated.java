package org.codice.ddf.catalog.ui.metacard.associations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.codice.ddf.catalog.ui.util.EndpointUtil;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class Associated {
    private static final String ASSOCIATION_PREFIX = "metacard.associations.";

    private final CatalogFramework catalogFramework;

    private final EndpointUtil util;

    private final String metacardId;

    public Associated(CatalogFramework catalogFramework, EndpointUtil util, String metacardId) {
        this.catalogFramework = catalogFramework;
        this.util = util;
        this.metacardId = metacardId;
    }

    public List<AssociatedItem> getAssociations()
            throws SourceUnavailableException, UnsupportedQueryException, FederationException {
        return getAssociations(util.getMetacard(metacardId));
    }

    public List<AssociatedItem> putAssociations(List<AssociatedItem> associatedItems,
            List<String> emptyAssociations)
            throws SourceUnavailableException, UnsupportedQueryException, FederationException,
            IngestException {
        Metacard metacard = util.getMetacard(metacardId);
        Map<String, List<AssociatedItem>> associations = associatedItems.stream()
                .collect(Collectors.groupingBy(AssociatedItem::getType));

        List<Attribute> attributes = emptyAssociations.stream()
                .map(emptyAssociation -> new AttributeImpl(ASSOCIATION_PREFIX + emptyAssociation,
                        new String[0]))
                .collect(Collectors.toList());
        for (Map.Entry<String, List<AssociatedItem>> association : associations.entrySet()) {
            AttributeImpl updatedAttribute = new AttributeImpl(
                    ASSOCIATION_PREFIX + association.getKey(),
                    association.getValue()
                            .stream()
                            .map(AssociatedItem::getId)
                            .collect(Collectors.toList()));
            attributes.add(updatedAttribute);
        }

        attributes.forEach(metacard::setAttribute);
        UpdateResponse updateResponse = catalogFramework.update(new UpdateRequestImpl(metacardId,
                metacard));

        return associatedItems;
    }

    private List<AssociatedItem> getAssociations(Metacard metacard)
            throws SourceUnavailableException, UnsupportedQueryException, FederationException {
        Set<AttributeDescriptor> associationTypes = metacard.getMetacardType()
                .getAttributeDescriptors()
                .stream()
                .filter(ad -> ad.getName()
                        .startsWith(ASSOCIATION_PREFIX))
                .collect(Collectors.toSet());

        List<String> ids = util.getStringList(associationTypes.stream()
                .map(AttributeDescriptor::getName)
                .map(metacard::getAttribute)
                .filter(Objects::nonNull)
                .map(Attribute::getValues)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

        Map<String, Result> metacardMap = util.getMetacards(ids, "*");

        List<AssociatedItem> associatedItems = associationTypes.stream()
                .map(AttributeDescriptor::getName)
                .map(metacard::getAttribute)
                .filter(Objects::nonNull)
                .filter(at -> at.getValue() != null || at.getValues() != null)
                .flatMap(attr -> util.getStringList(metacard.getAttribute(attr.getName())
                        .getValues())
                        .stream()
                        .map(metacardMap::get)
                        .filter(Objects::nonNull)
                        .map(Result::getMetacard)
                        .map(m -> new AssociatedItem(attr.getName()
                                .substring(ASSOCIATION_PREFIX.length()), m)))
                .collect(Collectors.toList());
        return associatedItems;
    }

}
