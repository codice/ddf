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
var template = require('./time-settings.hbs');
var user = require('component/singletons/user-instance');
var CustomElements = require('js/CustomElements');
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');
var Common = require('js/Common');
var moment = require('moment');

var counter = 0;

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('time-settings'),
    regions: {
        propertyTimeFormat: '.property-time-format',
        propertyTimeCurrent: '.property-time-current'
    },
    animationFrameId: undefined,
    onBeforeShow: function () {
        this.setupResultCount();
        this.setupCurrentTime();
        this.repaintCurrentTime();
    },
    repaintCurrentTime: function(){
        this.animationFrameId = window.requestAnimationFrame(() => {
            if (counter % 5 === 0){
                this.setupCurrentTime();
            }
            counter++;
            this.repaintCurrentTime();
        });
    },
    setupCurrentTime: function(){
        this.propertyTimeCurrent.show(new PropertyView({
            model: new Property({
                label: 'Current Time (example)',
                value: [moment()],
                type: 'DATE'
            })
        }));
    },
    setupResultCount: function () {
        var timeFormat = user.get('user').get('preferences').get('timeFormat');

        this.propertyTimeFormat.show(new PropertyView({
            model: new Property({
                label: 'Time Format',
                value: [timeFormat],
                radio: [{
                    label: '24 Hour',
                    value: Common.getTimeFormats()['24']
                }, {
                    label: '12 Hour',
                    value: Common.getTimeFormats()['12']
                }]
            })
        }));

        this.propertyTimeFormat.currentView.turnOnEditing();
        this.listenTo(this.propertyTimeFormat.currentView.model, 'change:value', this.save);
    },
    save: function () {
        var preferences = user.get('user').get('preferences');
        preferences.set({
            timeFormat: this.propertyTimeFormat.currentView.model.getValue()[0]
        });
        preferences.savePreferences();
    },
    onDestroy: function(){
        window.cancelAnimationFrame(this.animationFrameId);
    }
});