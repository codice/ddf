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
var template = require('./relative-time.hbs');
var CustomElements = require('js/CustomElements');
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');
var CQLUtils = require('js/CQLUtils');
var Common = require('js/Common');


/*
    For specifying a relative time.  It shows a number field and a units field.
    Supports passing in a model that will have it's value field auto updated and synced.
    Supports not passing in a model and instead passing in a value option where currentLast and currentUnit are defined like so:
    {
        currentLast: 2,
        currentUnit: 'h'
    }
    Supports not passing in anything and simply letting the values default.  The values can be grabbed whenever needed by calling
    getViewValue.  This will return the values like so:
    {
        currentLast: 2,
        currentUnit: 'h'
    }
*/
module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('relative-time'),
    regions: {
        basicTimeRelativeValue: '.relative-value',
        basicTimeRelativeUnit: '.relative-unit'
    },
    onBeforeShow: function () {
        this.setupTimeRelative();
        this.turnOnEditing();
        this.listenTo(this.basicTimeRelativeUnit.currentView.model, 'change:value', this.updateModelValue);
        this.listenTo(this.basicTimeRelativeValue.currentView.model, 'change:value', this.updateModelValue);
        this.updateModelValue();
    },
    turnOnEditing: function () {
        this.$el.addClass('is-editing');
        this.regionManager.forEach(function (region) {
            if (region.currentView && region.currentView.turnOnEditing) {
                region.currentView.turnOnEditing();
            }
        });
    },
    getViewValue: function () {
        const timeLast = this.basicTimeRelativeValue.currentView.model.getValue()[0];
        const timeUnit = this.basicTimeRelativeUnit.currentView.model.getValue()[0];
        let duration;
        if (timeUnit === 'm' || timeUnit === 'h') {
            duration = "PT" + timeLast + timeUnit.toUpperCase();
        } else {
            duration = "P" + timeLast + timeUnit.toUpperCase();
        }
        return `RELATIVE(${duration})`;
    },
    getModelValue() {
        const currentValue = this.model.toJSON().value[0];
        if (currentValue === null || currentValue.indexOf('RELATIVE') !== 0) {
            return;
        }
        const duration = currentValue.substring(9, currentValue.length - 1).match(/(T?\d+)./)[0];
        let currentUnit = duration.substring(duration.length - 1, duration.length);
        let currentLast = duration.match(/\d+/);

        currentUnit = currentUnit.toLowerCase();
        if (duration.indexOf('T') === -1 && currentUnit === 'm') {
            //must capitalize months
            currentUnit = currentUnit.toUpperCase();
        }
        return {
            currentLast,
            currentUnit
        }
    },  
    updateModelValue() {
        if (this.model === undefined) {
            return;
        }
        this.model.setValue([this.getViewValue()]);
    },
    getOptionsValue() {
        return this.options.value;
    },
    getStartingValue() {
        if (this.model !== undefined) {
            return this.getModelValue();
        } else if (this.options !== undefined) {
            return this.getOptionsValue();
        } 
    },
    setupTimeRelative: function () {
        const {currentLast = 1 , currentUnit = 'h'} = this.getStartingValue() || {};
        this.basicTimeRelativeValue.show(new PropertyView({
            model: new Property({
                value: [currentLast],
                id: 'Last',
                placeholder: 'Limit searches to between the present and this time.',
                type: 'INTEGER'
            })
        }));
        this.basicTimeRelativeUnit.show(new PropertyView({
            model: new Property({
                value: [currentUnit],
                enum: [{
                    label: 'Minutes',
                    value: 'm'
                }, {
                    label: 'Hours',
                    value: 'h'
                }, {
                    label: 'Days',
                    value: 'd'
                }, {
                    label: 'Months',
                    value: 'M'
                }, {
                    label: 'Years',
                    value: 'y'
                }],
                id: 'Unit'
            })
        }))
    }
});