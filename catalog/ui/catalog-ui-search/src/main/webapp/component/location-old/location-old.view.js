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
    'js/view/Progress.view',
    'wreqr',
    'text!./location-old.hbs',
    'maptype',
    'js/store',
    'js/CustomElements',
    './location-old',
    'bootstrapselect'
], function ($, Backbone, Marionette, _, properties, MetaCard, Progress, wreqr, template, maptype, store, CustomElements, LocationOldModel) {

   return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('location-old'),
        events: {
            'click #locationPoint': 'drawCircle',
            'click #locationPolygon': 'drawPolygon',
            'click #locationBbox': 'drawBbox',
            'click #latlon': 'swapLocationTypeLatLon',
            'click #usng': 'swapLocationTypeUsng',
            'change #radiusUnits': 'onRadiusUnitsChanged',
            'keydown input[id=radiusValue]': 'filterNonPositiveNumericValues'
        },
        initialize: function (options) {
            this.propertyModel = this.model;
            this.model = new LocationOldModel();
            _.bindAll(this);
            this.modelBinder = new Backbone.ModelBinder();
            this.deserialize();
            this.setupListeners();
        },
       setupListeners: function(){
           this.listenTo(this.propertyModel.get('property'), 'change:isEditing', this.handleEdit);
       },
       handleEdit: function(){
            if (this.propertyModel.get('property').get('isEditing')){
                this.edit();
            } else {
                this.readOnly();
            }
       },
       readOnly: function(){
           this.$el.find('label').attr('disabled', 'disabled');
           this.$el.find('input').attr('disabled', 'disabled');
           this.$el.find('select').attr('disabled', 'disabled');
           this.$el.find('form button').attr('disabled', 'disabled');
           this.$('#radiusUnits').multiselect('disable');
       },
       edit: function(){
           this.$el.addClass('is-editing');
           this.$el.find('label').removeAttr('disabled');
           this.$el.find('input').removeAttr('disabled');
           this.$el.find('select').removeAttr('disabled');
           this.$el.find('form button').removeAttr('disabled');
           this.$('#radiusUnits').multiselect('enable');
       },
       deserialize: function(){
           if (this.propertyModel){
               var filter = this.propertyModel.get('value');
               switch(filter.type){
                   case 'DWITHIN':
                       var pointText = filter.value.value.substring(6);
                       pointText = pointText.substring(0, pointText.length - 1);
                       latLon = pointText.split(' ');
                       this.model.set({
                           lat: latLon[1],
                           lon: latLon[0],
                           radius: filter.distance
                       });
                       wreqr.vent.trigger('search:circledisplay', this.model);
                   break;
                   case 'INTERSECTS':
                       var pointText = filter.value.value.substring(9);
                       pointText = pointText.substring(0, pointText.length - 2);
                       pointText = pointText.split(',');
                       var points = pointText.map(function(pairText){
                            return pairText.trim().split(' ').map(function(point){
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
                radius: undefined,
                bbox: undefined,
                polygon: undefined,
                usng: undefined,
                usngbb: undefined
            }, { unset: true });
            //wreqr.vent.trigger('search:drawstop');
            wreqr.vent.trigger('search:drawend', this.model);
        },
        swapLocationTypeLatLon: function () {
            this.model.set('locationType', 'latlon');
            this.updateLocationFields();
        },
        swapLocationTypeUsng: function () {
            this.model.set('locationType', 'usng');
            //this.model.setLatLon();
            this.updateLocationFields();
        },
        updateLocationFields: function () {
            if (this.model.get('locationType') === 'latlon') {
                //radius
                this.$('#latdiv').css('display', 'table');
                this.$('#londiv').css('display', 'table');
                this.$('#usngdiv').css('display', 'none');
                //bbox
                this.$('#westdiv').css('display', 'table');
                this.$('#southdiv').css('display', 'table');
                this.$('#eastdiv').css('display', 'table');
                this.$('#northdiv').css('display', 'table');
                this.$('#usngbbdiv').css('display', 'none');
            } else if (this.model.get('locationType') === 'usng') {
                //radius
                this.$('#latdiv').css('display', 'none');
                this.$('#londiv').css('display', 'none');
                this.$('#usngdiv').css('display', 'table');
                //bbox
                this.$('#westdiv').css('display', 'none');
                this.$('#southdiv').css('display', 'none');
                this.$('#eastdiv').css('display', 'none');
                this.$('#northdiv').css('display', 'none');
                this.$('#usngbbdiv').css('display', 'table');
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
                        var deltaThreshold = 0.0000001;
                        // same used in cesium.bbox.js
                        // only update the view's value if it's significantly different from the model's value
                        return Math.abs(currentValue - distanceFromMeters) > deltaThreshold ? distanceFromMeters : currentValue;
                }
            }, polygonConverter = function (direction, value) {
                if (value && direction === 'ViewToModel') {
                    return $.parseJSON(value);
                } else if (value && direction === 'ModelToView') {
                    var retVal = '[';
                    for (var i = 0; i < value.length; i++) {
                        var point = value[i];
                        retVal += '[' + point[0].toFixed(2) + ', ' + point[1].toFixed(2) + ']';
                        if (i < value.length - 1) {
                            retVal += ', ';
                        }
                    }
                    retVal += ']';
                    return retVal;
                }
            };
            var queryModelBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            queryModelBindings.radius.selector = '#radiusValue';
            queryModelBindings.radius.converter = radiusConverter;
            queryModelBindings.polygon.converter = polygonConverter;
            this.modelBinder.bind(this.model, this.$el, queryModelBindings, {
                changeTriggers: {
                    '': 'change dp.change',
                    'input[name=q]': 'input'
                }
            });
            this.delegateEvents();
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
            this.$('#radiusUnits').multiselect(singleselectOptions);
            this.updateLocationFields();
            this.handleEdit();
        },
        drawCircle: function () {
            this.clearLocation();
            wreqr.vent.trigger('search:drawcircle', this.model);
        },
        drawPolygon: function () {
            this.clearLocation();
            wreqr.vent.trigger('search:drawpoly', this.model);
        },
        drawBbox: function () {
            this.clearLocation();
            wreqr.vent.trigger('search:drawbbox', this.model);
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
       serializeData: function(){
           return this.model.toJSON({
               additionalProperties: ['cid']
           })
       },
       getCurrentValue: function(){
           var modelJSON = this.model.toJSON();
           var type;
           if (modelJSON.north && modelJSON.south && modelJSON.east && modelJSON.west) {
               type = 'BBOX';
           } else if (modelJSON.polygon) {
               type = 'POLYGON';
           } else if (modelJSON.lat && modelJSON.lon && modelJSON.radius) {
               type = 'POINTRADIUS'
           }
           return _.extend(this.model.toJSON(), {
               type: type
           });
       },
       onDestroy: function(){
           wreqr.vent.trigger('search:drawend', this.model);
       }
    });
});
