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
var template = require('./query-altitude.hbs');
var CustomElements = require('js/CustomElements');
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');
var CQLUtils = require('js/CQLUtils');
var DistanceUtils = require('js/DistanceUtils');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('query-altitude'),
    regions: {
        basicAltitude: '.basic-altitude',
        basicAltitudeUnits: '.basic-altitude-units',
        basicAltitudeAbove: '.basic-altitude-above',
        basicAltitudeBelow: '.basic-altitude-below',
        basicAltitudeBetweenAbove: '.basic-altitude-between .between-above',
        basicAltitudeBetweenBelow: '.basic-altitude-between .between-below'
    },
    previousAltitudeUnit: 'meters',
    onBeforeShow: function () {
        this.turnOnEditing();
        this.setupAltitudeInput();
        this.setupAltitudeAbove();
        this.setupAltitudeBelow();
        this.setupAltitudeBetween();
        this.setupAltitudeUnit();
        this.listenTo(this.basicAltitude.currentView.model, 'change:value', this.handleAltitudeRangeValue);
        this.listenTo(this.basicAltitudeUnits.currentView.model, 'change:value', this.handleAltitudeUnitValue);
        this.handleAltitudeRangeValue();
    },
    turnOnEditing: function () {
        this.$el.addClass('is-editing');
        this.regionManager.forEach(function (region) {
            if (region.currentView && region.currentView.turnOnEditing) {
                region.currentView.turnOnEditing();
            }
        });
    },
    handleAltitudeRangeValue: function () {
        var altitudeRange = this.basicAltitude.currentView.model.getValue()[0];
        this.$el.toggleClass('is-altitudeRange-any', altitudeRange === 'any');
        this.$el.toggleClass('is-altitudeRange-above', altitudeRange === 'above');
        this.$el.toggleClass('is-altitudeRange-below', altitudeRange === 'below');
        this.$el.toggleClass('is-altitudeRange-between', altitudeRange === 'between');

        this.$el.toggleClass('is-altitudeUnit', altitudeRange === 'above' ||
            altitudeRange === 'below' ||
            altitudeRange === 'between');
    },
    setupAltitudeUnit: function () {
        this.basicAltitudeUnits.show(new PropertyView({
            model: new Property({
                value: ['meters'],
                id: 'Altitude Unit',
                radio: [{
                    label: 'meters',
                    value: 'meters'
                }, {
                    label: 'kilometers',
                    value: 'kilometers'
                }, {
                    label: 'feet',
                    value: 'feet'
                }, {
                    label: 'yards',
                    value: 'yards'
                }, {
                    label: 'miles',
                    value: 'miles'
                }]
            })
        }));
    },
    handleAltitudeUnitValue: function () {
        var unit = this.basicAltitudeUnits
            .currentView.model.getValue()[0];

        var fields = [this.basicAltitudeAbove,
            this.basicAltitudeBelow,
            this.basicAltitudeBetweenAbove,
            this.basicAltitudeBetweenBelow
        ];

        for (var i = 0; i < fields.length; i++) {
            var field = fields[i].currentView.model;
            var value = parseFloat(field.getValue()[0]);

            // convert to meters and convert to any units.
            value = DistanceUtils.getDistanceInMeters(value, this.previousAltitudeUnit);
            value = DistanceUtils.getDistanceFromMeters(value, unit);
                
            value = DistanceUtils.altitudeRound(value);

            field.setValue([value.toString()]);
            fields[i].$el.find('input').val(value);
        }

        this.previousAltitudeUnit = unit;
    },
    setupAltitudeAbove: function () {
        var currentAbove = 0;

        var altFilters = this.options.filter["location.altitude-meters"];

        if (altFilters !== undefined) {
            // Search for the Above value
            for (var i = 0; i < altFilters.length; i++) {
                var value = altFilters[i].value;
                if (altFilters[i].type === ">=") {
                    if (value > currentAbove || currentAbove === 0) {
                        currentAbove = value;
                    }
                }
            }
        }

        this.basicAltitudeAbove.show(new PropertyView({
            model: new Property({
                value: [currentAbove],
                id: 'Above',
                type: 'INTEGER'
            })
        }));
    },
    setupAltitudeBelow: function () {
        var currentBelow = 0;

        var altFilters = this.options.filter["location.altitude-meters"];

        if (altFilters !== undefined) {
            // Search for the Before value
            for (var i = 0; i < altFilters.length; i++) {
                var value = altFilters[i].value;
                if (altFilters[i].type === "<=") {
                    if (value < currentBelow || currentBelow === 0) {
                        currentBelow = value;
                    }
                }
            }
        }

        this.basicAltitudeBelow.show(new PropertyView({
            model: new Property({
                value: [currentBelow],
                id: 'Below',
                type: 'INTEGER'
            })
        }));
    },
    setupAltitudeBetween: function () {
        var currentBelow = 0;
        var currentAbove = 0;

        var altFilters = this.options.filter["location.altitude-meters"];

        if (altFilters !== undefined) {
            // Search for the Before/Above values
            for (var i = 0; i < altFilters.length; i++) {
                var type = altFilters[i].type;
                var value = altFilters[i].value;

                if (type === "<=") {
                    if (value < currentBelow || currentBelow === 0) {
                        currentBelow = value;
                    }
                } else if (type === ">=") {
                    if (value > currentAbove || currentAbove === 0) {
                        currentAbove = value;
                    }
                }
            }
        }

        this.basicAltitudeBetweenAbove.show(new PropertyView({
            model: new Property({
                value: [currentAbove],
                id: 'Above',
                type: 'INTEGER'
            })
        }));
        this.basicAltitudeBetweenBelow.show(new PropertyView({
            model: new Property({
                value: [currentBelow],
                id: 'Below',
                type: 'INTEGER'
            })
        }));
    },
    setupAltitudeInput: function () {
        var currentValue = 'any';

        var altFilters = this.options.filter["location.altitude-meters"];

        if (altFilters !== undefined) {
            /* If the only filter is a <=, then it is a Before altitude filter.
               If the only filter is a >=, then it is a Above altitude filter.
               If there is a <= and >= filter, then it is a between altitude filter.
               If anything else, no filters - Select 'any'
            */

            var hasAbove = false;
            var hasBefore = false;

            for (var i = 0; i < altFilters.length; i++) {
                var type = altFilters[i].type;

                if (type === ">=") {
                    hasAbove = true;
                } else if (type === "<=") {
                    hasBefore = true;
                }
            }

            if (hasBefore && !hasAbove) {
                currentValue = "below";
            } else if (!hasBefore && hasAbove) {
                currentValue = "above";
            } else if (hasBefore && hasAbove) {
                currentValue = "between";
            }
        }

        this.basicAltitude.show(new PropertyView({
            model: new Property({
                value: [currentValue],
                id: 'Altitude Range',
                radio: [{
                    label: 'Any',
                    value: 'any'
                }, {
                    label: 'Above',
                    value: 'above'
                }, {
                    label: 'Below',
                    value: 'below'
                }, {
                    label: 'Between',
                    value: 'between'
                }]
            })
        }));
    },
    constructFilter: function () {
        // Determine which option is selected for altitude range
        var filters = [];
        var altitudeSelect = this.basicAltitude
            .currentView.model.getValue()[0];
        var altitudeUnit = this.basicAltitudeUnits.currentView.model.getValue()[0];

        switch (altitudeSelect) {
            // Build filters for altitude
            case 'above':
                // Handle Above altitude selected
                var aboveAltitude = parseFloat(
                this.basicAltitudeAbove.currentView.model.getValue()[0]);

                aboveAltitude = DistanceUtils.getDistanceInMeters(aboveAltitude, altitudeUnit);
                aboveAltitude = DistanceUtils.altitudeRound(aboveAltitude);

                var aboveAltitudeFilter = CQLUtils.generateFilter('>=',
                    'location.altitude-meters', aboveAltitude);
                filters.push(aboveAltitudeFilter);

                break;
            case 'below':
                // Handle Below altitude selected
                var belowAltitude = parseFloat(
                this.basicAltitudeBelow.currentView.model.getValue()[0]);

                belowAltitude = DistanceUtils.getDistanceInMeters(belowAltitude, altitudeUnit);
                belowAltitude = DistanceUtils.altitudeRound(belowAltitude);

                var belowAltitudeFilter = CQLUtils.generateFilter('<=',
                    'location.altitude-meters', belowAltitude);
                filters.push(belowAltitudeFilter);
                break;
            case 'between':
                // Handle between altitude selected

                var aboveAltitude = parseFloat(
                this.basicAltitudeBetweenAbove
                    .currentView.model.getValue()[0]);

                var belowAltitude = parseFloat(
                    this.basicAltitudeBetweenBelow
                    .currentView.model.getValue()[0]);

                aboveAltitude = DistanceUtils.getDistanceInMeters(aboveAltitude, altitudeUnit);
                aboveAltitude = DistanceUtils.altitudeRound(aboveAltitude);
                    
                belowAltitude = DistanceUtils.getDistanceInMeters(belowAltitude, altitudeUnit);
                belowAltitude = DistanceUtils.altitudeRound(belowAltitude);

                var aboveAltitudeFilter = CQLUtils.generateFilter('>=',
                    'location.altitude-meters', aboveAltitude);

                var belowAltitudeFilter = CQLUtils.generateFilter('<=',
                    'location.altitude-meters', belowAltitude);

                var altitudeFilters = {
                    type: "AND",
                    filters: [aboveAltitudeFilter, belowAltitudeFilter]
                };

                filters.push(altitudeFilters);
                break;
            case 'any':
            default:
                break;
        }
        return filters;
    }
}); 

