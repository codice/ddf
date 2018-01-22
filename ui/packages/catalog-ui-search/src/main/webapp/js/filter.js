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
var _ = require('underscore');
var metacardDefinitions = require('component/singletons/metacard-definitions');
var Terraformer = require('terraformer');
var TerraformerWKTParser = require('terraformer-wkt-parser');
var CQLUtils = require('js/CQLUtils');
var Turf = require('@turf/turf');
var TurfMeta = require('@turf/meta');
var wkx = require('wkx');
var moment = require('moment');
var cql = require('js/cql');

function checkTokenWithWildcard(token, filter) {
    var filterRegex = new RegExp(filter.split('*').join('.*'));
    return filterRegex.test(token);
}

function checkToken(token, filter) {
    if (filter.indexOf('*') >= 0) {
        return checkTokenWithWildcard(token, filter);
    } else if (token === filter) {
        return true;
    }
    return false;
}

function matchesILIKE(value, filter) {
    var valueToCheckFor = filter.value.toLowerCase();
    value = value.toString().toLowerCase();
    var tokens = value.split(' ');
    for (var i = 0; i <= tokens.length - 1; i++) {
        if (checkToken(tokens[i], valueToCheckFor)) {
            return true;
        }
    }
    return false;
}

function matchesLIKE(value, filter) {
    var valueToCheckFor = filter.value;
    var tokens = value.toString().split(' ');
    for (var i = 0; i <= tokens.length - 1; i++) {
        if (checkToken(tokens[i], valueToCheckFor)) {
            return true;
        }
    }
    return false;
}

function matchesEQUALS(value, filter) {
    var valueToCheckFor = filter.value;
    if (value.toString() === valueToCheckFor.toString()) {
        return true;
    }
    return false;
}

function matchesNOTEQUALS(value, filter) {
    var valueToCheckFor = filter.value;
    if (value.toString() !== valueToCheckFor.toString()) {
        return true;
    }
    return false;
}

function matchesGreaterThan(value, filter) {
    var valueToCheckFor = filter.value;
    if (value > valueToCheckFor) {
        return true;
    }
    return false;
}

function matchesGreaterThanOrEqualTo(value, filter) {
    var valueToCheckFor = filter.value;
    if (value >= valueToCheckFor) {
        return true;
    }
    return false;
}

function matchesLessThan(value, filter) {
    var valueToCheckFor = filter.value;
    if (value < valueToCheckFor) {
        return true;
    }
    return false;
}

function matchesLessThanOrEqualTo(value, filter) {
    var valueToCheckFor = filter.value;
    if (value <= valueToCheckFor) {
        return true;
    }
    return false;
}

// terraformer doesn't offically support Point, MultiPoint, FeatureCollection, or GeometryCollection
// terraformer incorrectly supports MultiPolygon, so turn it into a Polygon first
function intersects(terraformerObject, value) {
    var intersected = false;
    switch (value.type) {
        case 'Point':
            return terraformerObject.contains(value);
        case 'MultiPoint':
            value.coordinates.forEach(function (coordinate) {
                intersected = intersected || intersects(terraformerObject, {
                    type: 'Point',
                    coordinates: coordinate
                });
            });
            return intersected;
        case 'LineString':
        case 'MultiLineString':
        case 'Polygon':
            return terraformerObject.intersects(value);
        case 'MultiPolygon':
            value.coordinates.forEach(function (coordinate) {
                intersected = intersected || intersects(terraformerObject, {
                    type: 'Polygon',
                    coordinates: coordinate
                });
            });
            return intersected;
        case 'Feature':
            return intersects(terraformerObject, value.geometry);
        case 'FeatureCollection':
            value.features.forEach(function (feature) {
                intersected = intersected || intersects(terraformerObject, feature);
            });
            return intersected;
        case 'GeometryCollection':
            value.geometries.forEach(function (geometry) {
                intersected = intersected || intersects(terraformerObject, geometry);
            });
            return intersected;
        default:
            return intersected;
    }
}

function matchesPOLYGON(value, filter) {
    var polygonToCheck = TerraformerWKTParser.parse(filter.value.value);
    if (intersects(polygonToCheck, value)) {
        return true;
    }
    return false;
}

function matchesCIRCLE(value, filter) {
    if (filter.distance <= 0) {
        return false;
    }
    var points = filter.value.value.substring(6, filter.value.value.length - 1).split(' ');
    var circleToCheck = new Terraformer.Circle(points, filter.distance, 64);
    var polygonCircleToCheck = new Terraformer.Polygon(circleToCheck.geometry);
    if (intersects(polygonCircleToCheck, value)) {
        return true;
    }
    return false;
}

