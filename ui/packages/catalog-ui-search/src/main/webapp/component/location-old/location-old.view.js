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

const { reactToMarionette } = require('component/transmute');
const LocationView = reactToMarionette(require('react-component/location'));

if (process.env.NODE_ENV !== 'production') {
    module.hot.accept('react-component/location', () => {
        LocationView.reload(require('react-component/location'));
    });
}

const Marionette = require('marionette');
const _ = require('underscore');
const wreqr = require('wreqr');
const store = require('js/store');
const CustomElements = require('js/CustomElements');
const LocationOldModel = require('./location-old');
const CQLUtils = require('js/CQLUtils');
const ShapeUtils = require('js/ShapeUtils');

const minimumDifference = 0.0001;
const minimumBuffer = 0.000001;

module.exports = Marionette.LayoutView.extend({
    template: () => `<div class="location-input"></div>`,
    tagName: CustomElements.register('location-old'),
    regions: {
        location: '.location-input'
    },
    initialize: function(options) {
        this.propertyModel = this.model;
        this.model = new LocationOldModel();
        _.bindAll.apply(_, [this].concat(_.functions(this))); // underscore bindAll does not take array arg
        this.deserialize();
        this.setupListeners();
        this.listenTo(this.model, 'change', this.updateMap);
        this.listenTo(this.model, 'change:polygon', () => {
            if (this.model.get('mode') !== 'poly') {
                wreqr.vent.trigger('search:polydisplay', this.model);
            }
        });
        this.listenTo(this.model, 'change:mode', () => {
            this.clearLocation();
        });
    },
    // Updates the map with a drawing whenever the user is entering coordinates manually
    updateMap: function() {
        if (!this.isDestroyed) {
            var mode = this.model.get('mode');
            if (mode !== undefined && store.get('content').get('drawing') !== true) {
                wreqr.vent.trigger('search:' + mode + 'display', this.model);
            }
        }
    },
    setupListeners: function() {
        this.listenTo(
            this.model,
            'change:mapNorth change:mapSouth change:mapEast change:mapWest',
            this.updateMaxAndMin
        );
    },
    updateMaxAndMin: function() {
        this.model.setLatLon();
    },
    deserialize: function() {
        if (this.propertyModel) {
            var filter = this.propertyModel.get('value');
            switch (filter.type) {
                // these cases are for when the model matches the filter model
                case 'DWITHIN':
                    if (CQLUtils.isPointRadiusFilter(filter)) {
                        var pointText = filter.value.value.substring(6);
                        pointText = pointText.substring(0, pointText.length - 1);
                        var latLon = pointText.split(' ');
                        this.model.set({
                            mode: 'circle',
                            locationType: 'latlon',
                            lat: latLon[1],
                            lon: latLon[0],
                            radius: filter.distance
                        });
                        wreqr.vent.trigger('search:circledisplay', this.model);
                    } else {
                        var pointText = filter.value.value.substring(11);
                        pointText = pointText.substring(0, pointText.length - 1);
                        this.model.set({
                            mode: 'line',
                            lineWidth: filter.distance,
                            line: pointText.split(',').map(function(coordinate) {
                                return coordinate.split(' ').map(function(value) {
                                    return Number(value);
                                });
                            })
                        });
                        wreqr.vent.trigger('search:linedisplay', this.model);
                    }
                    break;
                case 'INTERSECTS':
                    var filterValue =
                        typeof filter.value === 'object' ? filter.value.value : filter.value;
                    if (!filterValue || typeof filterValue !== 'string') {
                        break;
                    }
                    this.model.set({
                        mode: 'poly',
                        polygon: CQLUtils.arrayFromPolygonWkt(filterValue)
                    });
                    wreqr.vent.trigger('search:polydisplay', this.model);
                    break;
                // these cases are for when the model matches the location model
                case 'BBOX':
                    this.model.set({
                        mode: 'bbox',
                        locationType: 'latlon',
                        north: filter.north,
                        south: filter.south,
                        east: filter.east,
                        west: filter.west
                    });
                    wreqr.vent.trigger('search:bboxdisplay', this.model);
                    break;
                case 'MULTIPOLYGON':
                case 'POLYGON':
                    this.model.set({
                        mode: 'poly',
                        polygon: filter.polygon,
                    });
                    wreqr.vent.trigger('search:polydisplay', this.model);
                    break;
                case 'POINTRADIUS':
                    this.model.set({
                        mode: 'circle',
                        locationType: 'latlon',
                        lat: filter.lat,
                        lon: filter.lon,
                        radius: filter.radius
                    });
                    wreqr.vent.trigger('search:circledisplay', this.model);
                    break;
                case 'LINE':
                    this.model.set({
                        mode: 'line',
                        line: filter.line,
                        lineWidth: filter.lineWidth
                    });
                    wreqr.vent.trigger('search:linedisplay', this.model);
                    break;
            }
        }
    },
    clearLocation: function() {
        this.model.set({
            north: undefined,
            east: undefined,
            west: undefined,
            south: undefined,
            lat: undefined,
            lon: undefined,
            radius: 1,
            bbox: undefined,
            polygon: undefined,
            hasKeyword: false,
            usng: undefined,
            usngbb: undefined,
            utmEasting: undefined,
            utmNorthing: undefined,
            utmZone: 1,
            utmHemisphere: 'Northern',
            utmUpperLeftEasting: undefined,
            utmUpperLeftNorthing: undefined,
            utmUpperLeftZone: 1,
            utmUpperLeftHemisphere: 'Northern',
            utmLowerRightEasting: undefined,
            utmLowerRightNorthing: undefined,
            utmLowerRightZone: 1,
            utmLowerRightHemisphere: 'Northern',
            line: undefined,
            lineWidth: 1
        });
        wreqr.vent.trigger('search:drawend', this.model);
        this.$el.trigger('change');
    },
    onRender: function() {
        this.location.show(
            new LocationView({
                model: this.model,
                onDraw: (drawingType) => {
                    wreqr.vent.trigger('search:draw' + this.model.get('mode'), this.model);
                }
            })
        );
    },
    getCurrentValue: function() {
        var modelJSON = this.model.toJSON();
        var type;
        if (modelJSON.polygon !== undefined) {
            type = ShapeUtils.isArray3D(modelJSON.polygon) ? 'MULTIPOLYGON' : 'POLYGON';
        } else if (
            modelJSON.lat !== undefined &&
            modelJSON.lon !== undefined &&
            modelJSON.radius !== undefined
        ) {
            type = 'POINTRADIUS';
        } else if (modelJSON.line !== undefined && modelJSON.lineWidth !== undefined) {
            type = 'LINE';
        } else if (
            modelJSON.north !== undefined &&
            modelJSON.south !== undefined &&
            modelJSON.east !== undefined &&
            modelJSON.west !== undefined
        ) {
            type = 'BBOX';
        }

        return _.extend(modelJSON, {
            type: type,
            lineWidth: Math.max(modelJSON.lineWidth, minimumBuffer),
            radius: Math.max(modelJSON.radius, minimumBuffer)
        });
    },
    onDestroy: function() {
        wreqr.vent.trigger('search:drawend', this.model);
    },
    isValid: function(){
        return this.getCurrentValue().type != undefined;
    }
});
