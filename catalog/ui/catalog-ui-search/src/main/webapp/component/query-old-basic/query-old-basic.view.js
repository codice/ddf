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
    'cesium',
    'marionette',
    'underscore',
    'properties',
    'js/model/Metacard',
    'js/view/Progress.view',
    'wreqr',
    'text!./query-old-basic.hbs',
    'direction',
    'maptype',
    'js/store',
    'js/CustomElements',
    'bootstrapselect'
], function ($, Backbone, Cesium, Marionette, _, properties, MetaCard, Progress, wreqr, queryOldBasicTemplate, dir, maptype, store, CustomElements) {

    var QueryOldBasicView = Marionette.ItemView.extend({
        template: queryOldBasicTemplate,
        tagName: CustomElements.register('query-old-basic'),
        events: {
            'click .cancel': 'cancel',
            'click .edit': 'edit',
            'click .save': 'save',
            'click .time': 'clearTime',
            'click .location': 'clearLocation',
            'click .type': 'clearType',
            'click #locationPoint': 'drawCircle',
            'click #locationPolygon': 'drawPolygon',
            'click #locationBbox': 'drawBbox',
            'click #locationAny': 'notDrawing',
            'click #federationNo': 'setNoFederation',
            'click #federationSelected': 'setSelectedFederation',
            'click #federationAll': 'setEnterpriseFederation',
            'click #created': 'swapTimeTypeCreated',
            'click #modified': 'swapTimeTypeModified',
            'click #effective': 'swapTimeTypeEffective',
            'click #latlon': 'swapLocationTypeLatLon',
            'click #usng': 'swapLocationTypeUsng',
            'keypress input[name=q]': 'filterOnEnter',
            'change #radiusUnits': 'onRadiusUnitsChanged',
            'change #offsetTimeUnits': 'onTimeUnitsChanged',
            'change #sortOrderSelected': 'onSortOrderChanged',
            'click #scheduledNo': 'updateScheduling',
            'click #scheduledYes': 'updateScheduling',
            'keydown input[name=offsetTime]': 'filterNonPositiveNumericValues',
            'keydown input[id=radiusValue]': 'filterNonPositiveNumericValues'
        },
        modelEvents: {
            'change:bbox change:radius change:polygon': 'updateZoomOnResults',
            'searchCleared': 'onSearchCleared'
        },
        initialize: function (options) {
            _.bindAll(this);
            this.modelBinder = new Backbone.ModelBinder();
            // Assign each source id as both the HTML <option>'s
            // "value" attribute and the <option>'s text. Convert the
            // the source's "available" attribute to the <options>'s
            // "disabled" attribute.
            var sourcesBindings = {
                id: [
                    {
                        selector: '',
                        elAttribute: 'value'
                    },
                    { selector: '' }
                ],
                available: {
                    selector: '',
                    elAttribute: 'disabled',
                    converter: function (direction, value) {
                        return !value;
                    }
                }
            };
            var typesBindings = {
                value: [{
                    selector: '',
                    elAttribute: 'name'
                }],
                name: [{ selector: '' }]
            };
            this.sourcesCollectionBinder = new Backbone.CollectionBinder(new Backbone.CollectionBinder.ElManagerFactory('<option></option>', sourcesBindings));
            this.typesCollectionBinder = new Backbone.CollectionBinder(new Backbone.CollectionBinder.ElManagerFactory('<option></option>', typesBindings));
            this.isWorkspace = options.isWorkspace;
            this.sources = store.get('sources');
            this.types = this.sources.types();
        },
        updateScheduling: function (e) {
            if (e.target.name === 'notScheduledButton') {
                this.model.set({ scheduled: false });
            } else if (e.target.name === 'scheduledButton') {
                this.model.set({ scheduled: true });
            }
            this.model.set({ scheduleValue: undefined });
        },
        setNoFederation: function () {
            this.model.set('src', 'local');
            this.model.set('federation', 'local');
            this.updateScrollbar();
        },
        setEnterpriseFederation: function () {
            this.model.unset('src');
            this.model.set('federation', 'enterprise');
            this.updateScrollbar();
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
        setSelectedFederation: function () {
            if (this.model.get('src') === 'local') {
                this.model.unset('src');
            }
            this.model.set('federation', 'selected');
            this.updateScrollbar();
        },
        clearTime: function () {
            this.model.set({
                dtstart: undefined,
                dtend: undefined,
                dtoffset: undefined,
                offsetTime: undefined
            }, { unset: true });
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
                bbox: undefined,
                polygon: undefined,
                usng: undefined,
                usngbb: undefined
            }, { unset: true });
            wreqr.vent.trigger('search:drawstop');
            wreqr.vent.trigger('search:drawend');
            this.updateScrollbar();
        },
        clearType: function () {
            this.model.set({ type: undefined }, { unset: true });
            this.updateScrollbar();
        },
        swapTimeTypeCreated: function () {
            this.model.set('timeType', 'created');
        },
        swapTimeTypeModified: function () {
            this.model.set('timeType', 'modified');
        },
        swapTimeTypeEffective: function () {
            this.model.set('timeType', 'effective');
        },
        swapLocationTypeLatLon: function () {
            this.model.set('locationType', 'latlon');
            this.updateLocationFields();
        },
        swapLocationTypeUsng: function () {
            this.model.set('locationType', 'usng');
            this.model.setLatLon();
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
        updateScrollbar: function () {
        },
        serializeData: function () {
            var allSources, allTypes;
            if (this.sources) {
                allSources = this.sources.toJSON();
            }
            if (this.types) {
                allTypes = this.types.toJSON();
            }
            return _.extend(this.model.toJSON(), {
                types: allTypes,
                sources: allSources,
                isWorkspace: this.isWorkspace,
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
                        var deltaThreshold = Cesium.Math.EPSILON7;
                        // same used in cesium.bbox.js
                        // only update the view's value if it's significantly different from the model's value
                        return Math.abs(currentValue - distanceFromMeters) > deltaThreshold ? distanceFromMeters : currentValue;
                }
            }, offsetConverter = function (direction, value) {
                if (direction !== 'ModelToView') {
                    //all we really need to do is just calculate the dtoffset on the model based on these two values
                    view.model.set('dtoffset', view.getTimeInMillis(view.$('input[name=offsetTime]').val(), view.$('select[name=offsetTimeUnits]').val()));
                }
                return value;
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
            }, listConverter = function (direction, value) {
                // If there are multiple federated sources, the model wants
                // a comma separated string for the list of federated sources.
                // If there is only one federated source, the model wants
                // just the souce name (no comma).  The join will only return
                // a comma separated string if there are multiple federated
                // source.  If there is only one federated source, the join
                // will only return the federated source (without a comma).
                if (value && direction === 'ViewToModel') {
                    return value.join(',');
                } else if (value && direction === 'ModelToView') {
                    // The view wants an array.  We need to convert
                    // the string of federated sources from the model
                    // to an array.
                    return value.split(',');
                }
            };
            var queryModelBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            queryModelBindings.radius.selector = '#radiusValue';
            queryModelBindings.radius.converter = radiusConverter;
            queryModelBindings.offsetTime.converter = offsetConverter;
            queryModelBindings.offsetTimeUnits.converter = offsetConverter;
            queryModelBindings.src = {};
            queryModelBindings.src.selector = '#federationSources';
            queryModelBindings.src.converter = listConverter;
            queryModelBindings.type = {};
            queryModelBindings.type.selector = '#typeList';
            queryModelBindings.type.converter = listConverter;
            queryModelBindings.polygon.converter = polygonConverter;
            // ORDER MATTERS! The SourcesCollection must be bound prior to
            // the QueryModel so that the sources exist in the select list
            // before the model bindings attempt to select them.
            this.sourcesCollectionBinder.bind(this.sources, this.$('#federationSources'));
            this.typesCollectionBinder.bind(this.types, this.$('#typeList'));
            this.modelBinder.bind(this.model, this.$el, queryModelBindings, {
                changeTriggers: {
                    '': 'change',
                    'input[name=q]': 'input'
                }
            });
            // Refresh the sources multiselect widget to reflect
            // changes when sources are added/removed or
            // modified (e.g., become available/unavailable)
            this.sources.bind('add change remove', function () {
                $('.select-sources').selectpicker('refresh');
            });
            this.types.bind('add change remove', function () {
                $('.select-types').selectpicker('refresh');
            });
            this.initDateTimePicker('#absoluteStartTime');
            this.initDateTimePicker('#absoluteEndTime');
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
            var multiselectOptions = {
                minWidth: 300,
                height: 185,
                classes: 'multiselect',
                checkAllText: 'Select all',
                uncheckAllText: 'Deselect all',
                selectedText: function (numChecked, numTotal) {
                    return numChecked + ' of ' + numTotal + ' selected';
                }
            };
            var typeSelectOptions = _.clone(multiselectOptions);
            typeSelectOptions.minWidth = 300;
            typeSelectOptions.noneSelectedText = 'Select a Type';
            var federationSourcesSelectOptions = _.clone(multiselectOptions);
            federationSourcesSelectOptions.noneSelectedText = 'Select Sources';
            this.$('.select-types').selectpicker();
            this.$('.select-sources').selectpicker();
            this.$('#radiusUnits').multiselect(singleselectOptions);
            this.$('#offsetTimeUnits').multiselect(singleselectOptions);
            this.$('#scheduleUnits').multiselect(singleselectOptions);
            this.$('#sortOrderSelected').selectpicker({ width: '200px' });
            this.setupPopOver('[data-toggle="keyword-popover"]', 'Search by free text using the grammar of the underlying source. For wildcard searches, use % or * after partial keywords (e.g. earth%).');
            this.setupPopOver('[data-toggle="time-popover"]', 'Search based on relative or absolute time of the created, modified, or effective date.');
            this.setupPopOver('[data-toggle="location-popover"]', 'Search by latitude/longitude or the USNG using a polygon, point-radius, or bounding box.');
            this.setupPopOver('[data-toggle="type-popover"]', 'Search for specific content types.');
            this.setupPopOver('[data-toggle="sorting-popover"]', 'Sort results by relevance, distance, created time, modified time or effective time.');
            this.setupPopOver('[data-toggle="additional-sources-popover"]', 'Perform an enterprise search (all federations are queried) or search specific sources.');
            this.updateZoomOnResults();
            this.updateLocationFields();
            if (this.model._cloneOf === undefined){
                this.$el.addClass('is-editing');
            } else {
                this.readOnly();
            }
        },
        setupPopOver: function (selector, content) {
            var options = {
                trigger: 'hover',
                content: content
            };
            this.$el.find(selector).popover(options);
        },
        beforeShowDatePicker: function (picker) {
            picker.style.zIndex = 200;
        },
        notDrawing: function () {
            this.clearLocation();
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
        onDestroy: function () {
            this.modelBinder.unbind();
            this.sourcesCollectionBinder.unbind();
        },
        filterOnEnter: function (e) {
            var view = this;
            if (e.keyCode === 13) {
                // defer it to make sure the event to set the query parameter is run
                if (this.isWorkspace) {
                    _.defer(function () {
                        view.workspaceSearch();
                    });
                } else {
                    _.defer(function () {
                        view.search();
                    });
                }
                e.preventDefault();
            }
        },
        updateZoomOnResults: function () {
            this.zoomOnResults = this.model.get('bbox') || this.model.get('radius') || this.model.get('polygon') ? true : false;
        },
        workspaceSearch: function () {
            var queryName = 'need a name';
            if (!queryName) {
                return;
            }
            wreqr.vent.trigger('search:clearfilters');
            if (_.isUndefined(this.model.get('src'))) {
                this.model.setSources(this.sources);
            }
            this.model.set({ name: queryName });
            this.model.startSearch();
            wreqr.vent.trigger('search:drawstop');
            wreqr.vent.trigger('workspace:save', this.model);
            wreqr.vent.trigger('workspace:show');
        },
        onSearchCleared: function () {
            // i don't like this being here.  I'd like to keep things centralized.
            wreqr.vent.trigger('filterFlagChanged', false);
        },
        onRadiusUnitsChanged: function () {
            this.$('#radiusValue').val(this.getDistanceFromMeters(this.model.get('radius'), this.$('#radiusUnits').val()));
        },
        onTimeUnitsChanged: function () {
            var timeInMillis = this.getTimeInMillis(this.model.get('dtoffset'), this.$('#offsetTimeUnits').val());
            // silently set it so as not to trigger a modelbinder update
            this.model.set('dtoffset', timeInMillis, { silent: true });
        },
        onSortOrderChanged: function () {
            var value = this.$('#sortOrderSelected').val();
            if (_.isString(value)) {
                var sort = value.split(':');
                if (sort.length === 2) {
                    this.model.set({
                        sortOrder: sort[0],
                        sortField: sort[1]
                    });
                }
            }
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
        getTimeInMillis: function (val, units) {
            switch (units) {
                case 'seconds':
                    return val * 1000;
                case 'minutes':
                    return this.getTimeInMillis(val, 'seconds') * 60;
                case 'hours':
                    return this.getTimeInMillis(val, 'minutes') * 60;
                case 'days':
                    return this.getTimeInMillis(val, 'hours') * 24;
                case 'weeks':
                    return this.getTimeInMillis(val, 'days') * 7;
                case 'months':
                    return this.getTimeInMillis(val, 'weeks') * 4;
                case 'years':
                    return this.getTimeInMillis(val, 'days') * 365;
                default:
                    return val;
            }
        },
        resetDateTimePicker: function (selector) {
            this.$(selector).datetimepicker('destroy');
            this.initDateTimePicker(selector);
        },
        initDateTimePicker: function (selector) {
            this.$(selector).datetimepicker({
                dateFormat: $.datepicker.ATOM,
                timeFormat: 'HH:mm:ss.lz',
                separator: 'T',
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
        },
        save: function(){
            store.saveQuery();
            this.workspaceSearch();
        },
        readOnly: function(){
            this.$el.find('label').attr('disabled', 'disabled');
            this.$el.find('input').attr('disabled', 'disabled');
            this.$el.find('select').attr('disabled', 'disabled');
            this.$el.find('form button').attr('disabled', 'disabled');
        },
        edit: function(){
            this.$el.addClass('is-editing');
            this.$el.find('label').removeAttr('disabled');
            this.$el.find('input').removeAttr('disabled');
            this.$el.find('select').removeAttr('disabled');
            this.$el.find('form button').removeAttr('disabled');
        },
        cancel: function(){
            store.resetQuery();
        }
    });
    return QueryOldBasicView;
});
