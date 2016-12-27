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
    'js/store',
    'js/Common'
], function (_, Backbone, wellknown, usngs, store, Common) {

    var converter = new usngs.Converter();
    var minimumDifference = 0.0001;
    var minimumBuffer = 0.000001;

    function convertToValid(key, model){
        if (key.mapSouth !== undefined && 
            (key.mapSouth >= key.mapNorth || 
            (!key.mapNorth && key.mapSouth >= model.get('mapNorth')))){
            key.mapSouth = parseFloat((key.mapNorth || model.get('mapNorth'))) - minimumDifference;
        }
        if (key.mapEast !== undefined &&
            (key.mapEast <= key.mapWest || 
            (!key.mapWest && key.mapEast <= model.get('mapWest')))){
            key.mapEast = parseFloat((key.mapWest || model.get('mapWest'))) + minimumDifference;
        }
        if (key.mapWest !== undefined && 
            (key.mapWest >= key.mapEast || 
            (!key.mapEast && key.mapWest >= model.get('mapEast')))){
            key.mapWest = parseFloat((key.mapEast || model.get('mapEast'))) - minimumDifference;
        }
        if (key.mapNorth !== undefined &&
            (key.mapNorth <= key.mapSouth || 
            (!key.mapSouth && key.mapNorth <= model.get('mapSouth')))){
            key.mapNorth = parseFloat((key.mapSouth || model.get('mapSouth'))) + minimumDifference;
        }
        if (key.mapNorth !== undefined){
            key.mapNorth = Math.max(-90 + minimumDifference, key.mapNorth);
            key.mapNorth = Math.min(90, key.mapNorth);
        }
        if (key.mapSouth !== undefined){
            key.mapSouth = Math.max(-90, key.mapSouth);
            key.mapSouth = Math.min(90 - minimumDifference, key.mapSouth);
        }
        if (key.mapWest !== undefined){
            key.mapWest = Math.max(-180, key.mapWest);
            key.mapWest = Math.min(180 - minimumDifference, key.mapWest);
        }
        if (key.mapEast !== undefined){
            key.mapEast = Math.max(-180 + minimumDifference, key.mapEast);
            key.mapEast = Math.min(180, key.mapEast);
        }
        if (key.lat !== undefined){
            key.lat = Math.max(-90, key.lat);
            key.lat = Math.min(90, key.lat);
        }
        if (key.lon !== undefined){
            key.lon = Math.max(-180, key.lon);
            key.lon = Math.min(180, key.lon);
        }
        if (key.radius !== undefined){
            key.radius = Math.max(minimumBuffer, key.radius);
        }
        if (key.lineWidth !== undefined){
            key.lineWidth = Math.max(minimumBuffer, key.lineWidth);
        }
    }

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
            radius: 1,
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
        set: function(key, value, options){
            if (!_.isObject(key)){
                var keyObject = {};
                keyObject[key] = value;
                key = keyObject;
                value = options;
            }
            convertToValid(key, this);
            Backbone.AssociatedModel.prototype.set.call(this, key, value, options);
            Common.queueExecution(function(){
                this.trigger('change', Object.keys(key));
            }.bind(this));
        },
        initialize: function(){
            this.listenTo(this, 'change:north change:south change:east change:west', this.setBBox);
            this.listenTo(this, 'change:locationType', this.handleLocationType);
            this.listenTo(this, 'change:bbox', this.setBboxLatLon);
            this.listenTo(this, 'change:lat change:lon', this.setRadiusLatLon);
            this.listenTo(this, 'change:usngbb', this.setBboxUsng);
            this.listenTo(this, 'change:usng', this.setRadiusUsng);
            this.listenTo(this, 'EndExtent', this.notDrawing);
            this.listenTo(this, 'BeginExtent', this.drawingOn);
            if (this.get('color') === undefined && store.get('content').get('query')){
                this.set('color', store.get('content').get('query').get('color'));
            } else if (this.get('color') === undefined) {
                this.set('color', '#c89600');
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
            if (this.get('locationType') === "latlon") {
                var result = {};
                result.north = this.get('mapNorth');
                result.south = this.get('mapSouth');
                result.west = this.get('mapWest');
                result.east = this.get('mapEast');
                if (!(result.north && result.south && result.west && result.east)) {
                    result = converter.USNGtoLL(this.get('usngbb'));

                }
                this.set(result);
            }
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
                try {
                    var usngsStr = converter.LLBboxtoUSNG(north, south, east, west);

                    this.set('usngbb', usngsStr, {silent: this.get('locationType') !== 'usng'});
                    if (this.get('locationType') === 'usng' && this.drawing) {
                        this.repositionLatLon();
                    }
                } catch(err){

                }
            }
        },

        setRadiusLatLon: function () {
            var lat = this.get('lat'),
                lon = this.get('lon');
            if (lat && lon) {
                try {
                    var usngsStr = converter.LLtoUSNG(lat, lon, 6);
                    this.set('usng', usngsStr, {silent: true});
                } catch(err){

                }
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
        },
        handleLocationType: function(){
            if (this.get('locationType') === 'latlon') {
                this.set({
                    north: this.get('mapNorth'),
                    south: this.get('mapSouth'),
                    east: this.get('mapEast'),
                    west: this.get('mapWest')
                });
            }
        }
    });
});