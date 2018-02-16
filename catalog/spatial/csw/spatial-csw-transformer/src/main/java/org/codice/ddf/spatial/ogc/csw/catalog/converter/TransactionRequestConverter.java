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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import net.opengis.cat.csw.v_2_0_2.DeleteType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.InsertAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.DeleteActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.UpdateActionImpl;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;

public class TransactionRequestConverter implements Converter {
  private static JAXBContext jaxBContext;

  private Converter delegatingTransformer;

  private CswRecordConverter cswRecordConverter;

  private AttributeRegistry registry;

  public TransactionRequestConverter(Converter itp, AttributeRegistry registry) {
    this.delegatingTransformer = itp;
    this.registry = registry;
  }

  public CswRecordConverter getCswRecordConverter() {
    return this.cswRecordConverter;
  }

  public void setCswRecordConverter(CswRecordConverter cswRecordConverter) {
    this.cswRecordConverter = cswRecordConverter;
  }

  @Override
  public void marshal(
      Object o, HierarchicalStreamWriter writer, MarshallingContext marshallingContext) {
    if (o == null || !CswTransactionRequest.class.isAssignableFrom(o.getClass())) {
      return;
    }
    CswTransactionRequest request = (CswTransactionRequest) o;

    writer.addAttribute(CswConstants.SERVICE, request.getService());
    writer.addAttribute(CswConstants.VERSION, request.getVersion());
    writer.addAttribute(CswConstants.VERBOSE_RESPONSE, String.valueOf(request.isVerbose()));
    writer.addAttribute(
        CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER + CswConstants.CSW_NAMESPACE_PREFIX,
        CswConstants.CSW_OUTPUT_SCHEMA);
    writer.addAttribute(
        CswConstants.XMLNS + CswConstants.NAMESPACE_DELIMITER + CswConstants.OGC_NAMESPACE_PREFIX,
        CswConstants.OGC_SCHEMA);
    for (InsertAction insertAction : request.getInsertActions()) {
      writer.startNode(CswConstants.CSW_TRANSACTION_INSERT_NODE);
      writer.addAttribute(CswConstants.TYPE_NAME_PARAMETER, insertAction.getTypeName());
      marshallingContext.put(CswConstants.TRANSFORMER_LOOKUP_KEY, TransformerManager.ID);
      marshallingContext.put(CswConstants.TRANSFORMER_LOOKUP_VALUE, insertAction.getTypeName());
      for (Metacard metacard : insertAction.getRecords()) {
        marshallingContext.convertAnother(metacard, delegatingTransformer);
      }
      writer.endNode();
    }
    for (UpdateAction updateAction : request.getUpdateActions()) {
      writer.startNode(CswConstants.CSW_TRANSACTION_UPDATE_NODE);
      writer.addAttribute(CswConstants.TYPE_NAME_PARAMETER, updateAction.getTypeName());
      marshallingContext.put(CswConstants.TRANSFORMER_LOOKUP_KEY, TransformerManager.ID);
      marshallingContext.put(CswConstants.TRANSFORMER_LOOKUP_VALUE, updateAction.getTypeName());
      marshallingContext.convertAnother(updateAction.getMetacard(), delegatingTransformer);

      writer.endNode();
    }
    for (DeleteAction deleteAction : request.getDeleteActions()) {
      writer.startNode(CswConstants.CSW_TRANSACTION_DELETE_NODE);
      writer.addAttribute(CswConstants.TYPE_NAME_PARAMETER, deleteAction.getTypeName());
      writer.startNode(CswConstants.CSW_CONSTRAINT);
      writer.addAttribute(CswConstants.VERSION, CswConstants.CONSTRAINT_VERSION);
      writer.startNode(CswConstants.CSW_CQL_TEXT);
      writer.setValue(deleteAction.getConstraint().getCqlText());
      writer.endNode();
      writer.endNode();

      writer.endNode();
    }
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    CswTransactionRequest cswTransactionRequest = new CswTransactionRequest();

    cswTransactionRequest.setVersion(reader.getAttribute(CswConstants.VERSION));
    cswTransactionRequest.setService(reader.getAttribute(CswConstants.SERVICE));
    cswTransactionRequest.setVerbose(
        Boolean.valueOf(reader.getAttribute(CswConstants.VERBOSE_RESPONSE)));

    XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);