function matchesLINESTRING(value, filter) {
    var pointText = filter.value.value.substring(11);
    pointText = pointText.substring(0, pointText.length - 1);
    var lineWidth = filter.distance || 0;
    if (lineWidth <= 0) {
        return false;
    }
    var line = pointText.split(',').map(function (coordinate) {
        return coordinate.split(' ').map(function (value) {
            return Number(value);
        });
    });
    var turfLine = Turf.lineString(line);
    var bufferedLine = Turf.buffer(turfLine, lineWidth, 'meters');
    var polygonToCheck = new Terraformer.Polygon({
        type: 'Polygon',
        coordinates: bufferedLine.geometry.coordinates
    });
    if (intersects(polygonToCheck, value)) {
        return true;
    }
    return false;
}

function matchesBEFORE(value, filter) {
    var date1 = moment(value);
    var date2 = moment(filter.value);
    if (date1 <= date2) {
        return true;
    }
    return false;
}

function matchesAFTER(value, filter) {
    var date1 = moment(value);
    var date2 = moment(filter.value);
    if (date1 >= date2) {
        return true;
    }
    return false;
}

function flattenMultivalueProperties(valuesToCheck) {
    return _.flatten(valuesToCheck, true);
}

function matchesFilter(metacard, filter) {
    if (!filter.filters) {
        var valuesToCheck = [];
        if (metacardDefinitions.metacardTypes[filter.property] && metacardDefinitions.metacardTypes[filter.property].type === 'GEOMETRY') {
            filter.property = 'anyGeo';
        }
        switch (filter.property) {
            case '"anyText"':
                valuesToCheck = Object.keys(metacard.properties).filter(function (property) {
                    return Boolean(metacardDefinitions.metacardTypes[property]) && (metacardDefinitions.metacardTypes[property].type === 'STRING');
                }).map(function (property) {
                    return metacard.properties[property];
                });
                break;
            case 'anyGeo':
                valuesToCheck = Object.keys(metacard.properties).filter(function (property) {
                    return Boolean(metacardDefinitions.metacardTypes[property]) && (metacardDefinitions.metacardTypes[property].type === 'GEOMETRY');
                }).map(function (property) {
                    return new Terraformer.Primitive(wkx.Geometry.parse(metacard.properties[property]).toGeoJSON());
                });
                break;
            default:
                var valueToCheck = metacard.properties[filter.property.replace(/['"]+/g, '')];
                if (valueToCheck !== undefined) {
                    valuesToCheck.push(valueToCheck);
                }
                break;
        }

        if (valuesToCheck.length === 0) {
            return filter.value === ""; // aligns with how querying works on the server
        }

        valuesToCheck = flattenMultivalueProperties(valuesToCheck);

        for (var i = 0; i <= valuesToCheck.length - 1; i++) {
            switch (filter.type) {
                case 'ILIKE':
                    if (matchesILIKE(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case 'LIKE':
                    if (matchesLIKE(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case '=':
                    if (matchesEQUALS(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case '!=':
                    if (matchesNOTEQUALS(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case '>':
                    if (matchesGreaterThan(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case '>=':
                    if (matchesGreaterThanOrEqualTo(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case '<':
                    if (matchesLessThan(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case '<=':
                    if (matchesLessThanOrEqualTo(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case 'INTERSECTS':
                    if (matchesPOLYGON(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case 'DWITHIN':
                    if (CQLUtils.isPointRadiusFilter(filter)) {
                        if (matchesCIRCLE(valuesToCheck[i], filter)) {
                            return true;
                        }
                    } else if (matchesLINESTRING(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case 'AFTER':
                    if (matchesAFTER(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
                case 'BEFORE':
                    if (matchesBEFORE(valuesToCheck[i], filter)) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    } else {
        return matchesFilters(metacard, filter);
    }
}

function matchesFilters(metacard, resultFilter) {
    var i;
    switch (resultFilter.type) {
        case 'AND':
            for (i = 0; i <= resultFilter.filters.length - 1; i++) {
                if (!matchesFilter(metacard, resultFilter.filters[i])) {
                    return false;
                }
            }
            return true;
        case 'NOT AND':
            for (i = 0; i <= resultFilter.filters.length - 1; i++) {
                if (!matchesFilter(metacard, resultFilter.filters[i])) {
                    return true;
                }
            }
            return false;
        case 'OR':
            for (i = 0; i <= resultFilter.filters.length - 1; i++) {
                if (matchesFilter(metacard, resultFilter.filters[i])) {
                    return true;
                }
            }
            return false;
        case 'NOT OR':
            for (i = 0; i <= resultFilter.filters.length - 1; i++) {
                if (matchesFilter(metacard, resultFilter.filters[i])) {
                    return false;
                }
            }
            return true;
        default:
            return matchesFilter(metacard, resultFilter);
    }
}

module.exports = {
    matchesFilters: matchesFilters,
    matchesCql: function(metacardJSON, cqlString) {
        if (cqlString === "") {
            return true;
        }
        return this.matchesFilters(metacardJSON, cql.read(cqlString));
    }
};