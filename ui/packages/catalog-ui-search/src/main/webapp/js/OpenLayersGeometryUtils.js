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
/*global define*/
/*jshint bitwise: false*/
define([
    'openlayers',
    'properties',
    './Common'
], function (ol, properties, Common) {
    return {
        wrapCoordinatesFromGeometry: function(geometry) {
            var type = geometry.getType();
            var coordinates = [];
            switch (type) {
                case 'LineString':
                    coordinates = geometry.getCoordinates();
                    break;
                case 'Polygon':
                    coordinates = geometry.getCoordinates()[0];
                    break;
                case 'Circle':
                    coordinates = [geometry.getCenter()];
                    break;
                default:
                    break;
            }
            coordinates = coordinates.map(function(p) { return ol.proj.transform(p, properties.projection, 'EPSG:4326'); });
            coordinates = Common.wrapMapCoordinatesArray(coordinates);
            coordinates = coordinates.map(function(p) { return ol.proj.transform(p, 'EPSG:4326', properties.projection); });
            switch (type) {
                case 'LineString':
                    geometry.setCoordinates(coordinates);
                    break;
                case 'Polygon':
                    geometry.setCoordinates([coordinates]);
                    break;
                case 'Circle':
                    geometry.setCenter(coordinates[0]);
                    break;
                default:
                    break;
            }
            return geometry;
        }
    };
});