    while (reader.hasMoreChildren()) {
      reader.moveDown();

      if (reader.getNodeName().contains("Insert")) {
        String typeName =
            StringUtils.defaultIfEmpty(
                reader.getAttribute(CswConstants.TYPE_NAME_PARAMETER), CswConstants.CSW_RECORD);
        String handle =
            StringUtils.defaultIfEmpty(reader.getAttribute(CswConstants.HANDLE_PARAMETER), "");
        context.put(CswConstants.TRANSFORMER_LOOKUP_KEY, TransformerManager.ID);
        context.put(CswConstants.TRANSFORMER_LOOKUP_VALUE, typeName);
        List<Metacard> metacards = new ArrayList<>();
        // Loop through the individual records to be inserted, converting each into a Metacard
        while (reader.hasMoreChildren()) {
          reader.moveDown(); // move down to the record's tag
          Metacard metacard =
              (Metacard) context.convertAnother(null, MetacardImpl.class, delegatingTransformer);
          if (metacard != null) {
            metacards.add(metacard);
          }

          // move back up to the <SearchResults> parent of the <csw:Record> tags
          reader.moveUp();
        }
        cswTransactionRequest
            .getInsertActions()
            .add(new InsertActionImpl(typeName, handle, metacards));
      } else if (reader.getNodeName().contains("Delete")) {
        XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);

        Map<String, String> xmlnsAttributeToUriMappings =
            getXmlnsAttributeToUriMappingsFromContext(context);
        Map<String, String> prefixToUriMappings =
            getPrefixToUriMappingsFromXmlnsAttributes(xmlnsAttributeToUriMappings);

        StringWriter writer = new StringWriter();
        XStreamAttributeCopier.copyXml(reader, writer, xmlnsAttributeToUriMappings);

        DeleteType deleteType = getElementFromXml(writer.toString(), DeleteType.class);

        cswTransactionRequest
            .getDeleteActions()
            .add(new DeleteActionImpl(deleteType, prefixToUriMappings));
      } else if (reader.getNodeName().contains("Update")) {
        XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);
        UpdateAction updateAction = parseUpdateAction(reader, context);
        cswTransactionRequest.getUpdateActions().add(updateAction);
      }
      reader.moveUp();
    }

    return cswTransactionRequest;
  }

  private UpdateAction parseUpdateAction(
      HierarchicalStreamReader reader, UnmarshallingContext context) {
    Map<String, String> xmlnsAttributeToUriMappings =
        getXmlnsAttributeToUriMappingsFromContext(context);
    Map<String, String> prefixToUriMappings =
        getPrefixToUriMappingsFromXmlnsAttributes(xmlnsAttributeToUriMappings);

    String typeName =
        StringUtils.defaultIfEmpty(
            reader.getAttribute(CswConstants.TYPE_NAME_PARAMETER), CswConstants.CSW_RECORD);
    String handle =
        StringUtils.defaultIfEmpty(reader.getAttribute(CswConstants.HANDLE_PARAMETER), "");

    // Move down to the content of the <Update>.
    reader.moveDown();

    UpdateAction updateAction;

    // Do we have a list of <RecordProperty> elements or a new <csw:Record>?
    if (reader.getNodeName().contains("RecordProperty")) {
      Map<String, Serializable> cswRecordProperties = new HashMap<>();

      while (reader.getNodeName().contains("RecordProperty")) {
        String cswField;
        Serializable newValue = null;

        // Move down to the <Name>.
        reader.moveDown();
        if (reader.getNodeName().contains("Name")) {
          String attribute = reader.getValue();
          cswField = CswRecordConverter.getCswAttributeFromAttributeName(attribute);
        } else {
          throw new ConversionException(
              "Missing Parameter Value: missing a Name in a RecordProperty.");
        }
        // Move back up to the <RecordProperty>.
        reader.moveUp();

        String attrName = DefaultCswRecordMap.getDefaultMetacardFieldForPrefixedString(cswField);
        cswRecordProperties.put(attrName, null);
        // Is there a <Value>?
        while (reader.hasMoreChildren()) {
          // Move down to the <Value>.
          reader.moveDown();

          if (reader.getNodeName().contains("Value")) {
            newValue = getRecordPropertyValue(reader, attrName);
          } else {
            throw new ConversionException(
                "Invalid Parameter Value: invalid element in a RecordProperty.");
          }
          Serializable currentValue = cswRecordProperties.get(attrName);
          if (currentValue != null) {
            if (currentValue instanceof List) {
              ((List) currentValue).add(newValue);
            } else {
              LinkedList<Serializable> list = new LinkedList<>();
              list.add(currentValue);
              list.add(newValue);
              cswRecordProperties.put(attrName, list);
            }
          } else {
            cswRecordProperties.put(attrName, newValue);
          }

          // Back to the <RecordProperty>.
          reader.moveUp();
        }

        // Back to the <Update>, look for the next <RecordProperty>.
        reader.moveUp();

        if (!reader.hasMoreChildren()) {
          // If there aren't any more children of the <Update>, that means there's no
          // Constraint, which is required.
          throw new ConversionException("Missing Parameter Value: missing a Constraint.");
        }

        // What's the next element in the <Update>?
        reader.moveDown();
      }

      // Now there should be a <Constraint> element.
      if (reader.getNodeName().contains("Constraint")) {
        StringWriter writer = new StringWriter();
        XStreamAttributeCopier.copyXml(reader, writer, xmlnsAttributeToUriMappings);

        QueryConstraintType constraint =
            getElementFromXml(writer.toString(), QueryConstraintType.class);

        // For any CSW attributes that map to basic metacard attributes (e.g. title,
        // modified date, etc.), update the basic metacard attributes as well.
        Map<String, String> cswToMetacardAttributeNames =
            DefaultCswRecordMap.getDefaultCswRecordMap().getCswToMetacardAttributeNames();
        Map<String, Serializable> cswRecordPropertiesWithMetacardAttributes =
            new HashMap<>(cswRecordProperties);

        for (Entry<String, Serializable> recordProperty : cswRecordProperties.entrySet()) {
          String cswAttributeName = recordProperty.getKey();

          // If this CSW attribute maps to a basic metacard attribute, attempt to set the
          // basic metacard attribute.
          if (cswToMetacardAttributeNames.containsKey(cswAttributeName)) {
            String metacardAttrName = cswToMetacardAttributeNames.get(cswAttributeName);
            // If this basic metacard attribute hasn't already been set, set it.
            if (!cswRecordPropertiesWithMetacardAttributes.containsKey(metacardAttrName)) {
              Attribute metacardAttr =
                  cswRecordConverter.getMetacardAttributeFromCswAttribute(
                      cswAttributeName, recordProperty.getValue(), metacardAttrName);
              cswRecordPropertiesWithMetacardAttributes.put(
                  metacardAttrName, metacardAttr.getValue());
            }
          }
        }

        updateAction =
            new UpdateActionImpl(
                cswRecordPropertiesWithMetacardAttributes,
                typeName,
                handle,
                constraint,
                prefixToUriMappings);
      } else {
        throw new ConversionException("Missing Parameter Value: missing a Constraint.");
      }
    } else {
      context.put(CswConstants.TRANSFORMER_LOOKUP_KEY, TransformerManager.ID);
      context.put(CswConstants.TRANSFORMER_LOOKUP_VALUE, typeName);
      Metacard metacard =
          (Metacard) context.convertAnother(null, MetacardImpl.class, delegatingTransformer);

      updateAction = new UpdateActionImpl(metacard, typeName, handle);
      // Move back to the <Update>.
      reader.moveUp();
    }

    return updateAction;
  }

  private Serializable getRecordPropertyValue(HierarchicalStreamReader reader, String cswField) {
    try {
      Serializable newValue;
      if (reader.hasMoreChildren()) {
        reader.moveDown();
        newValue = readPropertyValue(reader, cswField);
        reader.moveUp();
      } else {
        newValue = readPropertyValue(reader, cswField);
      }

      return newValue;
    } catch (NumberFormatException e) {
      throw new ConversionException(
          "Invalid Parameter Value: a RecordProperty "
              + "specified a Value that does not match the type "
              + cswField
              + " expected by  for the field "
              + cswField,
          e);
    }
  }

  private Serializable readPropertyValue(HierarchicalStreamReader reader, String cswField) {
    if (registry != null) {
      Optional<AttributeDescriptor> descriptor = registry.lookup(cswField);
      if (descriptor.isPresent()) {
        return CswUnmarshallHelper.convertRecordPropertyToMetacardAttribute(
            descriptor.get().getType().getAttributeFormat(), reader, CswAxisOrder.LON_LAT);
      }
    }
    // Assume the value is a String
    return reader.getValue();
  }

  private <T> T getElementFromXml(String xml, Class<T> clazz) {
    JAXBElement<T> root;

    try {
      JAXBContext jaxbContext = getJaxBContext();
      XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
      xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
      XMLStreamReader xmlStreamReader =
          xmlInputFactory.createXMLStreamReader(new StringReader(xml));
      root = jaxbContext.createUnmarshaller().unmarshal(xmlStreamReader, clazz);
    } catch (JAXBException | XMLStreamException e) {
      throw new ConversionException(e);
    }

    return root.getValue();
  }

  public static synchronized JAXBContext getJaxBContext() throws JAXBException {
    if (jaxBContext == null) {

      List<String> contextList =
          Arrays.asList(
              net.opengis.cat.csw.v_2_0_2.ObjectFactory.class.getPackage().getName(),
              net.opengis.filter.v_1_1_0.ObjectFactory.class.getPackage().getName(),
              net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage().getName(),
              net.opengis.ows.v_1_0_0.ObjectFactory.class.getPackage().getName());
      jaxBContext = JAXBContext.newInstance(StringUtils.join(contextList, ":"));
    }
    return jaxBContext;
  }

  private Map<String, String> getXmlnsAttributeToUriMappingsFromContext(
      UnmarshallingContext context) {
    Object namespaceObj = context.get(CswConstants.NAMESPACE_DECLARATIONS);
    if (namespaceObj instanceof Map<?, ?>) {
      return (Map<String, String>) namespaceObj;
    }

    return null;
  }

  private Map<String, String> getPrefixToUriMappingsFromXmlnsAttributes(
      Map<String, String> xmlnsAttributeToUriMappings) {
    if (xmlnsAttributeToUriMappings != null) {
      // The xmlns attributes on the top-level Transaction element have been copied
      // into the UnmarshallingContext by
      // XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext().
      Map<String, String> prefixToUriMappings = new HashMap<>();
      for (Entry<String, String> entry : xmlnsAttributeToUriMappings.entrySet()) {
        String xmlnsAttribute = entry.getKey();
        if (StringUtils.contains(xmlnsAttribute, CswConstants.NAMESPACE_DELIMITER)) {
          String prefix = xmlnsAttribute.split(CswConstants.NAMESPACE_DELIMITER)[1];
          prefixToUriMappings.put(prefix, entry.getValue());
        }
      }
      return prefixToUriMappings;
    }

    return DefaultCswRecordMap.getDefaultCswRecordMap().getPrefixToUriMapping();
  }

  @Override
  public boolean canConvert(Class aClass) {
    return CswTransactionRequest.class.isAssignableFrom(aClass);
  }
}
