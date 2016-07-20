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
define([
    'underscore',
    'backbone',
    'wellknown',
    'usngs',
    'js/store'
], function (_, Backbone, wellknown, usngs, store) {

    var converter = new usngs.Converter();

    return Backbone.AssociatedModel.extend({
        defaults: {
            drawing: false,
            north: undefined,
            east: undefined,
            south: undefined,
            west: undefined,
            mapNorth: undefined,
            mapEast: undefined,
            mapWest: undefined,
            mapSouth: undefined,
            radiusUnits: 'meters',
            radius: 0,
            locationType: 'latlon',
            lat: undefined,
            lon: undefined,
            bbox: undefined,
            usngbb: undefined,
            usng: undefined,
            color: undefined,
            line: undefined,
            lineWidth: 1,
            lineUnits: 'meters'
        },
        initialize: function(){
            this.listenTo(this, 'change:north change:south change:east change:west', this.setBBox);
            this.listenTo(this, 'change:bbox', this.setBboxLatLon);
            this.listenTo(this, 'change:lat change:lon', this.setRadiusLatLon);
            this.listenTo(this, 'change:usngbb', this.setBboxUsng);
            this.listenTo(this, 'change:usng', this.setRadiusUsng);
            this.listenTo(this, 'EndExtent', this.notDrawing);
            this.listenTo(this, 'BeginExtent', this.drawingOn);
            if (store.get('content').get('query')){
                this.set('color', store.get('content').get('query').get('color'));
            } else {
                this.set('color', '#cab2d6');
            }
        },
        notDrawing: function () {
            this.drawing = false;
            store.get('content').turnOffDrawing();
        },

        drawingOn: function () {
            this.drawing = true;
            store.get('content').turnOnDrawing();
        },

        repositionLatLon: function () {
            if (this.get('usngbb')) {
                var result = converter.USNGtoLL(this.get('usngbb'));
                var newResult = {};
                newResult.mapNorth = result.north;
                newResult.mapSouth = result.south;
                newResult.mapEast = result.east;
                newResult.mapWest = result.west;

                this.set(newResult);
            }
        },

        setLatLon: function () {
            var result = {};
            result.north = this.get('mapNorth');
            result.south = this.get('mapSouth');
            result.west = this.get('mapWest');
            result.east = this.get('mapEast');
            if (!(result.north && result.south && result.west && result.east)) {
                result = converter.USNGtoLL(this.get('usngbb'));

            }
            this.set(result);
        },

        setFilterBBox: function (model) {
            var north = parseFloat(model.get('north'));
            var south = parseFloat(model.get('south'));
            var west = parseFloat(model.get('west'));
            var east = parseFloat(model.get('east'));

            model.set({mapNorth: north, mapSouth: south, mapEast: east, mapWest: west});
        },

        setBboxLatLon: function () {
            var north = this.get('north'),
                south = this.get('south'),
                west = this.get('west'),
                east = this.get('east');
            if (north && south && east && west) {
                var usngsStr = converter.LLBboxtoUSNG(north, south, east, west);

                this.set('usngbb', usngsStr, {silent: this.get('locationType') !== 'usng'});
                if (this.get('locationType') === 'usng' && this.drawing) {
                    this.repositionLatLon();
                }
            }
        },

        setRadiusLatLon: function () {
            var lat = this.get('lat'),
                lon = this.get('lon');
            if (lat && lon) {
                var usngsStr = converter.LLtoUSNG(lat, lon, 5);
                this.set('usng', usngsStr, {silent: true});
            }
        },

        setBboxUsng: function () {
            var result = converter.USNGtoLL(this.get('usngbb'));
            var newResult = {};
            newResult.mapNorth = result.north;
            newResult.mapSouth = result.south;
            newResult.mapEast = result.east;
            newResult.mapWest = result.west;
            this.set(newResult);
        },

        setBBox: function () {

            //we need these to always be inferred
            //as numeric values and never as strings
            var north = parseFloat(this.get('north'));
            var south = parseFloat(this.get('south'));
            var west = parseFloat(this.get('west'));
            var east = parseFloat(this.get('east'));

            if (north && south && east && west) {
                this.set('bbox', [west, south, east, north].join(','), {silent: this.get('locationType') === 'usng' && !this.drawing});
            }
            if (this.get('locationType') !== 'usng') {
                this.set({mapNorth: north, mapSouth: south, mapEast: east, mapWest: west});
            }
        },

        setRadiusUsng: function () {
            var result = converter.USNGtoLL(this.get('usng'), true);
            this.set(result);
        }
    });
});