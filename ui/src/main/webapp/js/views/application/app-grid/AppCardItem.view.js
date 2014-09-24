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
/*global define*/
define([
    'marionette',
    'icanhaz',
    'underscore',
    'js/wreqr.js',
    'text!applicationInfo'
    ],function (Marionette, ich, _, wreqr, applicationInfo) {
    "use strict";

    if(!ich.applicationInfo) {
        ich.addTemplate('applicationInfo', applicationInfo);
    }

    // List of apps that cannot have any actions performed on them through
    // the applications module
    var disableList = [
        'platform-app',
        'admin-app'
    ];

    // Itemview for each individual application
    var AppInfoView = Marionette.ItemView.extend({
        template: 'applicationInfo',
        className: 'grid-cell',
        events: {
            'click .fa.fa-times.stopApp': 'stopMessage',
            'click .fa.fa-download.startApp': 'startMessage',
            'click .stopAppConfirm': 'stopPrompt',
            'click .startAppConfirm': 'startPrompt',
            'click .removeAppConfirm': 'removePrompt',
            'click .fa.fa-times.removeApp': 'removeMessage',
            'click .fa.fa-download.installApp': 'installMessage',
            'click .removeConfirm': 'removePrompt',
            'click .installConfirm': 'installPrompt',
            'click': 'selectApplication'
        },

        // Will disable functionality for certain applications
        serializeData: function () {
            var that = this;
            var disable = false;
            disableList.forEach(function(child) {
                if(that.model.get('appId') === child) {
                    disable = true;
                }
            });

            return _.extend(this.model.toJSON(), {isDisabled: disable});
        },
        // Creates a message that gets displayed on the stop prompt displaying
        // any dependent applications that will also be stopped in the process
        stopMessage: function() {
            var that = this;
            var children = this.model.get('dependencies');
            var stopMessage = [];

            if(children.length !== 0) {
                children.forEach(function(child) {
                    that.model.collection.each(function(modelChild) {
                        if((modelChild.get('appId') === child) &&
                            (modelChild.get('state') === 'ACTIVE')) {
                            stopMessage.push(child);
                        }
                    });
                });
                this.model.set({stopMessage: stopMessage});
            }
        },

        // Creates a message that gets displayed on the start prompt displaying
        // any parent applications that will also be started in the process
        startMessage: function() {
            var that = this;
            var parents = this.model.get('parents');
            var startMessage = [];

            if(parents.length !== 0) {
                parents.forEach(function(parent) {
                    that.model.collection.each(function(modelChild) {
                        if((modelChild.get('appId') === parent) &&
                            (modelChild.get('state') === 'INACTIVE')) {
                            startMessage.push(parent);
                        }
                    });
                });
                this.model.set({startMessage: startMessage});
            }
        },
        // Only toggle the flag if the stop action is confirmed
        stopPrompt: function() {
            this.stopMessage();
            this.model.toggleChosenApp();
        },
        // Only toggle the flag if the start action is confirmed
        startPrompt: function() {
            this.startMessage();
            this.model.toggleChosenApp();
        },
        removePrompt: function() {
            this.model.toggleChosenApp();
        },
        selectApplication: function(){
            wreqr.vent.trigger('application:reqestSelection',this.model);
        }
    });

    return AppInfoView;
});