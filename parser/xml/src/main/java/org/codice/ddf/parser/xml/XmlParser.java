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
package org.codice.ddf.parser.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * XML Parser implementation, uses JAXB internally to marshal and unmarshal object.
 */
public class XmlParser implements Parser {
    private static final Joiner CTX_JOINER = Joiner.on(":").skipNulls();

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlParser.class);

    private LoadingCache<CacheKey, JAXBContext> jaxbContextCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS).maximumSize(1000)
            .build(new CacheLoader<CacheKey, JAXBContext>() {
                @Override
                public JAXBContext load(CacheKey cacheKey) throws Exception {
                    JAXBContext jaxbContext;

                    try {
                        jaxbContext = JAXBContext.newInstance(cacheKey.joinedPath, cacheKey.loader);
                    } catch (JAXBException e) {
                        LOGGER.error("Unable to create JAXB context using context path: {}",
                                cacheKey.joinedPath, e);
                        throw e;
                    }
                    return jaxbContext;
                }
            });

    @Override
    public ParserConfigurator configureParser(List<String> contextPath, ClassLoader loader) {
        return new XmlParserConfigurator().
                setContextPath(contextPath).
                setClassLoader(loader);
    }

    @Override
    public void marshal(ParserConfigurator configurator, Object obj, OutputStream os)
            throws ParserException {
        JAXBContext jaxbContext = getContext(configurator.getContextPath(),
                configurator.getClassLoader());

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(configurator.getClassLoader());
            Marshaller marshaller = jaxbContext.createMarshaller();
            if (configurator.getAdapter() != null) {
                marshaller.setAdapter(configurator.getAdapter());
            }
            if (configurator.getHandler() != null) {
                marshaller.setEventHandler(configurator.getHandler());
            }
            for (Map.Entry<String, Object> propRow : configurator.getProperties().entrySet()) {
                marshaller.setProperty(propRow.getKey(), propRow.getValue());
            }

            marshaller.marshal(obj, os);
        } catch (JAXBException e) {
            LOGGER.error("Error marshalling ", e);
            throw new ParserException("Error marshalling", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    @Override
    public <T> T unmarshal(ParserConfigurator configurator, Class<? extends T> cls,
            InputStream stream) throws ParserException {
        JAXBContext jaxbContext = getContext(configurator.getContextPath(),
                configurator.getClassLoader());

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(configurator.getClassLoader());

            XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance()
                    .createXMLStreamReader(stream);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            if (configurator.getAdapter() != null) {
                unmarshaller.setAdapter(configurator.getAdapter());
            }
            if (configurator.getHandler() != null) {
                unmarshaller.setEventHandler(configurator.getHandler());
            }
            for (Map.Entry<String, Object> propRow : configurator.getProperties().entrySet()) {
                unmarshaller.setProperty(propRow.getKey(), propRow.getValue());
            }

            return (T) unmarshaller.unmarshal(xmlStreamReader);
        } catch (JAXBException | XMLStreamException e) {
            LOGGER.error("Error unmarshalling ", e);
            throw new ParserException("Error unmarshalling", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    private JAXBContext getContext(List<String> contextPath, ClassLoader loader)
            throws ParserException {
        String joinedPath = CTX_JOINER.join(contextPath);

        JAXBContext jaxbContext;

        try {
            jaxbContext = jaxbContextCache.get(new CacheKey(joinedPath, loader));
        } catch (ExecutionException e) {
            LOGGER.error("Unable to create JAXB context using context path: {}", joinedPath, e);
            throw new ParserException("Unable to create XmlParser", e.getCause());
        }

        return jaxbContext;
    }

    static class CacheKey {
        private final String joinedPath;

        private final ClassLoader loader;

        CacheKey(String joinedPath, ClassLoader loader) {
            this.joinedPath = joinedPath;
            this.loader = loader;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CacheKey cacheKey = (CacheKey) o;

            return new EqualsBuilder().append(joinedPath, cacheKey.joinedPath)
                    .append(loader, cacheKey.loader).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(joinedPath).append(loader).toHashCode();
        }
    }
}
