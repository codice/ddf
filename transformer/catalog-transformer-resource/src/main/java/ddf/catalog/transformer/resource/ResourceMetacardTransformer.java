/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.transformer.resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceRequestById;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

/**
 * 
 * This transformer uses the Catalog Framework to obtain and return
 * the resource based on the metacard id.
 * 
 * @author Tim Anderson
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 *
 */
public class ResourceMetacardTransformer implements MetacardTransformer {

   private static final Logger LOGGER = Logger
         .getLogger(ResourceMetacardTransformer.class);

   private CatalogFramework catalogFramework;
   
   private static final String DEFAULT_MIME_TYPE_STR = "application/octet-stream";

    /**
     * Construct instance with required framework to resolve the resource
     * 
     * @param framework
     */
   public ResourceMetacardTransformer(CatalogFramework framework) {
      LOGGER.debug("constructing resource metacard transformer");
      this.catalogFramework = framework;
   }

   @Override
   public BinaryContent transform(Metacard metacard,
         Map<String, Serializable> arguments)
         throws CatalogTransformerException {

      LOGGER.trace("Entering resource ResourceMetacardTransformer.transform");
      
      if ( ! isValid( metacard )) {
         throw new CatalogTransformerException( "Could not transform metacard to a resource because the metacard is not valid.");
      }
      
      String id = metacard.getId();

      if (LOGGER.isDebugEnabled()) {
         LOGGER.debug("executing resource request with id '" + id + "'");
      }
      final ResourceRequest resourceRequest = new ResourceRequestById(id, arguments);
      
      ResourceResponse resourceResponse = null;

      String sourceName = metacard.getSourceId();
      
      if(StringUtils.isBlank(sourceName)){
          sourceName = catalogFramework.getId();
      }
      
      try {
         resourceResponse = catalogFramework.getResource(resourceRequest,
                 sourceName);
      } catch (IOException e) {
         throw new CatalogTransformerException(
               "Unable to retrieve resource for the requested metacard with id: '" + id + "'.", e);
      } catch (ResourceNotFoundException e) {
         throw new CatalogTransformerException(
               "Unable to retrieve resource for the requested metacard with id: '" + id + "'.", e);
      } catch (ResourceNotSupportedException e) {
         throw new CatalogTransformerException(
               "Unable to retrieve resource for the requested metacard with id: '" + id + "'.", e);
      }

      if (resourceResponse == null) {
         throw new CatalogTransformerException(
               "Resource response is null: Unable to retrieve the product for the metacard with id: '" + id + "'.");
      }

      Resource transformedContent = resourceResponse.getResource();
      MimeType mimeType = transformedContent.getMimeType();

      if (mimeType == null) {
         try {
            mimeType = new MimeType(DEFAULT_MIME_TYPE_STR);
            // There is no method to set the MIME type, so in order to set it to our default one, we need to create a new object.
            transformedContent = new ResourceImpl(transformedContent.getInputStream(),
                    mimeType, transformedContent.getName());
         } catch (MimeTypeParseException e) {
            throw new CatalogTransformerException( "Could not create default mime type upon null mimeType, for default mime type '" + DEFAULT_MIME_TYPE_STR + "'.", e);
         }
      }
      if ( LOGGER.isDebugEnabled() ) {
         LOGGER.debug("Found mime type: '" + mimeType.toString() + "'" + 
                   " for product of metacard with id: '" + id + "'." +
                   "\nGetting associated resource from input stream. \n");
      }

      if ( LOGGER.isTraceEnabled() ) {
         LOGGER.trace("Exiting resource transform for metacard id: '"
            + id + "'");
      }
      return transformedContent;
   }
   
   /**
    * Checks to see whether the given metacard is valid.  If it is not valid,
    * it will return false, otherwise true.
    * 
    * @param metacard The metacard to be validated.
    * @return boolean indicating valid.
    */
   private boolean isValid(Metacard metacard ) {
      boolean valid = true;
      if ( metacard == null ) {
         LOGGER.warn("Metacard cannot be null");
         return false;
      }
      if ( metacard.getId() == null ) {
         LOGGER.warn("Metacard id cannot be null");
         return false;
      }
     return valid;
   }

}
