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
package ddf.catalog.federation.layered.replication;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

public class RestReplicatorPlugin implements PostIngestPlugin {

	/**
	 * A configurable property of parent's location.
	 */
	private String parentAddress = null;

	private MetacardTransformer transformer = null;

	private static final Logger LOGGER = Logger.getLogger(RestReplicatorPlugin.class);
	private WebClient client;

	public RestReplicatorPlugin(String endpointAddress) {
		setParentAddress(endpointAddress);
	}

	@Override
	public CreateResponse process(CreateResponse input) throws PluginExecutionException {

		if (client != null && transformer != null) {

			for (Metacard m : input.getCreatedMetacards()) {

				String data = transform(m, client);

				Response r = client.post(data);
				
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Posted the following GeoJSON: \n" + data );
					LOGGER.debug("RESPONSE: [" + ToStringBuilder.reflectionToString(r) + "]");
				}
			}
		}

		return input;
	}

	@Override
	public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {

		if (client != null && transformer != null) {

			WebClient updateClient = WebClient.fromClient(client);

			updateClient.type(MediaType.APPLICATION_JSON);

			List<Update> updates = input.getUpdatedMetacards();

			if (updates == null) {
				return input;
			}

			UpdateRequest request = input.getRequest();
			if (request != null && !Metacard.ID.equals(request.getAttributeName())) {
				throw new PluginExecutionException(new UnsupportedOperationException(
						"Cannot replicate records that are not updated by " + Metacard.ID));
			}

			for (int i = 0; i < updates.size(); i++) {

				Update update = updates.get(i);

				if (request != null && request.getUpdates() != null && request.getUpdates().get(i) != null
						&& request.getUpdates().get(i).getKey() != null) {

					updateClient.path(request.getUpdates().get(i).getKey());

					Metacard newMetacard = update.getNewMetacard();

					String newData = transform(newMetacard, updateClient);

					Response r = updateClient.put(newData);

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("RESPONSE: [" + ToStringBuilder.reflectionToString(r) + "]");
					}
				}

			}
		}

		return input;
	}

	@Override
	public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {

		if (client != null) {

			WebClient updateClient = WebClient.fromClient(client);

			updateClient.type(MediaType.APPLICATION_JSON);

			if (input == null || input.getDeletedMetacards() == null || input.getDeletedMetacards().isEmpty()) {
				return input;
			}

			for (int i = 0; i < input.getDeletedMetacards().size(); i++) {

				Metacard metacard = input.getDeletedMetacards().get(i);

				if (metacard != null && metacard.getId() != null) {

					updateClient.path(metacard.getId());

					Response r = updateClient.delete();

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("RESPONSE: [" + ToStringBuilder.reflectionToString(r) + "]");
					}
				}

			}
		}

		return input;
	}

	public String getParentAddress() {
		return parentAddress;
	}

	public void setParentAddress(String endpointAddress) {

		if (endpointAddress == null) {

			this.parentAddress = endpointAddress;

			client = null;

		} else if (!endpointAddress.equals(this.parentAddress)) {

			String previous = this.parentAddress;

			this.parentAddress = endpointAddress;

			client = WebClient.create(this.parentAddress, true);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Changed the parent address property from [" + previous + "]  to [" + this.parentAddress
						+ "]");
			}
		}

	}

	public MetacardTransformer getTransformer() {
		return transformer;
	}

	public void setTransformer(MetacardTransformer transformer) {
		this.transformer = transformer;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Changed transformer to [" + this.transformer + "]");
		}
	}

	private String transform(Metacard m, WebClient client) throws PluginExecutionException {

		BinaryContent binaryContent;
		try {
			binaryContent = transformer.transform(m, null);
			client.type(getValidMimeType(binaryContent.getMimeTypeValue()));
			return new String(binaryContent.getByteArray());
		} catch (IOException e) {
			LOGGER.warn("Could not understand metacard.", e);
			throw new PluginExecutionException("Could not send metacard.");
		} catch (CatalogTransformerException e) {
			LOGGER.warn("Could not transform metacard.", e);
			throw new PluginExecutionException("Could not send metacard.");
		}
	}

	private String getValidMimeType(String mimeTypeValue) {
		if (mimeTypeValue == null) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		return mimeTypeValue;
	}
}
