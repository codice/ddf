package org.codice.ddf.ui.searchui.standard.endpoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class EndpointUtil {
    private final List<MetacardType> metacardTypes;
    private final CatalogFramework catalogFramework;
    private final FilterBuilder filterBuilder;

    public EndpointUtil(List<MetacardType> metacardTypes, CatalogFramework catalogFramework,
            FilterBuilder filterBuilder) {
        this.metacardTypes = metacardTypes;
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
    }

    public Metacard getMetacard( String id)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            StandardSearchException {
        Filter idFilter = filterBuilder.attribute(Metacard.ID)
                .is()
                .equalTo()
                .text(id);
        Filter tagsFilter = filterBuilder.attribute(Metacard.TAGS)
                .is()
                .like()
                .text("*");
        Filter filter = filterBuilder.allOf(idFilter, tagsFilter);

        QueryResponse queryResponse = catalogFramework.query(new QueryRequestImpl(new QueryImpl(
                filter), true));

        if (queryResponse.getHits() == 0) {
            throw new StandardSearchException("Could not find metacard for that metacard id");
        }

        Result result = queryResponse.getResults().get(0);
        return result.getMetacard();
    }

    public List<String> getStringList(List<Serializable> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getMetacardTypeMap() {
        Map<String, Object> resultTypes = new HashMap<>();
        for (MetacardType metacardType : metacardTypes) {
            List<Object> attributes = new ArrayList<>();
            for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
                Map<String, Object> attributeProperties = new HashMap<>();
                attributeProperties.put("type",
                        descriptor.getType()
                                .getAttributeFormat()
                                .name());
                attributeProperties.put("multivalued", descriptor.isMultiValued());
                attributeProperties.put("id", descriptor.getName());
                attributes.add(attributeProperties);
            }
            resultTypes.put(metacardType.getName(), attributes);
        }
        return resultTypes;
    }

    public Map<String, Object> transformToJson(Metacard metacard) {
        Set<AttributeDescriptor> attributeDescriptors = metacard.getMetacardType()
                .getAttributeDescriptors();
        Map<String, Object> result = new HashMap<>();
        for (AttributeDescriptor descriptor : attributeDescriptors) {
            if (metacard.getAttribute(descriptor.getName()) == null) {
                if (descriptor.isMultiValued()) {
                    result.put(descriptor.getName(), Collections.emptyList());
                } else {
                    result.put(descriptor.getName(), null);
                }
                continue;
            }
            if (Metacard.THUMBNAIL.equals(descriptor.getName())) {
                if (metacard.getThumbnail() != null) {
                    result.put(descriptor.getName(),
                            Base64.getEncoder()
                                    .encodeToString(metacard.getThumbnail()));
                } else {
                    result.put(descriptor.getName(), null);
                }
                continue;

            }
            if (descriptor.isMultiValued()) {
                result.put(descriptor.getName(),
                        metacard.getAttribute(descriptor.getName())
                                .getValues());
            } else {
                result.put(descriptor.getName(),
                        metacard.getAttribute(descriptor.getName())
                                .getValue());
            }
        }

        Map<String, Object> typeMap = new HashMap<>();
        typeMap.put("type",
                getMetacardTypeMap().get(metacard.getMetacardType()
                        .getName()));
        typeMap.put("type-name",
                metacard.getMetacardType()
                        .getName());
        typeMap.put("ids", Collections.singletonList(metacard.getId()));

        List<Object> typeList = new ArrayList<>();
        typeList.add(typeMap);

        Map<String, Object> outerMap = new HashMap<>();
        outerMap.put("metacards", Collections.singletonList(result));
        outerMap.put("metacard-types", typeList);

        return outerMap;
    }

    public Optional<MetacardType> getMetacardType(String name) {
        return metacardTypes.stream()
                .filter(mt -> mt.getName()
                        .equals(name))
                .findFirst();
    }
}
