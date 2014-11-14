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
define([
    'underscore',
    'moment',
    'properties'
],function (_, moment, Properties) {



    var Filter = {};

    Filter.CQLFactory = {
        toCQL: function(filter){
            var fieldType = filter.get('fieldType');
            var fieldOperator = filter.get('fieldOperator');
            if(Filter.CQLFactory[fieldType] && Filter.CQLFactory[fieldType][fieldOperator]){
                return "(" + Filter.CQLFactory[fieldType][fieldOperator](filter) + ")"; // fun
            }
            return null;
        },

        getValue: function(value) {
            switch (typeof value) {
                case 'string':
                    if(moment(value).isValid()){
                        return moment(value).format(Properties.CQL_DATE_FORMAT);
                    } else {
                        return "'" + value.replace(/'/g, "''") + "'";
                    }
                    break;
                case 'number':
                    return String(value);
                case 'object':
                    if (_.isDate(value)) {
                        return moment(value).format(Properties.CQL_DATE_FORMAT);
                    } else {
                        throw new Error("Can't write object to CQL: " + value);
                    }
                    break;
                default:
                    throw new Error("Can't write value to CQL: " + value);
            }
        },

        formatFieldName: function(fieldName){
            if(fieldName === 'anyType'){
                return fieldName;
            }
            return '"' + fieldName + '"';
        },

        string: {
            'equals': function(filter){
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' = ' + Filter.CQLFactory.getValue(filter.get('stringValue1'));
            },
            'contains': function(filter){
                if(filter.get('fieldName') === Properties.filters.METADATA_CONTENT_TYPE){
                    var split = filter.get('stringValue1').split(',');
                    var joinArray = [];
                    _.each(split, function(subContentType){
                        joinArray.push(Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' ILIKE ' + Filter.CQLFactory.getValue(subContentType));
                    });
                    return joinArray.join(' OR ');
                } else {
                    return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' ILIKE ' + Filter.CQLFactory.getValue(filter.get('stringValue1'));
                }

            }
        },
        xml: {
            'equals': function(filter){
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' = ' + Filter.CQLFactory.getValue(filter.get('stringValue1'));
            },
            'contains': function(filter){
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' ILIKE ' + Filter.CQLFactory.getValue(filter.get('stringValue1'));
            }
        },
        date: {
            'before': function(filter){
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' BEFORE ' + Filter.CQLFactory.getValue(filter.get('dateValue1'));
            },
            'after': function(filter){
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' AFTER ' + Filter.CQLFactory.getValue(filter.get('dateValue1'));
            }
        },
        number: {
            '=': function (filter) {
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' = ' + filter.get('numberValue1');
            },
            '!=': function () {
                throw new Error("!= is not supported for this filter.");
            },
            '>': function (filter) {
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' > ' + filter.get('numberValue1');
            },
            '>=': function (filter) {
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' >= ' + filter.get('numberValue1');
            },
            '<': function (filter) {
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' < ' + filter.get('numberValue1');
            },
            '<=': function (filter) {
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' <= ' + filter.get('numberValue1');
            }
        },
        anyGeo: {
            'intersects': function(filter) {
                var geoType = filter.get('geoType');
                if(geoType === 'bbox'){
                    // build the bbox value.
                    var north = filter.get('north'),
                        south = filter.get('south'),
                        west = filter.get('west'),
                        east = filter.get('east');

                    var bbox = 'POLYGON ((' +
                        west + ' ' + south +
                        ', ' + west + ' ' + north +
                        ', ' + east + ' ' + north +
                        ', ' + east + ' ' + south +
                        ', ' + west + ' ' + south +
                        '))';

                    return 'INTERSECTS(anyGeo, ' + bbox + ')';
                } else if(geoType === 'polygon'){
                    // build the polygon value.
                    var polygon = filter.get('polygon');
                    var poly = 'POLYGON ((';
                    var polyPoint;
                    for (var i = 0;i<polygon.length;i++) {
                        polyPoint = polygon[i];
                        poly += polyPoint[0] + ' ' + polyPoint[1];
                        if (i < polygon.length - 1) {
                            poly += ', ';
                        }
                    }

                    if(_.first(polygon)[0] !== _.last(polygon)[0] && _.first(polygon)[1] !== _.last(polygon)[1]){
                        // first and last point must be the same.
                        poly += ' , ' + _.first(polygon)[0] + ' ' + _.first(polygon)[1];
                    }

                    poly += '))';
                    return 'INTERSECTS(anyGeo, ' + poly + ')';
                } else if(geoType === 'circle'){
                    // build the circle cql value.
                    var lon = filter.get('lon'),
                        lat = filter.get('lat'),
                        radius = filter.get('radius');
                    var point = 'POINT(' + lon + ' ' + lat + ')';

                    return 'DWITHIN(anyGeo, ' + point + ',' + radius + ', meters)';
                }
                return null;
            }
        }
    };

    return Filter.CQLFactory;
});