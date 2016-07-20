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
    'js/cql',
    'component/singletons/metacard-definitions'
], function (cql, metacardDefinitions) {

    return {
        sanitizeForCql: function (text) {
            return text.split('[').join('(').split(']').join(')').split("'").join('').split('"').join('');
        },
        //we should probably regex this or find a better way, but for now this works
        sanitizeGeometryCql: function (cqlString) {
            return cqlString.split("'POLYGON((").join("POLYGON((").split("))'").join("))")
                .split("'POINT(").join("POINT(").split(")'").join(")")
                .split("'LINESTRING(").join("LINESTRING(").split("))'").join("))");
        },
        getProperty: function (filter) {
            return filter.property.split('"').join('');
        },
        generateFilter: function (type, property, value) {
            switch (metacardDefinitions.metacardTypes[property].type) {
                case 'LOCATION':
                case 'GEOMETRY':
                    return this.generateAnyGeoFilter(property, value);
                default:
                    return {
                        type: type,
                        property: '"' + property + '"',
                        value: value
                    };
            }
        },
        generateAnyGeoFilter: function (property, model) {
            switch (model.type) {
                case 'LINE':
                    return {
                        type: 'DWITHIN',
                        property: property,
                        value: 'LINESTRING' +
                        this.sanitizeForCql(JSON.stringify(this.lineToCQLLIne(model.line))),
                        distance: Number(model.lineWidth)
                    };
                case 'POLYGON':
                    return {
                        type: 'INTERSECTS',
                        property: property,
                        value: 'POLYGON(' +
                        this.sanitizeForCql(JSON.stringify(this.polygonToCQLPolygon(model.polygon))) + ')'
                    };
                case 'BBOX':
                    return {
                        type: 'INTERSECTS',
                        property: property,
                        value: 'POLYGON(' +
                        this.sanitizeForCql(JSON.stringify(this.bboxToCQLPolygon(model))) + ')'
                    };
                case 'POINTRADIUS':
                    return {
                        type: 'DWITHIN',
                        property: property,
                        value: 'POINT(' + model.lon + ' ' + model.lat + ')',
                        distance: Number(model.radius)
                    };
            }
        },
        bboxToCQLPolygon: function (model) {
            return [
                model.west + ' ' + model.south,
                model.west + ' ' + model.north,
                model.east + ' ' + model.north,
                model.east + ' ' + model.south,
                model.west + ' ' + model.south
            ];
        },
        polygonToCQLPolygon: function (model) {
            var cqlPolygon = model.map(function (point) {
                return point[0] + ' ' + point[1];
            });
            cqlPolygon.push(cqlPolygon[0]);
            return cqlPolygon;
        },
        lineToCQLLIne: function(model){
            var cqlLINE = model.map(function (point) {
                return point[0] + ' ' + point[1];
            });
            return cqlLINE;
        },
        isGeoFilter: function(type){
            return (type === 'DWITHIN' || type === 'INTERSECTS');
        },
        transformFilterToCQL: function(filter){
            return this.sanitizeGeometryCql("(" + cql.write(cql.simplify(cql.read(cql.write(filter)))) + ")");
        },
        isPointRadiusFilter: function(filter){
            return filter.value && filter.value.value && filter.value.value.indexOf('POINT') >= 0;
        }
    };
});