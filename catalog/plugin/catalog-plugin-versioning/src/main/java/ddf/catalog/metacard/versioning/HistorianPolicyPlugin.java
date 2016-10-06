/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * </p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.metacard.versioning;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import ddf.catalog.core.versioning.DeletedMetacard;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;

/**
 * HistorianPolicyPlugin prevents anyone without the {@link HistorianPolicyPlugin#HISTORY_ROLE}
 * from modifying a {@link MetacardVersion} in any way.
 */
public class HistorianPolicyPlugin implements PolicyPlugin {

    public static final String HISTORY_ROLE = "system-history";

    public static final String ROLE_CLAIM =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private final Predicate<Metacard> isMetacardHistory =
            (tags) -> tags != null && tags.getTags() != null && tags.getTags()
                    .contains(MetacardVersion.VERSION_TAG);

    private final Predicate<Metacard> isDeletedMetacard =
            (tags) -> tags != null && tags.getTags() != null && tags.getTags()
                    .contains(DeletedMetacard.DELETED_TAG);

    private final Predicate<Metacard> isHistoryOrDeleted = isMetacardHistory.or(isDeletedMetacard);

    @Override
    public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        if (!isHistoryOrDeleted.test(input)) {
            /* not modifying history, proceed */
            return new PolicyResponseImpl();
        }
        return new PolicyResponseImpl(new HashMap<>(),
                Collections.singletonMap(ROLE_CLAIM, Collections.singleton(HISTORY_ROLE)));

    }

    @Override
    public PolicyResponse processPreUpdate(Metacard newMetacard,
            Map<String, Serializable> properties) throws StopProcessingException {
        if (!isHistoryOrDeleted.test(newMetacard)) {
            /* not modifying history, proceed */
            return new PolicyResponseImpl();
        }
        return new PolicyResponseImpl(new HashMap<>(),
                Collections.singletonMap(ROLE_CLAIM, Collections.singleton(HISTORY_ROLE)));
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {
        if (!metacards.stream()
                .anyMatch(isHistoryOrDeleted)) {
            /* not modifying history, proceed */
            return new PolicyResponseImpl();
        }
        return new PolicyResponseImpl(new HashMap<>(),
                Collections.singletonMap(ROLE_CLAIM, Collections.singleton(HISTORY_ROLE)));
    }

    @Override
    public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreResource(ResourceRequest resourceRequest)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }
}
