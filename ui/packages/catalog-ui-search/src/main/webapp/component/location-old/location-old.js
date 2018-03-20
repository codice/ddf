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
    'wellknown/wellknown',
    'usng.js/usng',
    'js/store',
    'js/Common',
    'wreqr'
], function (_, Backbone, wellknown, usngs, store, Common, wreqr) {

    var converter = new usngs.Converter();
    var minimumDifference = 0.0001;
    var minimumBuffer = 0.000001;
    var utmLocationType = 'utm';
    // offset used by utm for southern hemisphere
    var northingOffset = 10000000;
    var usngPrecision = 6;

    function wrapNum(x, range) {
        var max = range[1],
            min = range[0],
            d = max - min;
        return ((x - min) % d + d) % d + min;
    }

    function convertToValid(key, model){
        if (key.mapSouth !== undefined && 
            (key.mapSouth >= key.mapNorth || 
            (key.mapNorth === undefined && key.mapSouth >= model.get('mapNorth')))){
            key.mapSouth = parseFloat((key.mapNorth || model.get('mapNorth'))) - minimumDifference;
        }
        if (key.mapNorth !== undefined &&
            (key.mapNorth <= key.mapSouth || 
            (key.mapSouth === undefined && key.mapNorth <= model.get('mapSouth')))){
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
            key.mapWest = wrapNum(key.mapWest, [-180, 180]);
        }
        if (key.mapEast !== undefined){
            key.mapEast = wrapNum(key.mapEast, [-180, 180]);
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
            utm: undefined,
            color: undefined,
            line: undefined,
            lineWidth: 1,
            lineUnits: 'meters',
            hasKeyword: false,
            utmUpperLeftEasting: undefined,
            utmUpperLeftNorthing: undefined,
            utmUpperLeftHemisphere: 'Northern',
            utmUpperLeftZone: 1,
            utmLowerRightEasting: undefined,
            utmLowerRightNorthing: undefined,
            utmLowerRightHemisphere: 'Northern',
            utmLowerRightZone: 1,
            utmEasting: undefined,
            utmNorthing: undefined,
            utmZone: 1,
            utmHemisphere: 'Northern'
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
            this.listenTo(this, 'change:utmEasting', this.setRadiusUtm);
            this.listenTo(this, 'change:utmNorthing', this.setRadiusUtm);
            this.listenTo(this, 'change:utmZone', this.setRadiusUtm);
            this.listenTo(this, 'change:utmHemisphere', this.setRadiusUtm);
            this.listenTo(this, 'change:utmUpperLeftEasting', this.setBboxUtm);
            this.listenTo(this, 'change:utmUpperLeftNorthing', this.setBboxUtm);
            this.listenTo(this, 'change:utmUpperLeftZone', this.setBboxUtm);
            this.listenTo(this, 'change:utmUpperLeftEasting', this.setBboxUtm);
            this.listenTo(this, 'change:utmUpperLeftHemisphere', this.setBboxUtm);
            this.listenTo(this, 'change:utmLowerRightEasting', this.setBboxUtm);
            this.listenTo(this, 'change:utmLowerRightNorthing', this.setBboxUtm);
            this.listenTo(this, 'change:utmLowerRightZone', this.setBboxUtm);
            this.listenTo(this, 'change:utmLowerRightEasting', this.setBboxUtm);
            this.listenTo(this, 'change:utmLowerRightHemisphere', this.setBboxUtm);
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
            store.get('content').turnOnDrawing(this);
        },

        repositionLatLonUtm: function(isDefined,parse,assign,clear) {
            if (isDefined(this)) {
                var utmParts = parse(this);
                if (utmParts !== undefined) {
                    var result = this.UTMtoLL(utmParts);

                    if(result !== undefined) {
                        var newResult = {};
                        assign(newResult, result.lat, result.lon);

                        this.set(newResult);
                    } else {
                        clear(this);
                    }
                }
            }
        },

        repositionLatLon: function () {
            if (this.get('usngbb') !== undefined) {
                var result = converter.USNGtoLL(this.get('usngbb'));
                var newResult = {};
                newResult.mapNorth = result.north;
                newResult.mapSouth = result.south;
                newResult.mapEast = result.east;
                newResult.mapWest = result.west;

                this.set(newResult);
            }

            this.repositionLatLonUtm(
                function(_this) { return _this.isUtmUpperLeftDefined(); },
                function(_this) { return _this.parseUtmUpperLeft(); },
                function(newResult, lat, lon) { newResult.mapNorth = lat; newResult.mapWest = lon; },
                function(_this) { return _this.clearUtmUpperLeft(true); });

            this.repositionLatLonUtm(
                function(_this) { return _this.isUtmLowerRightDefined(); },
                function(_this) { return _this.parseUtmLowerRight(); },
                function(newResult, lat, lon) { newResult.mapSouth = lat; newResult.mapEast = lon; },
                function(_this) { return _this.clearUtmLowerRight(true); });

        },

        setLatLonUtm: function(result,isDefined,parse,assign,clear) {
            if (!(result.north !== undefined && result.south !== undefined && result.west !== undefined && result.east !== undefined) && isDefined(this)) {
                var utmParts = parse(_this);
                if (utmParts !== undefined) {
                    var utmResult = this.UTMtoLL(utmParts);

                    if(utmResult !== undefined) {
                        assign(result, utmResult.lat, utmResult.lon);
                    } else {
                        clear(this);
                    }
                }
            }
        },

        setLatLon: function () {
            if (this.get('locationType') === "latlon") {
                var result = {};
                result.north = this.get('mapNorth');
                result.south = this.get('mapSouth');
                result.west = this.get('mapWest');
                result.east = this.get('mapEast');
                if (!(result.north !== undefined && result.south !== undefined && result.west !== undefined && result.east !== undefined) && this.get('usngbb')) {
                    result = converter.USNGtoLL(this.get('usngbb'));
                }

                this.setLatLonUtm(result,
                    function(_this) { return _this.isUtmUpperLeftDefined(); },
                    function(_this) { return _this.parseUtmUpperLeft(); },
                    function(result,lat,lon) { result.north = lat; result.west = lon; },
                    function(_this) { _this.clearUtmUpperLeft(true); });

                this.setLatLonUtm(result,
                    function(_this) { return _this.isUtmLowerRightDefined(); },
                    function(_this) { return _this.parseUtmLowerRight(); },
                    function(result,lat,lon) { result.south = lat; result.east = lon; },
                    function(_this) { _this.clearUtmLowerRight(true); });

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
            if (north !== undefined && south !== undefined && east !== undefined && west !== undefined) {
                try {

                    var usngsStr = converter.LLBboxtoUSNG(north, south, east, west);

                    this.set('usngbb', usngsStr, {silent: this.get('locationType') !== 'usng'});
                    if (this.get('locationType') === 'usng' && this.drawing) {
                        this.repositionLatLon();
                    }

                    var utmCoords = this.LLtoUTM(north, west);
                    if(utmCoords !== undefined) {
                        var utmParts = this.formatUtm(utmCoords);
                        this.setUtmUpperLeft(utmParts, !this.isLocationTypeUtm());
                    }

                    var utmCoords = this.LLtoUTM(south, east);
                    if(utmCoords !== undefined) {
                        var utmParts = this.formatUtm(utmCoords);
                        this.setUtmLowerRight(utmParts, !this.isLocationTypeUtm());
                    }

                    if (this.isLocationTypeUtm() && this.drawing) {
                        this.repositionLatLon();
                    }

                } catch(err){

                }
            }
        },

        setRadiusLatLon: function () {
            var lat = this.get('lat'),
                lon = this.get('lon');

            if (store.get('content').get('drawing') || this.get('locationType') === 'latlon') {

            if (lat !== undefined && lon !== undefined) {
                try {
                    var usngsStr = converter.LLtoUSNG(lat, lon, usngPrecision);
                    this.set('usng', usngsStr, {silent: true});
                } catch(err){

                }

                try {
                    var utmCoords = this.LLtoUTM(lat, lon);
                    if(utmCoords !== undefined) {
                        var utmParts = this.formatUtm(utmCoords);
                        this.setUtmPointRadius(utmParts, true);
                    } else {
                        this.clearUtmPointRadius(false);
                    }
                } catch(err){

                }
            }
            }
        },

        setBboxUsng: function () {
            if(this.get('locationType') === 'usng') {
                var result = converter.USNGtoLL(this.get('usngbb'));
                if(result !== undefined) {
                    var newResult = {};
                    newResult.mapNorth = result.north;
                    newResult.mapSouth = result.south;
                    newResult.mapEast = result.east;
                    newResult.mapWest = result.west;
                    this.set(newResult);
                    this.set(result, {silent: true});

                    var utmCoords = this.LLtoUTM(result.north, result.west);
                    if(utmCoords !== undefined) {
                        var utmFormatted = this.formatUtm(utmCoords);
                        this.setUtmUpperLeft(utmFormatted, true);
                    }

                    var utmCoords = this.LLtoUTM(result.south, result.east);
                    if(utmCoords !== undefined) {
                        var utmFormatted = this.formatUtm(utmCoords);
                        this.setUtmLowerRight(utmFormatted, true);
                    }
                }
            }
        },

        setBBox: function () {

            //we need these to always be inferred
            //as numeric values and never as strings
            var north = parseFloat(this.get('north'));
            var south = parseFloat(this.get('south'));
            var west = parseFloat(this.get('west'));
            var east = parseFloat(this.get('east'));

            if (north !== undefined && south !== undefined && east !== undefined && west !== undefined) {
                this.set('bbox', [west, south, east, north].join(','), {silent: (this.get('locationType') === 'usng' || this.isLocationTypeUtm()) && !this.drawing});
            }
            if (this.get('locationType') !== 'usng' && !this.isLocationTypeUtm()) {
                this.set({mapNorth: north, mapSouth: south, mapEast: east, mapWest: west});
            }
        },

        setRadiusUsng: function () {
            var usng = this.get('usng');
            if (usng !== undefined){
                var result = converter.USNGtoLL(usng, true);

                if(!isNaN(result.lat) && !isNaN(result.lon)) {
                    this.set(result);

                    var utmCoords = this.LLtoUTM(result.lat, result.lon);
                    if(utmCoords !== undefined) {
                        var utmParts = this.formatUtm(utmCoords);
                        this.setUtmPointRadius(utmParts, true);
                    }
                } else {
                    this.clearUtmPointRadius(true);
                    this.set({usng:undefined,lat:undefined,lon:undefined,radius:1});
                }
            }
        },

        isUtmLatLonValid: function(lat) {
            return lat <= 84 && lat >= -80;
        },

        // This method is called when the UTM point radius coordinates are changed by the user.
        setRadiusUtm: function () {
            if(this.isLocationTypeUtm()) {
                if (this.isUtmPointRadiusDefined()) {
                    var utmParts = this.parseUtmPointRadius();
                    if (utmParts !== undefined) {
                        var utmResult = this.UTMtoLL(utmParts);

                        if(utmResult !== undefined) {
                            this.set(utmResult);

                            var usngsStr = converter.LLtoUSNG(utmResult.lat, utmResult.lon, usngPrecision);
                            this.set('usng', usngsStr, {silent: true});
                        } else {
                            this.clearUtmPointRadius(true);
                            this.set({lat:undefined,lon:undefined,usng:undefined,radius:1});
                        }
                    }
                }
            }
        },

        // This method is called when the UTM bounding box coordinates are changed by the user.
        setBboxUtm: function () {

            if(this.isLocationTypeUtm()) {
                var upperLeft = undefined;
                var lowerRight = undefined;

                if (this.isUtmUpperLeftDefined()) {
                    var upperLeftParts = this.parseUtmUpperLeft();
                    if (upperLeftParts !== undefined) {
                        upperLeft = this.UTMtoLL(upperLeftParts);

                        if(upperLeft !== undefined) {
                            this.set({mapNorth: upperLeft.lat, mapWest: upperLeft.lon});
                            this.set({north: upperLeft.lat, west: upperLeft.lon}, {silent: true});
                        } else {
                            this.clearUtmUpperLeft(true);
                            upperLeft = undefined;
                            this.set({mapNorth:undefined,mapSouth:undefined,mapEast:undefined,mapWest:undefined,usngbb:undefined});
                        }
                    }
                }

                if (this.isUtmLowerRightDefined()) {
                    var lowerRightParts = this.parseUtmLowerRight();
                    if (lowerRightParts !== undefined) {
                        lowerRight = this.UTMtoLL(lowerRightParts);

                        if(lowerRight !== undefined) {
                            this.set({mapSouth: lowerRight.lat, mapEast: lowerRight.lon});
                            this.set({south: lowerRight.lat, east: lowerRight.lon}, {silent: true});
                        } else {
                            this.clearUtmLowerRight(true);
                            lowerRight = undefined;
                            this.set({mapNorth:undefined,mapSouth:undefined,mapEast:undefined,mapWest:undefined,usngbb:undefined});
                        }
                    }
                }

                if (upperLeft !== undefined && lowerRight !== undefined) {
                    var usngsStr = converter.LLBboxtoUSNG(upperLeft.lat, lowerRight.lat, lowerRight.lon, upperLeft.lon);
                    this.set('usngbb', usngsStr, {silent: this.get('locationType') === 'usng'});
                }
            }
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
        },

        // Convert Lat-Lon to UTM coordinates. Returns undefined if lat or lon is undefined or not a number.
        // Returns undefined if the underlying call to usng fails. Otherwise, returns an object with:
        //
        //   easting    : FLOAT
        //   northing   : FLOAT
        //   zoneNumber : INTEGER (>=1 and <= 60)
        //   hemisphere : STRING (NORTHERN or SOUTHERN)
        LLtoUTM: function(lat,lon) {

            if(isNaN(lat) || isNaN(lon)) {
                return undefined;
            }

            var utmCoords = [];
            var converterReturnCode = converter.LLtoUTM(lat, lon, utmCoords);

            if(converterReturnCode === "undefined") {
              return undefined;
            }

            var results = {};
            results.easting = utmCoords[0];
            results.northing = lat >= 0 ? utmCoords[1] : utmCoords[1] + northingOffset;
            results.zoneNumber = utmCoords[2];
            results.hemisphere = lat >= 0 ? "NORTHERN" : "SOUTHERN";

            return results;
        },

        // Convert UTM coordinates to Lat-Lon. Expects an argument object with:
        //
        //   easting    : FLOAT
        //   northing   : FLOAT
        //   zoneNumber : INTEGER (>=1 and <= 60)
        //   hemisphere : STRING (NORTHERN or SOUTHERN)
        //
        // Returns an object with:
        //
        //   lat : FLOAT
        //   lon : FLOAT
        //
        // Returns undefined if the latitude is out of range.
        //
        UTMtoLL: function(utmParts) {
            var results = converter.UTMtoLL(utmParts.hemisphere === "NORTHERN" ? utmParts.northing : utmParts.northing - northingOffset,
                                            utmParts.easting,
                                            utmParts.zoneNumber);

            if (!this.isUtmLatLonValid(results.lat)) {
                return undefined;
            }

            results.lon = results.lon % 360;

            if (results.lon < -180) {
                results.lon = results.lon + 360;
            }
            if (results.lon > 180) {
                results.lon = results.lon - 360;
            }
            return results;
        },

        // Return true if the current location type is UTM, otherwise false.
        isLocationTypeUtm: function() {
            return this.get('locationType') === utmLocationType;
        },

        // Set the model fields for the Upper-Left bounding box UTM. The arguments are:
        //
        //   utmFormatted : output from the method 'formatUtm'
        //   silent       : BOOLEAN (true if events should be generated)
        setUtmUpperLeft: function(utmFormatted, silent) {
            this.set('utmUpperLeftEasting', utmFormatted.easting, {silent: silent});
            this.set('utmUpperLeftNorthing', utmFormatted.northing, {silent: silent});
            this.set('utmUpperLeftZone', utmFormatted.zoneNumber, {silent: silent});
            this.set('utmUpperLeftHemisphere', utmFormatted.hemisphere, {silent: silent});
        },

        // Set the model fields for the Lower-Right bounding box UTM. The arguments are:
        //
        //   utmFormatted : output from the method 'formatUtm'
        //   silent       : BOOLEAN (true if events should be generated)
        setUtmLowerRight: function(utmFormatted, silent) {
            this.set('utmLowerRightEasting', utmFormatted.easting, {silent: silent});
            this.set('utmLowerRightNorthing', utmFormatted.northing, {silent: silent});
            this.set('utmLowerRightZone', utmFormatted.zoneNumber, {silent: silent});
            this.set('utmLowerRightHemisphere', utmFormatted.hemisphere, {silent: silent});
        },

        // Set the model fields for the Point Radius UTM. The arguments are:
        //
        //   utmFormatted : output from the method 'formatUtm'
        //   silent       : BOOLEAN (true if events should be generated)
        setUtmPointRadius: function(utmFormatted, silent) {
            this.set('utmEasting', utmFormatted.easting, {silent: silent});
            this.set('utmNorthing', utmFormatted.northing, {silent: silent});
            this.set('utmZone', utmFormatted.zoneNumber, {silent: silent});
            this.set('utmHemisphere', utmFormatted.hemisphere, {silent: silent});
        },

        clearUtmPointRadius: function(silent) {
            this.set('utmEasting', undefined, {silent: silent});
            this.set('utmNorthing', undefined, {silent: silent});
            this.set('utmZone', 1, {silent: silent});
            this.set('utmHemisphere', "Northern", {silent: silent});
        },

        clearUtmUpperLeft: function(silent) {
            this.set({'utmUpperLeftEasting': undefined,
                      'utmUpperLeftNorthing': undefined,
                      'utmUpperLeftZone': 1,
                      'utmUpperLeftHemisphere': "Northern" }, {silent: silent});
        },

        clearUtmLowerRight: function(silent) {
            this.set('utmLowerRightEasting', undefined, {silent: silent});
            this.set('utmLowerRightNorthing', undefined, {silent: silent});
            this.set('utmLowerRightZone', 1, {silent: silent});
            this.set('utmLowerRightHemisphere', "Northern", {silent: silent});
        },

        // Parse the UTM fields that come from the HTML layer. The parameters eastingRaw and northingRaw
        // are string representations of floating pointnumbers. The zoneRaw parameter is a string
        // representation of an integer in the range [1,60]. The hemisphereRaw parameters is a string
        // that should be 'Northern' or 'Southern'.
        parseUtm: function(eastingRaw,northingRaw,zoneRaw,hemisphereRaw) {

            var easting = parseFloat(eastingRaw);
            var northing = parseFloat(northingRaw);
            var zone = parseInt(zoneRaw);
            var hemisphere = hemisphereRaw === "Northern" ? 'NORTHERN' : ( hemisphereRaw === "Southern" ? 'SOUTHERN' : undefined );

            if(!isNaN(easting) &&
               !isNaN(northing) &&
               !isNaN(zone) &&
               hemisphere !== undefined &&
               zone >= 1 && zone <= 60) {

               return {
                   zoneNumber: zone,
                   hemisphere: hemisphere,
                   easting: easting,
                   northing: northing
               }
            }

            return undefined;
        },

        // Format the internal representation of UTM coordinates into the form expected by the model.
        formatUtm: function(utmCoords) {
            return {
                easting: utmCoords.easting,
                northing: utmCoords.northing,
                zoneNumber: utmCoords.zoneNumber,
                hemisphere: utmCoords.hemisphere === 'NORTHERN' ? 'Northern' : ( utmCoords.hemisphere === 'SOUTHERN' ? 'Southern' : undefined )
            };
        },

        // Return true if all of the utm upper-left model fields are defined. Otherwise, false.
        isUtmUpperLeftDefined: function() {
            return this.get('utmUpperLeftEasting') !== undefined &&
                   this.get('utmUpperLeftNorthing') !== undefined &&
                   this.get('utmUpperLeftZone') !== undefined &&
                   this.get('utmUpperLeftHemisphere') !== undefined;
        },

        // Return true if all of the utm lower-right model fields are defined. Otherwise, false.
        isUtmLowerRightDefined: function() {
            return this.get('utmLowerRightEasting') !== undefined &&
                   this.get('utmLowerRightNorthing') !== undefined &&
                   this.get('utmLowerRightZone') !== undefined &&
                   this.get('utmLowerRightHemisphere') !== undefined;
        },

        // Return true if all of the utm point radius model fields are defined. Otherwise, false.
        isUtmPointRadiusDefined: function() {
            return this.get('utmEasting') !== undefined &&
                   this.get('utmNorthing') !== undefined &&
                   this.get('utmZone') !== undefined &&
                   this.get('utmHemisphere') !== undefined;
        },

        // Get the UTM Upper-Left bounding box fields in the internal format. See 'parseUtm'.
        parseUtmUpperLeft: function() {
            return this.parseUtm(this.get('utmUpperLeftEasting'), this.get('utmUpperLeftNorthing'), this.get('utmUpperLeftZone'), this.get('utmUpperLeftHemisphere'));
        },

        // Get the UTM Lower-Right bounding box fields in the internal format. See 'parseUtm'.
        parseUtmLowerRight: function() {
            return this.parseUtm(this.get('utmLowerRightEasting'), this.get('utmLowerRightNorthing'), this.get('utmLowerRightZone'), this.get('utmLowerRightHemisphere'));
        },

        // Get the UTM point radius fields in the internal format. See 'parseUtm'.
        parseUtmPointRadius: function() {
            return this.parseUtm(this.get('utmEasting'), this.get('utmNorthing'), this.get('utmZone'), this.get('utmHemisphere'));
        }


    });
});