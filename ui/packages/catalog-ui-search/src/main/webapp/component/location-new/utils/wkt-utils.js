/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global require*/
const wkx = require('wkx');
const errorMessages = require('./errors');

function convertUserValueToWKT(val){
    val = val.split(' (').join('(').split(', ').join(',');
    val = val.split('MULTIPOINT').map(function(value, index){
        if (value.indexOf('((') === 0) {
            var endOfMultiPoint = value.indexOf('))') + 2;
            var multipointStr = value.substring(0, endOfMultiPoint);
            multipointStr = multipointStr.split('((').join('(').split('),(').join(',').split('))').join(')');
            return multipointStr + value.substring(endOfMultiPoint);
        } else {
            return value;
        }
    }).join('MULTIPOINT');
    return val;
}

function removeTrailingZeros(wkt) {
    return wkt.replace(/[-+]?[0-9]*\.?[0-9]+/g, (number) => Number(number));
}

function checkCoordinateOrder(coordinate){
    return coordinate[0] >= -180 && coordinate[0] <= 180 && coordinate[1] >= -90 && coordinate[1] <= 90;
}

function checkGeometryCoordinateOrdering(geometry){
    switch (geometry.type) {
        case 'Point':
            return checkCoordinateOrder(geometry.coordinates);
        case 'LineString':
        case 'MultiPoint':
            return geometry.coordinates.every(function(coordinate) {
                return checkCoordinateOrder(coordinate);
            });
        case 'Polygon':
        case 'MultiLineString':
            return geometry.coordinates.every(function(line) {
                    return line.every(function(coordinate) {
                        return checkCoordinateOrder(coordinate);
                    });
                });
        case 'MultiPolygon':
            return geometry.coordinates.every(function(multipolygon) {
                return multipolygon.every(function(polygon) {
                    return polygon.every(function(coordinate){
                        return checkCoordinateOrder(coordinate);
                    });
                });
            });
        case 'GeometryCollection':
            return geometry.geometries.every(function(subgeometry) {
                return checkGeometryCoordinateOrdering(subgeometry);
            });
    }
}

function checkForm(wkt){
    try {
        var test = wkx.Geometry.parse(wkt);
        return (test.toWkt() === removeTrailingZeros(convertUserValueToWKT(wkt)));
    } catch (err) {
        return false;
    }
}

function checkLonLatOrdering(wkt){
    try {
        var test = wkx.Geometry.parse(wkt);
        return checkGeometryCoordinateOrdering(test.toGeoJSON());
    } catch (err){
        return false;
    }
}

function inputIsBlank(wkt) {
    return !wkt || wkt.length === 0;
}

function validateWkt(wkt) {
    if (inputIsBlank(wkt)) {
        return { valid: true, error: null };
    }

    var valid = true;
    var error = null;
    if (!checkForm(wkt)) {
        valid = false;
        error = errorMessages.malformedWkt;
    } else if (!checkLonLatOrdering(wkt)) {
        valid = false;
        error = errorMessages.invalidWktCoordinates;
    }
    return { valid: valid, error: error };
}

module.exports = validateWkt;