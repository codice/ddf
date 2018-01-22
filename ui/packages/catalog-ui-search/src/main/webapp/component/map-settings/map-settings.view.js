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
var template = require('./map-settings.hbs');
var CustomElements = require('js/CustomElements');
var Property = require('component/property/property');
var PropertyView = require('component/property/property.view');
var user = require('component/singletons/user-instance');
var mtgeo = require('mt-geo');
var Common = require('js/Common');

var exampleLat = '14.94';
var exampleLon = '-11.875';
var exampleDegrees = mtgeo.toLat(exampleLat) + ' ' + mtgeo.toLon(exampleLon);
var exampleDecimal = exampleLat + ' ' + exampleLon;

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('map-settings'),
    modelEvents: {},
    events: {},
    regions: {
        coordinateFormat: '> .property-coordinate-format',
        coordinateFormatExample: '> .property-coordinate-example'
    },
    ui: {},
    initialize: function(){
        this.listenTo(user.get('user').get('preferences'), 'change:coordinateFormat', this.onBeforeShow);
    },
    onBeforeShow: function () {
        this.setupResultCount();
        this.setupCoordinateExample();
    },
    setupCoordinateExample: function(){
        var coordinateFormat = user.get('user').get('preferences').get('coordinateFormat');

        this.coordinateFormatExample.show(new PropertyView({
            model: new Property({
                label: 'Example Coordinates',
                value: [coordinateFormat === 'degrees' ? exampleDegrees : exampleDecimal],
                type: 'STRING'
            })
        }));
        this.coordinateFormatExample.currentView.turnOnLimitedWidth();
    },
    setupResultCount: function () {
        var coordinateFormat = user.get('user').get('preferences').get('coordinateFormat');

        this.coordinateFormat.show(new PropertyView({
            model: new Property({
               label: 'Coordinate Format',
                value: [coordinateFormat],
                enum: [{
                    label: 'Degrees, Minutes, Seconds',
                    value: 'degrees'
                }, {
                    label: 'Decimal',
                    value: 'decimal'
                }]
            })
        }));

        this.coordinateFormat.currentView.turnOnLimitedWidth();
        this.coordinateFormat.currentView.turnOnEditing();
        this.listenTo(this.coordinateFormat.currentView.model, 'change:value', this.save);
    },
    save: function () {
        Common.queueExecution(() => {
            var preferences = user.get('user').get('preferences');
            preferences.set({
                coordinateFormat: this.coordinateFormat.currentView.model.getValue()[0]
            });
            preferences.savePreferences();
        });
    },
    repositionDropdown: function () {
        this.$el.trigger('repositionDropdown.' + CustomElements.getNamespace());
    }
});