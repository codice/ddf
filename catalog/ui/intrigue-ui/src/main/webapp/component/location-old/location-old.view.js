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
    'jquery',
    'backbone',
    'marionette',
    'underscore',
    'properties',
    'js/model/Metacard',
    'wreqr',
    './location-old.hbs',
    'maptype',
    'js/store',
    'js/CustomElements',
    './location-old',
    'js/CQLUtils'
], function ($, Backbone, Marionette, _, properties, MetaCard, wreqr, template, maptype,
             store, CustomElements, LocationOldModel, CQLUtils) {

    var minimumDifference = 0.0001;
    var minimumBuffer = 0.000001;
    var deltaThreshold = 0.0000001;

    var singleselectOptions = {
                                header: false,
                                minWidth: 110,
                                height: 185,
                                classes: 'input-group-addon multiselect',
                                multiple: false,
                                selectedText: function (numChecked, numTotal, checkedItems) {
                                    if (checkedItems && checkedItems.length > 0) {
                                        return checkedItems.pop().value;
                                    }
                                    return '';
                                }
                            };
    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('location-old'),
        events: {
            'click #locationPoint': 'drawCircle',
            'click #locationPolygon': 'drawPolygon',
            'click #locationBbox': 'drawBbox',
            'click #locationLine': 'drawLine',
            'click #latlon': 'swapLocationTypeLatLon',
            'click #latlon': 'swapLocationTypeLatLon',
            'click #usng': 'swapLocationTypeUsng',
            'click #utm': 'swapLocationTypeUtm',
            'change #radiusUnits': 'onRadiusUnitsChanged',
            'change #lineUnits': 'onLineUnitsChanged'
        },
        initialize: function (options) {
            this.propertyModel = this.model;
            this.model = new LocationOldModel();
            _.bindAll(this);
            this.modelBinder = new Backbone.ModelBinder();
            this.deserialize();
            this.setupListeners();
            this.handleCurrentMode();
        },
        handleCurrentMode: function(){
            this.$el.toggleClass('is-line', Boolean(this.model.get('line')));
            this.$el.toggleClass('is-polygon', Boolean(this.model.get('polygon')));
            this.$el.toggleClass('is-circle', Boolean(this.model.get('lat')));
            this.$el.toggleClass('is-bbox', Boolean(this.model.get('bbox')));
        },
        changeMode: function(mode){
            this.$el.toggleClass('is-line', mode === "line");
            this.$el.toggleClass('is-polygon', mode === "polygon");
            this.$el.toggleClass('is-circle', mode === "circle");
            this.$el.toggleClass('is-bbox', mode === "bbox");
        },
        setupListeners: function () {
            this.listenTo(this.propertyModel.get('property'), 'change:isEditing', this.handleEdit);
            this.listenTo(this.model, 'change:mapNorth change:mapSouth change:mapEast change:mapWest', this.updateMaxAndMin);
            this.listenTo(this.model, 'change', function(attrs){
                this.modelBinder.copyModelAttributesToView(attrs);
            }.bind(this));
        },
        updateMaxAndMin: function(){
            this.$el.find('#mapWest').attr('max', parseFloat(this.model.get('mapEast')) - minimumDifference);
            this.$el.find('#mapEast').attr('min', parseFloat(this.model.get('mapWest')) + minimumDifference);
            this.$el.find('#mapNorth').attr('min', parseFloat(this.model.get('mapSouth')) + minimumDifference);
            this.$el.find('#mapSouth').attr('max', parseFloat(this.model.get('mapNorth')) - minimumDifference);
            this.model.setLatLon();
        },
        handleEdit: function () {
            if (this.propertyModel.get('property').get('isEditing')) {
                this.edit();
            } else {
                this.readOnly();
            }
        },
        readOnly: function () {
            this.$el.find('label').attr('disabled', 'disabled');
            this.$el.find('input').attr('disabled', 'disabled');
            this.$el.find('select').attr('disabled', 'disabled');
            this.$el.find('form button').attr('disabled', 'disabled');
            this.$('#radiusUnits').multiselect('disable');
            this.$('#lineUnits').multiselect('disable');
            this.$('#utmZone').multiselect('disable');
            this.$('#utmUpperLeftZone').multiselect('disable');
            this.$('#utmLowerRightZone').multiselect('disable');
            this.$('#utmHemisphere').multiselect('disable');
            this.$('#utmUpperLeftHemisphere').multiselect('disable');
            this.$('#utmLowerRightHemisphere').multiselect('disable');
         },
        edit: function () {
            this.$el.addClass('is-editing');
            this.$el.find('label').removeAttr('disabled');
            this.$el.find('input').removeAttr('disabled');
            this.$el.find('select').removeAttr('disabled');
            this.$el.find('form button').removeAttr('disabled');
            this.$('#radiusUnits').multiselect('enable');
            this.$('#lineUnits').multiselect('enable');
            this.$('#utmZone').multiselect('enable');
            this.$('#utmUpperLeftZone').multiselect('enable');
            this.$('#utmLowerRightZone').multiselect('enable');
            this.$('#utmHemisphere').multiselect('enable');
            this.$('#utmUpperLeftHemisphere').multiselect('enable');
            this.$('#utmLowerRightHemisphere').multiselect('enable');
        },
        deserialize: function () {
            if (this.propertyModel) {
                var filter = this.propertyModel.get('value');
                switch (filter.type) {
                    case 'DWITHIN':
                        if (CQLUtils.isPointRadiusFilter(filter)){
                            var pointText = filter.value.value.substring(6);
                            pointText = pointText.substring(0, pointText.length - 1);
                            var latLon = pointText.split(' ');
                            this.model.set({
                                lat: latLon[1],
                                lon: latLon[0],
                                radius: filter.distance
                            });
                            wreqr.vent.trigger('search:circledisplay', this.model);
                        } else {
                            var pointText = filter.value.value.substring(11);
                            pointText = pointText.substring(0, pointText.length - 1);
                            this.model.set({
                                lineWidth: filter.distance,
                                line: pointText.split(',').map(function(coordinate){
                                    return coordinate.split(' ').map(function(value){
                                        return Number(value)
                                    });
                                })
                            });
                            wreqr.vent.trigger('search:linedisplay', this.model);
                        }
                        break;
                    case 'INTERSECTS':
                        var pointText = filter.value.value.substring(9);
                        pointText = pointText.substring(0, pointText.length - 2);
                        pointText = pointText.split(',');
                        var points = pointText.map(function (pairText) {
                            return pairText.trim().split(' ').map(function (point) {
                                return Number(point);
                            });
                        });
                        this.model.set({
                            polygon: points
                        });
                        wreqr.vent.trigger('search:polydisplay', this.model);
                        break;
                }
            }
        },
        filterNonPositiveNumericValues: function (e) {
            var code = e.keyCode ? e.keyCode : e.which;
            if ((code < 48 || code > 57) && //digits
                code !== 190 && //period
                code !== 8 && //backspace
                code !== 46 && (code < 37 || code > 40) && (code < 96 || code > 103))
            //numberpad
            {
                e.preventDefault();
                e.stopPropagation();
            }
        },
        clearLocation: function () {
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
            //wreqr.vent.trigger('search:drawstop');
            wreqr.vent.trigger('search:drawend', this.model);
            this.$el.trigger('change');
        },
        swapLocationTypeLatLon: function () {
            this.model.set('locationType', 'latlon');
            this.updateLocationFields();
        },
        swapLocationTypeUsng: function () {
            this.model.set('locationType', 'usng');
            this.updateLocationFields();
        },
        swapLocationTypeUtm: function () {
            this.model.set('locationType', 'utm');
            this.updateLocationFields();
        },
        updateLocationFields: function () {
            if (this.model.get('locationType') === 'latlon') {
                //radius
                this.$('#latdiv').css('display', 'table');
                this.$('#londiv').css('display', 'table');
                this.$('#usngdiv').css('display', 'none');
                this.$('#utmdivEasting').css('display', 'none');
                this.$('#utmdivNorthing').css('display', 'none');
                this.$('#utmdivZone').css('display', 'none');
                this.$('#utmdivHemisphere').css('display', 'none');
                //bbox
                this.$('#westdiv').css('display', 'table');
                this.$('#southdiv').css('display', 'table');
                this.$('#eastdiv').css('display', 'table');
                this.$('#northdiv').css('display', 'table');
                this.$('#usngbbdiv').css('display', 'none');
                this.$('#utmuldiv').css('display', 'none');
                this.$('#utmuldivEasting').css('display', 'none');
                this.$('#utmuldivNorthing').css('display', 'none');
                this.$('#utmuldivHemisphere').css('display', 'none');
                this.$('#utmuldivZone').css('display', 'none');
                this.$('#utmlrdiv').css('display', 'none');
                this.$('#utmlrdivEasting').css('display', 'none');
                this.$('#utmlrdivNorthing').css('display', 'none');
                this.$('#utmlrdivHemisphere').css('display', 'none');
                this.$('#utmlrdivZone').css('display', 'none');
            } else if (this.model.get('locationType') === 'usng') {
                //radius
                this.$('#latdiv').css('display', 'none');
                this.$('#londiv').css('display', 'none');
                this.$('#usngdiv').css('display', 'table');
                this.$('#utmdivEasting').css('display', 'none');
                this.$('#utmdivNorthing').css('display', 'none');
                this.$('#utmdivZone').css('display', 'none');
                this.$('#utmdivHemisphere').css('display', 'none');
                //bbox
                this.$('#westdiv').css('display', 'none');
                this.$('#southdiv').css('display', 'none');
                this.$('#eastdiv').css('display', 'none');
                this.$('#northdiv').css('display', 'none');
                this.$('#usngbbdiv').css('display', 'table');
                this.$('#utmuldiv').css('display', 'none');
                this.$('#utmuldivEasting').css('display', 'none');
                this.$('#utmuldivNorthing').css('display', 'none');
                this.$('#utmuldivHemisphere').css('display', 'none');
                this.$('#utmuldivZone').css('display', 'none');
                this.$('#utmlrdiv').css('display', 'none');
                this.$('#utmlrdivEasting').css('display', 'none');
                this.$('#utmlrdivNorthing').css('display', 'none');
                this.$('#utmlrdivHemisphere').css('display', 'none');
                this.$('#utmlrdivZone').css('display', 'none');
            } else if (this.model.get('locationType') === 'utm') {
                //radius
                this.$('#latdiv').css('display', 'none');
                this.$('#londiv').css('display', 'none');
                this.$('#usngdiv').css('display', 'none');
                this.$('#utmdivEasting').css('display', 'table');
                this.$('#utmdivNorthing').css('display', 'table');
                this.$('#utmdivZone').css('display', 'table');
                this.$('#utmdivHemisphere').css('display', 'table');
                //bbox
                this.$('#westdiv').css('display', 'none');
                this.$('#southdiv').css('display', 'none');
                this.$('#eastdiv').css('display', 'none');
                this.$('#northdiv').css('display', 'none');
                this.$('#usngbbdiv').css('display', 'none');
                this.$('#utmuldiv').css('display', 'table');
                this.$('#utmuldivEasting').css('display', 'table');
                this.$('#utmuldivNorthing').css('display', 'table');
                this.$('#utmuldivHemisphere').css('display', 'table');
                this.$('#utmuldivZone').css('display', 'table');
                this.$('#utmlrdiv').css('display', 'table');
                this.$('#utmlrdivEasting').css('display', 'table');
                this.$('#utmlrdivNorthing').css('display', 'table');
                this.$('#utmlrdivHemisphere').css('display', 'table');
                this.$('#utmlrdivZone').css('display', 'table');
            }
        },
        serializeData: function () {
            return _.extend(this.model.toJSON(), {
                is3D: maptype.is3d(),
                is2D: maptype.is2d()
            });
        },
        onRender: function () {
            var view = this;
            var radiusConverter = function (direction, value) {
                var radiusUnitVal = view.model.get('radiusUnits');
                switch (direction) {
                    case 'ViewToModel':
                        var distanceInMeters = view.getDistanceInMeters(value, radiusUnitVal);
                        //radius value is bound to radius since radiusValue is converted, so we just need to set
                        //the value so that it shows up in the view
                        view.model.set('radius', distanceInMeters);
                        return distanceInMeters;
                    case 'ModelToView':
                        var distanceFromMeters = view.getDistanceFromMeters(view.model.get('radius'), radiusUnitVal);
                        var currentValue = this.boundEls[0].value;
                        // same used in cesium.bbox.js
                        // only update the view's value if it's significantly different from the model's value or is <= minimumBuffer (min for cql)
                        return (Math.abs((currentValue - distanceFromMeters)) > deltaThreshold) || currentValue <= minimumBuffer ? distanceFromMeters : currentValue;
                }
            }, lineWidthConverter = function (direction, value) {
                var lineUnitVal = view.model.get('lineUnits');
                switch (direction) {
                    case 'ViewToModel':
                        var distanceInMeters = view.getDistanceInMeters(value, lineUnitVal);
                        //radius value is bound to radius since radiusValue is converted, so we just need to set
                        //the value so that it shows up in the view
                        view.model.set('lineWidth', distanceInMeters);
                        return distanceInMeters;
                    case 'ModelToView':
                        var distanceFromMeters = view.getDistanceFromMeters(view.model.get('lineWidth'), lineUnitVal);
                        var currentValue = this.boundEls[0].value;
                        // same used in cesium.bbox.js
                        // only update the view's value if it's significantly different from the model's value or is <= minimumBuffer (min for cql)
                        return (Math.abs((currentValue - distanceFromMeters)) > deltaThreshold) || currentValue <= minimumBuffer ? distanceFromMeters : currentValue;
                }
            }, polygonConverter = function (direction, value) {
                if (value !== undefined && direction === 'ViewToModel') {
                    return $.parseJSON(value);
                } else if (value !== undefined && direction === 'ModelToView') {
                    var retVal = '[';
                    for (var i = 0; i < value.length; i++) {
                        var point = value[i];
                        retVal += '[' + point[0] + ', ' + point[1] + ']';
                        if (i < value.length - 1) {
                            retVal += ', ';
                        }
                    }
                    retVal += ']';
                    return retVal;
                }
            }, utmZoneConverter = function (direction, value) {
                if (direction === 'ModelToView') {
                    this.$('#utmZone').multiselect(singleselectOptions);
                }
                return value;
            }.bind(this), utmHemisphereConverter = function (direction, value) {
                if (direction === 'ModelToView') {
                    this.$('#utmHemisphere').multiselect(singleselectOptions);
                }
                return value;
            }.bind(this), utmUpperLeftZoneConverter = function (direction, value) {
                if (direction === 'ModelToView') {
                    this.$('#utmUpperLeftZone').multiselect(singleselectOptions);
                }
                return value;
            }.bind(this), utmUpperLeftHemisphereConverter = function (direction, value) {
                if (direction === 'ModelToView') {
                    this.$('#utmUpperLeftHemisphere').multiselect(singleselectOptions);
                }
                return value;
            }.bind(this), utmLowerRightZoneConverter = function (direction, value) {
                if (direction === 'ModelToView') {
                    this.$('#utmLowerRightZone').multiselect(singleselectOptions);
                }
                return value;
            }.bind(this), utmLowerRightHemisphereConverter = function (direction, value) {
                if (direction === 'ModelToView') {
                    this.$('#utmLowerRightHemisphere').multiselect(singleselectOptions);
                }
                return value;
            }.bind(this);

            var queryModelBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            queryModelBindings.lineWidth.selector = '#lineWidthValue';
            queryModelBindings.lineWidth.converter = lineWidthConverter;
            queryModelBindings.radius.selector = '#radiusValue';
            queryModelBindings.radius.converter = radiusConverter;
            queryModelBindings.polygon.converter = polygonConverter;
            queryModelBindings.line.converter = polygonConverter;
            queryModelBindings.utmZone.converter = utmZoneConverter;
            queryModelBindings.utmHemisphere.converter = utmHemisphereConverter;
            queryModelBindings.utmUpperLeftZone.converter = utmUpperLeftZoneConverter;
            queryModelBindings.utmUpperLeftHemisphere.converter = utmUpperLeftHemisphereConverter;
            queryModelBindings.utmLowerRightZone.converter = utmLowerRightZoneConverter;
            queryModelBindings.utmLowerRightHemisphere.converter = utmLowerRightHemisphereConverter;
            this.modelBinder.bind(this.model, this.$el, queryModelBindings, {
                changeTriggers: {
                    '': 'change dp.change',
                    'input[name=q]': 'input'
                }
            });
            this.delegateEvents();
            this.$('#radiusUnits').multiselect(singleselectOptions);
            this.$('#lineUnits').multiselect(singleselectOptions);
            this.$('#utmUpperLeftZone').multiselect(singleselectOptions);
            this.$('#utmLowerRightZone').multiselect(singleselectOptions);
            this.$('#utmUpperLeftHemisphere').multiselect(singleselectOptions);
            this.$('#utmLowerRightHemisphere').multiselect(singleselectOptions);
            this.$('#utmZone').multiselect(singleselectOptions);
            this.$('#utmHemisphere').multiselect(singleselectOptions);

            this.blockMultiselectEvents();
            this.updateLocationFields();
            this.handleEdit();
        },
        blockMultiselectEvents: function () {
            $('.ui-multiselect-menu').on('mousedown', function (e) {
                e.stopPropagation();
            });
        },
        drawLine: function () {
            this.clearLocation();
            wreqr.vent.trigger('search:drawline', this.model);
            this.changeMode("line");
        },
        drawCircle: function () {
            this.clearLocation();
            wreqr.vent.trigger('search:drawcircle', this.model);
            this.changeMode("circle");
        },
        drawPolygon: function () {
            this.clearLocation();
            wreqr.vent.trigger('search:drawpoly', this.model);
            this.changeMode("polygon");
        },
        drawBbox: function () {
            this.clearLocation();
            wreqr.vent.trigger('search:drawbbox', this.model);
            this.changeMode("bbox");
        },
        onLineUnitsChanged: function () {
            this.$('#lineWidthValue').val(this.getDistanceFromMeters(this.model.get('lineWidth'), this.$('#lineUnits').val()));
        },
        onRadiusUnitsChanged: function () {
            this.$('#radiusValue').val(this.getDistanceFromMeters(this.model.get('radius'), this.$('#radiusUnits').val()));
        },
        getDistanceInMeters: function (distance, units) {
            distance = distance || 0;
            switch (units) {
                case 'meters':
                    return distance;
                case 'kilometers':
                    return distance * 1000;
                case 'feet':
                    return distance * 0.3048;
                case 'yards':
                    return distance * 0.9144;
                case 'miles':
                    return distance * 1609.34;
                default:
                    return distance;
            }
        },
        getDistanceFromMeters: function (distance, units) {
            distance = distance || 0;
            switch (units) {
                case 'meters':
                    return distance;
                case 'kilometers':
                    return distance / 1000;
                case 'feet':
                    return distance / 0.3048;
                case 'yards':
                    return distance / 0.9144;
                case 'miles':
                    return distance / 1609.34;
                default:
                    return distance;
            }
        },
        serializeData: function () {
            return this.model.toJSON({
                additionalProperties: ['cid']
            })
        },
        getCurrentValue: function () {
            var modelJSON = this.model.toJSON();
            var type;
            if (modelJSON.north !== undefined && modelJSON.south !== undefined && modelJSON.east !== undefined && modelJSON.west !== undefined) {
                type = 'BBOX';
            } else if (modelJSON.polygon !== undefined) {
                type = 'POLYGON';
            } else if (modelJSON.lat !== undefined && modelJSON.lon !== undefined && (modelJSON.radius !== undefined)) {
                type = 'POINTRADIUS'
            } else if (modelJSON.line !== undefined && (modelJSON.lineWidth !== undefined)) {
                type = 'LINE';
            }

            return _.extend(modelJSON, {
                type: type,
                lineWidth: Math.max(modelJSON.lineWidth, minimumBuffer),
                radius: Math.max(modelJSON.radius, minimumBuffer)
            });
        },
        onDestroy: function () {
            wreqr.vent.trigger('search:drawend', this.model);
        }
    });
});
