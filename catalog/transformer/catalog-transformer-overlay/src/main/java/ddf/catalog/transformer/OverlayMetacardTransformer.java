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
package ddf.catalog.transformer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.lang.Validate;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;

public class OverlayMetacardTransformer implements MetacardTransformer {
    private static final String PNG = "png";

    private static final MimeType MIME_TYPE;

    static {
        try {
            MIME_TYPE = new MimeType("image", PNG);
        } catch (MimeTypeParseException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final BiFunction<Metacard, Map<String, Serializable>, Optional<BufferedImage>>
            imageSupplier;

    public OverlayMetacardTransformer(
            BiFunction<Metacard, Map<String, Serializable>, Optional<BufferedImage>> imageSupplier) {
        Validate.notNull(imageSupplier, "The image supplier cannot be null.");
        this.imageSupplier = imageSupplier;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        Validate.notNull(metacard, "The metacard cannot be null.");
        return overlay(metacard, arguments);
    }

    private BinaryContent overlay(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        final Optional<BufferedImage> bufferedImageOptional = imageSupplier.apply(metacard,
                arguments);
        final BufferedImage image =
                bufferedImageOptional.orElseThrow(() -> new CatalogTransformerException(
                        "Did not receive an image from the image supplier."));
        final Builder imageBuilder = Thumbnails.of(image)
                .scale(1)
                .imageType(BufferedImage.TYPE_INT_ARGB)
                .outputFormat(PNG);
        rotate(imageBuilder, metacard.getLocation());
        return createBinaryContent(imageBuilder);
    }

    private void rotate(Builder imageBuilder, String wkt) throws CatalogTransformerException {
        final double angle = GeometryUtils.getRotationAngle(wkt);
        imageBuilder.rotate(angle);
    }

    private BinaryContent createBinaryContent(Builder imageBuilder)
            throws CatalogTransformerException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            imageBuilder.toOutputStream(baos);
        } catch (IOException e) {
            throw new CatalogTransformerException(e);
        }

        return new BinaryContentImpl(new ByteArrayInputStream(baos.toByteArray()), MIME_TYPE);
    }
}
