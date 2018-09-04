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
/*global require*/
var Marionette = require('marionette');
var template = require('./query-time.hbs');
var CustomElements = require('js/CustomElements');
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');
var properties = require('properties');
var CQLUtils = require('js/CQLUtils');
var Common = require('js/Common');
var metacardDefinitions = require('component/singletons/metacard-definitions');
const RelativeTimeView = require('component/relative-time/relative-time.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('query-time'),
    regions: {
        basicTime: '.basic-time',
        basicTimeField: '.basic-time-field',
        basicTemporalSelections: '.basic-temporal-selections',
        basicTimeBefore: '.basic-time-before',
        basicTimeAfter: '.basic-time-after',
        basicTimeBetweenBefore: '.between-before',
        basicTimeBetweenAfter: '.between-after',
        basicTimeRelative: '.basic-time-relative'
    },
    onBeforeShow: function () {
        this.turnOnEditing();
        this.setupTimeInput();
        this.setupTemporalSelections();
        this.setupTimeBefore();
        this.setupTimeAfter();
        this.setupTimeBetween();
        this.setupTimeRelative();
        this.listenTo(this.basicTime.currentView.model, 'change:value', this.handleTimeRangeValue);
        this.handleTimeRangeValue();
    },
    turnOnEditing: function () {
        this.$el.addClass('is-editing');
        this.regionManager.forEach(function (region) {
            if (region.currentView && region.currentView.turnOnEditing) {
                region.currentView.turnOnEditing();
            }
        });
    },
    constructFilter: function () {
        var filters = [];
        var timeRange = this.basicTime.currentView.model.getValue()[0];
        let timeSelection = this.basicTemporalSelections.currentView.model.getValue()[0];
        timeSelection = (!timeSelection.length) ? undefined : timeSelection;
        let timeBefore, timeAfter, relativeFunction;
        switch (timeRange) {
            case 'before':
                timeBefore = this.basicTimeBefore.currentView.model.getValue()[0];
                break;
            case 'after':
                timeAfter = this.basicTimeAfter.currentView.model.getValue()[0];
                break;
            case 'between':
                timeBefore = this.basicTimeBetweenBefore.currentView.model.getValue()[0];
                timeAfter = this.basicTimeBetweenAfter.currentView.model.getValue()[0];
                break;
            case 'relative':
                relativeFunction = this.basicTimeRelative.currentView.getViewValue();
                break;
        }
        if (timeBefore && timeSelection) {
            var timeFilter = {
                type: 'OR',
                filters: timeSelection.map((selection) => CQLUtils.generateFilter('BEFORE', selection, timeBefore))
            };
            filters.push(timeFilter);
        }
        if (timeAfter && timeSelection) {
            var timeFilter = {
                type: 'OR',
                filters: timeSelection.map((selection) => CQLUtils.generateFilter('AFTER', selection, timeAfter))
            };
            filters.push(timeFilter);
        }
        if (relativeFunction && timeSelection) {
            var timeDuration = {
                type: 'OR',
                filters: timeSelection.map((selection) => CQLUtils.generateFilter('=', selection, relativeFunction))
            };
            filters.push(timeDuration);
        }
        return filters;
    },
    handleTimeRangeValue: function () {
        var timeRange = this.basicTime.currentView.model.getValue()[0];
        this.$el.toggleClass('is-timeRange-any', timeRange === 'any');
        this.$el.toggleClass('is-timeRange-before', timeRange === 'before');
        this.$el.toggleClass('is-timeRange-after', timeRange === 'after');
        this.$el.toggleClass('is-timeRange-between', timeRange === 'between');
        this.$el.toggleClass('is-timeRange-relative', timeRange === 'relative');
    },
    setupTemporalSelections: function () {
        const definitions = metacardDefinitions.sortedMetacardTypes.filter((definition) => (!definition.hidden && definition.type === "DATE"))
        .map((definition) => ({
            label: definition.alias || definition.id,
            value: definition.id
        }));

        let value = properties.basicSearchTemporalSelectionDefault ? 
            [properties.basicSearchTemporalSelectionDefault] : [[]];

        if (this.options.filter.anyDate) {
            value = [this.options.filter.anyDate[0].property];
        }

        value = [value[0].filter((attribute) => (!metacardDefinitions.metacardTypes[attribute].hidden))];

        this.basicTemporalSelections.show(new PropertyView({
            model: new Property({
              enumFiltering: true,
              enumMulti: true,
              enum: definitions,
              isEditing: true,
              value: value,
              id: 'Apply Time Range To'
            })
          }));
    },
    setupTimeBefore: function () {
        var currentBefore = '';
        if (this.options.filter.anyDate) {
            this.options.filter.anyDate.forEach(function (subfilter) {
                if (subfilter.type === 'BEFORE') {
                    currentBefore = subfilter.value;
                }
            });
        }

        this.basicTimeBefore.show(new PropertyView({
            model: new Property({
                value: [currentBefore],
                id: 'Before',
                placeholder: 'Limit search to before this time.',
                type: 'DATE'
            })
        }));
    },
    setupTimeAfter: function () {
        var currentAfter = '';

        if (this.options.filter.anyDate) {
            this.options.filter.anyDate.forEach(function (subfilter) {
                if (subfilter.type === 'AFTER') {
                    currentAfter = subfilter.value;
                }
            });
        }

        this.basicTimeAfter.show(new PropertyView({
            model: new Property({
                value: [currentAfter],
                id: 'After',
                placeholder: 'Limit search to after this time.',
                type: 'DATE'
            })
        }));
    },
    setupTimeBetween: function () {
        var currentBefore = '';
        var currentAfter = '';

        // Pre-fill the last edited value or the load value from query
        if (this.options.filter.anyDate) {
            this.options.filter.anyDate.forEach(function (subfilter) {
                if (subfilter.type === 'BEFORE') {
                    currentBefore = subfilter.value;
                }
            });
        }

        if (this.options.filter.anyDate) {
            this.options.filter.anyDate.forEach(function (subfilter) {
                if (subfilter.type === 'AFTER') {
                    currentAfter = subfilter.value;
                }
            });
        }

        this.basicTimeBetweenBefore.show(new PropertyView({
            model: new Property({
                value: [currentBefore],
                id: 'To',
                placeholder: 'Limit search to before this time.',
                type: 'DATE'
            })
        }));
        this.basicTimeBetweenAfter.show(new PropertyView({
            model: new Property({
                value: [currentAfter],
                id: 'From',
                placeholder: 'Limit search to after this time.',
                type: 'DATE'
            })
        }));
    },
    setupTimeInput: function () {
        var currentValue = 'any';
        if (this.options.filter.anyDate) {
            if (this.options.filter.anyDate.length > 1) {
                currentValue = 'between';
            } else if (this.options.filter.anyDate[0].type === 'AFTER') {
                currentValue = 'after';
            } else if (this.options.filter.anyDate[0].type === 'BEFORE') {
                currentValue = 'before';
            } else {
                currentValue = 'relative';
            }
        }
        this.basicTime.show(new PropertyView({
            model: new Property({
                value: [currentValue],
                id: 'Time Range',
                enum: [{
                    label: 'Any',
                    value: 'any'
                }, {
                    label: 'After',
                    value: 'after'
                }, {
                    label: 'Before',
                    value: 'before'
                }, {
                    label: 'Between',
                    value: 'between'
                }, {
                    label: 'Relative',
                    value: 'relative'
                }]
            })
        }));
    },
    setupTimeRelative: function () {
        let currentValue;
        if (this.options.filter.anyDate) {
            this.options.filter.anyDate.forEach(function (subfilter) {
                if (subfilter.type === '=') {
                    currentValue = subfilter.value;
                }
            });
        }
        this.basicTimeRelative.show(new RelativeTimeView({
            value: currentValue
        }));
    }
});