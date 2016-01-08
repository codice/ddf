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
package org.codice.ddf.security.claims.attributequery;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import ddf.security.PropertiesLoader;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.samlp.SimpleSign;

public class AttributeQueryClaimsHandler implements ClaimsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeQueryClaimsHandler.class);

    protected static final String ERROR_RETRIEVING_ATTRIBUTES =
            "Error retrieving attributes from external attribute store [{}] for DN [{}]. ";

    protected static final String ERROR_RETRIEVING_ATTRIBUTES_SEC_LOG =
            "Error retrieving attributes from external attribute store [%s] for DN [%s]. ";

    private String attributeMapLocation;

    private List<String> supportedClaims;

    private Map<String, String> attributeMap;

    protected SimpleSign simpleSign;

    protected String externalAttributeStoreUrl;

    protected String issuer;

    protected String destination;

    public AttributeQueryClaimsHandler() {
        LOGGER.debug("Starting AttributeQueryClaimsHandler");
    }

    /**
     * Gets the supported claim types.
     *
     * @return List of supported claims.
     */
    @Override
    public List<URI> getSupportedClaimTypes() {
        LOGGER.debug("Getting supported claim types.");
        List<URI> supportedClaimTypes = new ArrayList<>();
        try {
            for (String claim : supportedClaims) {
                supportedClaimTypes.add(new URI(claim));
            }
        } catch (URISyntaxException e) {
            LOGGER.warn("Not a valid URI for claim type {}.", e);
        }
        return supportedClaimTypes;
    }

    /**
     * Retrieves claims from the external attribute store.
     *
     * @param claims The collection of claims.
     * @return The collection of claims or an empty collection if there are no security claims.
     * @throws URISyntaxException
     */
    @Override
    public ProcessedClaimCollection retrieveClaimValues(ClaimCollection claims,
            ClaimsParameters parameters) {
        Principal principal = parameters.getPrincipal();

        String nameId = getNameId(principal);

        ProcessedClaimCollection claimCollection = new ProcessedClaimCollection();
        try {
            if (!StringUtils.isEmpty(nameId)) {
                ProcessedClaimCollection securityClaimCollection = getAttributes(nameId);
                // If security claim collection came back empty, return an empty claim collection.
                if (!CollectionUtils.isEmpty(securityClaimCollection)) {
                    claimCollection.addAll(securityClaimCollection);
                }
            }
        } catch (URISyntaxException e) {
            LOGGER.error(ERROR_RETRIEVING_ATTRIBUTES, externalAttributeStoreUrl, nameId, e);
            SecurityLogger.logError(String.format(ERROR_RETRIEVING_ATTRIBUTES_SEC_LOG,
                    externalAttributeStoreUrl,
                    nameId), e);
        }
        return claimCollection;
    }

    /**
     * Retrieve the name from the principal.
     *
     * @param principal of the user.
     * @return CN of the user.
     */
    protected String getNameId(Principal principal) {
        return principal.getName();
    }

    /**
     * Gets the attributes for the supplied user from the external attribute store.
     *
     * @param nameId used for the request.
     * @return The collection of attributes retrieved from the external attribute store.
     * @throws URISyntaxException
     */
    protected ProcessedClaimCollection getAttributes(String nameId) throws URISyntaxException {
        ProcessedClaimCollection claimCollection = new ProcessedClaimCollection();

        LOGGER.debug("Sending AttributeQuery Request.");

        Assertion assertion;
        try {
            assertion = createAttributeQueryClient(simpleSign,
                    externalAttributeStoreUrl,
                    issuer,
                    destination).retrieveResponse(nameId);

            if (assertion != null) {
                createClaims(claimCollection, assertion);
            }
        } catch (AttributeQueryException ex) {
            LOGGER.error("Error occurred in AttributeQueryClient, did not retrieve response.", ex);
        }

        return claimCollection;
    }

    /**
     * Creates claims from the extracted attributes.
     *
     * @param claimsCollection The collection of claims.
     * @param assertion        Assertion from the response.
     * @return The collection of claims.
     * @throws URISyntaxException
     */
    protected ProcessedClaimCollection createClaims(ProcessedClaimCollection claimsCollection,
            Assertion assertion) throws URISyntaxException {

        // Should only contain one Attribute Statement.
        AttributeStatement attributeStatement = assertion.getAttributeStatements()
                .get(0);
        List<Attribute> attributeList = attributeStatement.getAttributes();

        // If the claim is supported, check if it has an attribute mapping.
        // If so, map the attribute (attribute value -> mapped attribute value)
        // and create the claim, otherwise, create the claim using its original attribute value.
        for (Attribute attribute : attributeList) {
            for (String claimType : supportedClaims) {
                if (claimType.equalsIgnoreCase(attribute.getName())) {
                    String claimValue = attribute.getDOM()
                            .getTextContent();
                    if (attributeMap.containsKey(claimValue)) {
                        claimsCollection.add(createSingleValuedClaim(claimType, attributeMap.get(
                                claimValue)));
                    } else {
                        claimsCollection.add(createSingleValuedClaim(claimType, claimValue));
                    }
                    break;
                }
            }
        }
        return claimsCollection;
    }

    /**
     * Creates a single valued claim.
     *
     * @param claimType  The claim type.
     * @param claimValue The claim value.
     * @return The claim.
     * @throws URISyntaxException
     */
    protected ProcessedClaim createSingleValuedClaim(String claimType, String claimValue)
            throws URISyntaxException {
        ProcessedClaim claim = new ProcessedClaim();

        claim.setClaimType(new URI(claimType));
        claim.setValues(ImmutableList.of(claimValue));

        LOGGER.debug("Created claim with type [{}] and value [{}].", claimType, claimValue);

        return claim;
    }

    /**
     * Creates a client for sending an AttributeQuery
     * request to a specified external attribute store.
     *
     * @param simpleSign                to create signature for request
     * @param externalAttributeStoreUrl endpoint of external web service
     * @param issuer                    of request
     * @param destination               of request
     * @return AttributeQueryClient
     */
    protected AttributeQueryClient createAttributeQueryClient(SimpleSign simpleSign,
            String externalAttributeStoreUrl, String issuer, String destination) {
        return new AttributeQueryClient(simpleSign, externalAttributeStoreUrl, issuer, destination);
    }

    public void setSimpleSign(SimpleSign simpleSign) {
        this.simpleSign = simpleSign;
    }

    public void setExternalAttributeStoreUrl(String externalAttributeStoreUrl) {
        this.externalAttributeStoreUrl = externalAttributeStoreUrl;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setAttributeMapLocation(String attributeMapLocation) {
        if (StringUtils.isNotBlank(attributeMapLocation)
                && !attributeMapLocation.equals(this.attributeMapLocation)) {
            attributeMap = PropertiesLoader.toMap(PropertiesLoader.loadProperties(
                    attributeMapLocation));
            this.attributeMapLocation = attributeMapLocation;
        }
    }

    public void setSupportedClaims(List<String> supportedClaims) {
        this.supportedClaims = supportedClaims;
    }
}