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

    function normalizeLongitude(lon_deg) {
        if (lon_deg >= -180 && lon_deg <= 180) {
            return lon_deg;//common case, and avoids slight double precision shifting
        }
        var off = (lon_deg + 180) % 360;
        if (off < 0) {
            return 180 + off;
        }
        else if (off === 0 && lon_deg > 0) {
            return 180;
        } else {
            return -180 + off;
        }
    }

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
                    return "'" + value.replace(/'/g, "''") + "'";
                case 'number':
                    return String(value);
                default:
                    throw new Error("Can't write value to CQL: " + value);
            }
        },

        getDateValue: function(value) {
            if (_.isDate(value) || moment(value).isValid()) {
                return moment.utc(value).format(Properties.CQL_DATE_FORMAT);
            } else {
                throw new Error("Can't write date value to CQL: " + value);
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
                        if(subContentType === 'no-value'){
                            joinArray.push(Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' IS NULL ');
                        } else {
                            // lets check if its a mapped value
                            if(Properties.typeNameMapping && Properties.typeNameMapping[subContentType]){
                                // for each content type mapped to this value, create a expression for it.
                                _.each(Properties.typeNameMapping[subContentType], function(customContentType){
                                    joinArray.push(Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' = ' + Filter.CQLFactory.getValue(customContentType));
                                });
                            } else {
                                // not a custom value.  just add normally.
                                joinArray.push(Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' = ' + Filter.CQLFactory.getValue(subContentType));
                            }
                        }
                    });
                    return joinArray.join(' OR ');
                } else {
                    var value = Filter.CQLFactory.getValue(filter.get('stringValue1'));
                    value = value.replace(/\*/g,"%");  // replace * with %.
                    return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' ILIKE ' + value;
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
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' BEFORE ' + Filter.CQLFactory.getDateValue(filter.get('dateValue1'));
            },
            'after': function(filter){
                return Filter.CQLFactory.formatFieldName(filter.get('fieldName')) + ' AFTER ' + Filter.CQLFactory.getDateValue(filter.get('dateValue1'));
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
                        west = normalizeLongitude(filter.get('west')),
                        east = normalizeLongitude(filter.get('east'));

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