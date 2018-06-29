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
var CQLUtils = require('js/CQLUtils');
var Common = require('js/Common');
const RelativeTimeView = require('component/relative-time/relative-time.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('query-time'),
    regions: {
        basicTime: '.basic-time',
        basicTimeField: '.basic-time-field',
        basicTimeBefore: '.basic-time-before',
        basicTimeAfter: '.basic-time-after',
        basicTimeBetweenBefore: '.between-before',
        basicTimeBetweenAfter: '.between-after',
        basicTimeRelative: '.basic-time-relative'
    },
    onBeforeShow: function () {
        this.turnOnEditing();
        this.setupTimeInput();
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
        if (timeBefore) {
            var timeFilter = {
                type: 'OR',
                filters: [
                    CQLUtils.generateFilter('BEFORE', 'created', timeBefore),
                    CQLUtils.generateFilter('BEFORE', 'modified', timeBefore),
                    CQLUtils.generateFilter('BEFORE', 'effective', timeBefore),
                    CQLUtils.generateFilter('BEFORE', 'metacard.created', timeBefore),
                    CQLUtils.generateFilter('BEFORE', 'metacard.modified', timeBefore)
                ]
            };
            filters.push(timeFilter);
        }
        if (timeAfter) {
            var timeFilter = {
                type: 'OR',
                filters: [
                    CQLUtils.generateFilter('AFTER', 'created', timeAfter),
                    CQLUtils.generateFilter('AFTER', 'modified', timeAfter),
                    CQLUtils.generateFilter('AFTER', 'effective', timeAfter),
                    CQLUtils.generateFilter('AFTER', 'metacard.created', timeAfter),
                    CQLUtils.generateFilter('AFTER', 'metacard.modified', timeAfter)
                ]
            };
            filters.push(timeFilter);
        }
        if (relativeFunction) {
            var timeDuration = {
                type: 'OR',
                filters: [
                    CQLUtils.generateFilter('=', 'created', relativeFunction),
                    CQLUtils.generateFilter('=', 'modified', relativeFunction),
                    CQLUtils.generateFilter('=', 'effective', relativeFunction),
                    CQLUtils.generateFilter('=', 'metacard.created', relativeFunction),
                    CQLUtils.generateFilter('=', 'metacard.modified', relativeFunction)
                ]
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
        let currentUnit, currentLast;
        if (this.options.filter.anyDate) {
            this.options.filter.anyDate.forEach(function (subfilter) {
                if (subfilter.type === '=') {
                    var duration = subfilter.value.substring(9, subfilter.value.length - 1).match(/(T?\d+)./)[0];
                    currentUnit = duration.substring(duration.length - 1, duration.length);
                    currentLast = duration.match(/\d+/);

                    currentUnit = currentUnit.toLowerCase();
                    if (duration.indexOf('T') === -1 && currentUnit === 'm') {
                        //must capitalize months
                        currentUnit = currentUnit.toUpperCase();
                    }
                }
            });
        }
        this.basicTimeRelative.show(new RelativeTimeView({
            value: {
                currentLast,
                currentUnit
            }
        }));
    }
});