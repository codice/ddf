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
        'icanhaz',
        'properties',
        'js/model/Metacard',
        'js/view/Progress.view',
        'wreqr',
        'text!templates/search/searchForm.handlebars',
        'direction'
    ],
    function ($, Backbone, Marionette, _, ich, properties, MetaCard, Progress, wreqr, searchFormTemplate, dir) {
        "use strict";
        var Query = {};

        ich.addTemplate('searchFormTemplate', searchFormTemplate);

        Query.QueryView = Marionette.ItemView.extend({
            template: 'searchFormTemplate',
            className : 'slide-animate',

            events: {
                'click #searchButton': 'search',
                'click #workspaceSearchButton': 'workspaceSearch',
                'click #workspaceCancelButton': 'workspaceCancel',
                'click .resetButton': 'reset',
                'click .time': 'clearTime',
                'click .location': 'clearLocation',
                'click .type': 'clearType',
                'click button[name=pointRadiusButton]' : 'drawCircle',
                'click button[name=bboxButton]' : 'drawBbox',
                'click button[name=noFederationButton]' : 'setNoFederation',
                'click button[name=selectedFederationButton]': 'setSelectedFederation',
                'click button[name=enterpriseFederationButton]': 'setEnterpriseFederation',
                'click button[name=createdTimeButton]': 'swapTimeTypeCreated',
                'click button[name=modifiedTimeButton]': 'swapTimeTypeModified',
                'click button[name=effectiveTimeButton]': 'swapTimeTypeEffective',
                'keypress input[name=q]': 'filterOnEnter',
                'change #radiusUnits': 'onRadiusUnitsChanged',
                'change #offsetTimeUnits': 'onTimeUnitsChanged',
                'click button[name=notScheduledButton]': 'updateScheduling',
                'click button[name=scheduledButton]': 'updateScheduling',
                'click #saveButton': 'saveSearch'
            },

            modelEvents: {
                'change:bbox change:radius': 'updateZoomOnResults'
            },

            initialize: function (options) {
                _.bindAll(this);
                this.modelBinder = new Backbone.ModelBinder();

                // Assign each source id as both the HTML <option>'s
                // "value" attribute and the <option>'s text. Convert the
                // the source's "available" attribute to the <options>'s
                // "disabled" attribute.
                var sourcesBindings = {
                    id: [ { selector: '',
                            elAttribute: 'value'},
                          { selector: ''} ],
                    available: { selector: '',
                                 elAttribute: 'disabled',
                                 converter: function (direction, value) {
                                     return !value;
                               }}
                };

                var typesBindings = {
                    value: [ { selector: '',
                        elAttribute: 'value'
                        } ],
                    name: [ { selector: ''} ]
                };

                this.sourcesCollectionBinder = new Backbone.CollectionBinder(
                    new Backbone.CollectionBinder.ElManagerFactory(
                        '<option></option>', sourcesBindings));

                this.typesCollectionBinder = new Backbone.CollectionBinder(
                    new Backbone.CollectionBinder.ElManagerFactory(
                        '<option></option>', typesBindings));

                this.isWorkspace = options.isWorkspace;

                if (wreqr.reqres.hasHandler('workspace:getsources')) {
                    this.sources = wreqr.reqres.request('workspace:getsources');
                }
                if (wreqr.reqres.hasHandler('workspace:gettypes')) {
                    this.types = wreqr.reqres.request('workspace:gettypes');
                }
            },

            updateScheduling: function(e) {
                if(e.target.name === 'notScheduledButton') {
                    this.model.set({scheduled: false});
                } else if(e.target.name === 'scheduledButton') {
                    this.model.set({scheduled: true});
                }
                this.model.set({scheduleValue: undefined});
            },

            setNoFederation : function () {
                this.model.set('federation', 'local');
                this.model.set('src','local');
                this.updateScrollbar();
            },

            setEnterpriseFederation : function () {
                this.model.set('federation', 'enterprise');
                this.model.unset('src');
                this.updateScrollbar();
            },

            setSelectedFederation : function () {
                this.model.set('federation', 'selected');
                this.model.unset('src');
                this.updateScrollbar();
            },

            clearTime: function () {
                this.model.set({
                    dtstart: undefined,
                    dtend: undefined,
                    dtoffset: undefined,
                    offsetTime: undefined
                }, {unset: true});
                this.resetDateTimePicker('#absoluteStartTime');
                this.resetDateTimePicker('#absoluteEndTime');
                this.updateScrollbar();
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
                    bbox: undefined
                }, {unset: true});
                wreqr.vent.trigger("search:drawend");
                this.updateScrollbar();
            },

            clearType: function () {
                this.model.set({
                    type: undefined
                }, {unset: true});
                this.updateScrollbar();
            },

            swapTimeTypeCreated: function() {
                this.model.set("timeType", "created");
            },

            swapTimeTypeModified: function() {
                this.model.set("timeType", "modified");
            },

            swapTimeTypeEffective: function() {
                this.model.set("timeType", "effective");
            },

            updateScrollbar: function () {
                var view = this;
                // defer seems to be necessary for this to update correctly
                _.defer(function () {
                    view.$el.perfectScrollbar('update');
                });
            },

            serializeData: function () {
                var allSources, allTypes;
                if(this.sources) {
                    allSources = this.sources.toJSON();
                }
                if(this.types) {
                    allTypes = this.types.toJSON();
                }
                return _.extend(this.model.toJSON(), {types: allTypes, sources: allSources, isWorkspace: this.isWorkspace});
            },

            onRender: function () {
                var view = this;

                var radiusConverter = function (direction, value) {
                        var radiusUnitVal = view.model.get("radiusUnits");
                        var distanceFromMeters = view.getDistanceFromMeters(parseFloat(value, 10), radiusUnitVal);

                        if (direction === 'ModelToView') {
                            //radius value is bound to radius since radiusValue is converted, so we just need to set
                            //the value so that it shows up in the view
                            view.model.set("radiusValue", distanceFromMeters);
                        }

                        return distanceFromMeters;
                    },

                    offsetConverter = function (direction, value) {
                        if (direction !== 'ModelToView') {
                            //all we really need to do is just calculate the dtoffset on the model based on these two values
                            view.model.set("dtoffset", view.getTimeInMillis(view.$("input[name=offsetTime]").val(), view.$("select[name=offsetTimeUnits]").val()));
                        }
                        return value;
                    },
                    listConverter = function(direction,value){
                        // If there are multiple federated sources, the model wants
                        // a comma separated string for the list of federated sources.
                        // If there is only one federated source, the model wants
                        // just the souce name (no comma).  The join will only return
                        // a comma separated string if there are multiple federated
                        // source.  If there is only one federated source, the join
                        // will only return the federated source (without a comma).
                        if(value && direction === "ViewToModel"){
                            return value.join(',');
                        } else if (value && direction === "ModelToView") {
                            // The view wants an array.  We need to convert
                            // the string of federated sources from the model
                            // to an array.
                            return value.split(',');
                        }
                    };

                var queryModelBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                queryModelBindings.radius.selector = '[name=radiusValue]';
                queryModelBindings.radius.converter = radiusConverter;
                queryModelBindings.offsetTime.converter = offsetConverter;
                queryModelBindings.offsetTimeUnits.converter = offsetConverter;
                queryModelBindings.src = {};
                queryModelBindings.src.selector = '#federationSources';
                queryModelBindings.src.converter =  listConverter;
                queryModelBindings.type = {};
                queryModelBindings.type.selector = '#typeList';
                queryModelBindings.type.converter = listConverter;

                // ORDER MATTERS! The SourcesCollection must be bound prior to
                // the QueryModel so that the sources exist in the select list
                // before the model bindings attempt to select them.
                this.sourcesCollectionBinder.bind(this.sources, this.$('#federationSources'));
                this.typesCollectionBinder.bind(this.types, this.$('#typeList'));
                this.modelBinder.bind(this.model, this.$el, queryModelBindings);

                // Refresh the sources multiselect widget to reflect
                // changes when sources are added/removed or
                // modified (e.g., become available/unavailable)
                this.sources.bind('add change remove', function() {
                    $('#federationSources').multiselect("refresh");
                });

                this.types.bind('add change remove', function() {
                    $('#typeList').multiselect("refresh");
                });

                this.initDateTimePicker('#absoluteStartTime');
                this.initDateTimePicker('#absoluteEndTime');

                this.delegateEvents();

                var singleselectOptions = {
                    header: false,
                    minWidth: 110,
                    height: 185,
                    classes: 'add-on multiselect',
                    multiple: false,
                    selectedText: function(numChecked, numTotal, checkedItems){
                        if(checkedItems && checkedItems.length > 0) {
                            return checkedItems.pop().value;
                        }
                        return '';
                    }
                };

                var multiselectOptions = {
                    minWidth: 350,
                    height: 185,
                    classes: 'multiselect',
                    checkAllText: 'Select all',
                    uncheckAllText: 'Deselect all',
                    selectedText: function(numChecked, numTotal){
                        return numChecked + ' of ' + numTotal + ' selected';
                    }
                };

                var typeSelectOptions = _.clone(multiselectOptions);
                typeSelectOptions.minWidth = 350;
                typeSelectOptions.noneSelectedText = 'Select a Type';

                var federationSourcesSelectOptions = _.clone(multiselectOptions);
                federationSourcesSelectOptions.noneSelectedText = 'Select Sources';

                this.$('#typeList').multiselect(typeSelectOptions).multiselectfilter();

                this.$('#federationSources').multiselect(federationSourcesSelectOptions).multiselectfilter();

                this.$('#radiusUnits').multiselect(singleselectOptions);

                this.$('#offsetTimeUnits').multiselect(singleselectOptions);

                this.$('#scheduleUnits').multiselect(singleselectOptions);
            },

            beforeShowDatePicker: function(picker){
                picker.style.zIndex = 200;
            },

            drawCircle: function(){
                wreqr.vent.trigger("search:drawcircle", this.model);
            },

            drawBbox: function(){
                wreqr.vent.trigger("search:drawbbox", this.model);
            },

            onClose: function () {
                this.modelBinder.unbind();
                this.sourcesCollectionBinder.unbind();
            },

            filterOnEnter: function(e) {
                var view = this;
                if (e.keyCode === 13) {
                    // defer it to make sure the event to set the query parameter is run
                    if(this.isWorkspace) {
                        _.defer(function () {
                            view.workspaceSearch();
                        });
                    } else {
                        _.defer(function () {
                            view.search();
                        });
                    }
                }

            },

            updateZoomOnResults: function() {
                this.zoomOnResults = this.model.get("bbox") || this.model.get("radius") ? true : false;
            },

            workspaceSearch: function() {
                var queryName = this.$('#queryName').val();

                if(!queryName) {
                    return;
                }

                if (_.isUndefined(this.model.get('src'))) {
                    this.model.setSources(this.sources);
                }
                this.model.set({name: queryName});

                this.model.startSearch();

                wreqr.vent.trigger("search:drawstop");
                wreqr.vent.trigger('workspace:save', this.model);
                wreqr.vent.trigger('workspace:show');
            },

            workspaceCancel: function() {
                wreqr.vent.trigger('workspace:cancel', this.model);
            },

            saveSearch: function() {
                wreqr.vent.trigger('workspace:saveresults', this.model);
            },

            search: function () {
                var view = this;

                var progress = new Progress.ProgressModel();

                var progressFunction = function(val, model) {
                                            progress.update.call(progress, {value: val, model: model});
                                        };

                if (_.isUndefined(this.model.get('src'))) {
                    this.model.setSources(this.sources);
                }

                // disable the whole form
                this.$('button').addClass('disabled');
                this.$('input').prop('disabled',true);
                wreqr.vent.trigger("search:drawstop");

                this.model.clearSearch();
                this.model.startSearch(progressFunction).complete(function () {
                    //this is fired after cometd has acknowledged our query request
                    //re-enable the whole form
                    view.$('button').removeClass('disabled');
                    view.$('input').prop('disabled',false);
                }).success(function() {
                    //this is fired after cometd has sent back the first result
                    wreqr.vent.trigger('search:results', dir.forward, view.model.get('result'));
                    wreqr.vent.trigger('map:results', view.model.get('result'), view.zoomOnResults);
                });

                wreqr.vent.trigger('search:start', this.model, progress);

                wreqr.reqres.setHandler('search:results', function () {
                    return this.model.get('result');
                });
            },

            reset: function () {
                $('button[name=noTemporalButton]').click();
                $('button[name=noLocationButton]').click();
                $('button[name=noTypeButton]').click();
                $('button[name=enterpriseFederationButton]').click();
                this.model.clear();
                this.model.setDefaults();
                // Refresh the multiselect lists to reflect the changes
                // to the model
                $('#typeList').multiselect("refresh");
                $('#federationSources').multiselect("refresh");

                wreqr.vent.trigger('search:clear');
                wreqr.vent.trigger('map:clear');
                $('input[name=q]').focus();
                this.zoomOnResults = false;
            },

            onRadiusUnitsChanged: function () {
                this.$('input[name=radiusValue]').val(
                    this.getDistanceFromMeters(this.model.get('radius'), this.$('#radiusUnits').val()));
            },

            onTimeUnitsChanged : function(){
                var timeInMillis = this.getTimeInMillis(this.model.get('dtoffset'), this.$('#offsetTimeUnits').val());
                // silently set it so as not to trigger a modelbinder update
                this.model.set('dtoffset', timeInMillis, {silent:true});
            },

            getDistanceInMeters: function (distance, units) {
                distance = distance || 0;

                switch (units) {
                    case "meters":
                        return distance;
                    case "kilometers":
                        return distance * 1000;
                    case "feet":
                        return Math.ceil(distance * 0.3048);
                    case "yards":
                        return Math.ceil(distance * 0.9144);
                    case "miles":
                        return Math.ceil(distance * 1609.34);
                    default:
                        return distance;
                }
            },
            getDistanceFromMeters: function (distance, units) {
                distance = distance || 0;

                switch (units) {
                    case "meters":
                        return distance;
                    case "kilometers":
                        return distance / 1000;
                    case "feet":
                        return Math.ceil(distance / 0.3048);
                    case "yards":
                        return Math.ceil(distance / 0.9144);
                    case "miles":
                        return Math.ceil(distance / 1609.34);
                    default:
                        return distance;
                }
            },

            getTimeInMillis: function (val, units) {
                switch (units) {
                    case "seconds":
                        return val * 1000;
                    case "minutes":
                        return this.getTimeInMillis(val, 'seconds') * 60;
                    case "hours":
                        return this.getTimeInMillis(val, 'minutes') * 60;
                    case "days":
                        return this.getTimeInMillis(val, 'hours') * 24;
                    case "weeks":
                        return this.getTimeInMillis(val, 'days') * 7;
                    case "months":
                        return this.getTimeInMillis(val, 'weeks') * 4;
                    case "years" :
                        return this.getTimeInMillis(val, 'days') * 365;
                    default:
                        return val;
                }
            },

            resetDateTimePicker: function(selector) {
                this.$(selector).datetimepicker('destroy');
                this.initDateTimePicker(selector);

            },

            initDateTimePicker: function (selector) {
                this.$(selector).datetimepicker({
                    dateFormat: $.datepicker.ATOM,
                    timeFormat: "HH:mm:ss.lz",
                    separator: "T",
                    timezoneIso8601: true,
                    useLocalTimezone: true,
                    showHour: true,
                    showMinute: true,
                    showSecond: false,
                    showMillisec: false,
                    showTimezone: false,
                    minDate: new Date(100, 0, 2),
                    maxDate: new Date(9999, 11, 30),
                    onClose: this.model.swapDatesIfNeeded,
                    beforeShow: this.beforeShowDatePicker
            });
          }

        });

        return Query;

});


