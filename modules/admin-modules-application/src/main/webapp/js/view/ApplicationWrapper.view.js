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
/** Main view page for add. */
define([
        'marionette',
        'icanhaz',
        'text!applicationModuleTemplate',
        '/applications/js/view/Application.view.js',
        'fileupload'
    ],
    function(Marionette, ich, applicationModuleTemplate, AppView) {
        "use strict";

        if(!ich.applicationModuleTemplate) {
            ich.addTemplate('applicationModuleTemplate', applicationModuleTemplate);
        }

        var ApplicationView = Marionette.Layout.extend({
            template: 'applicationModuleTemplate',
            tagName: 'div',
            className: 'full-height well',
            regions: {
                applications: '#application-view'
            },

            events: {
                'click .save': 'save',
                'click .cancel': 'cancel'
            },

            initialize: function (options) {
                this.modelClass = options.modelClass;
                this.enableApplicationRemoval = options.enableApplicationRemoval;
            },
            onRender: function () {
                this.applications.show(new AppView({modelClass: this.modelClass, enableApplicationRemoval: this.enableApplicationRemoval}));
            },

            save: function () {
                var that = this;
                if(this.applications) {
                    this.applications.currentView.installApp(function(message) {
                        that.$('.app-status-area').html(message);
                    });
                }
            },
            cancel: function () {
                this.applications.currentView.model.sync('read', this.applications.currentView.response);
                this.$('.app-status-area').html('');
            }
        });

        return ApplicationView;

    });
