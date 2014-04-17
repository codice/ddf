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
        'text!/installer/templates/application.handlebars',
        '/installer/lib/application-module/js/view/Application.view.js',
        'text!/installer/lib/application-module/css/style.css',
        'jquery',
        'fileupload'
    ],
    function(Marionette, ich, applicationWrapperTemplate, AppView, AppCSS, $) {
    "use strict";

        $("<style type='text/css'> " + AppCSS + " </style>").appendTo("head");

        ich.addTemplate('applicationWrapperTemplate', applicationWrapperTemplate);

        var ApplicationView = Marionette.Layout.extend({
            template: 'applicationWrapperTemplate',
            tagName: 'div',
            className: 'full-height',
            regions: {
                applications: '#application-region'
            },

            initialize: function (options) {
                this.navigationModel = options.navigationModel;
                this.modelClass = options.modelClass;
                this.listenTo(this.navigationModel, 'next', this.next);
                this.listenTo(this.navigationModel, 'previous', this.previous);
            },
            onRender: function () {
                this.applications.show(new AppView({navigationModel: this.navigationModel, modelClass: this.modelClass, showAddUpgradeBtn: false}));
            },
            onClose: function () {
                this.stopListening(this.navigationModel);
            },

            next: function () {
                this.navigationModel.trigger('block');

                if(this.applications) {
                    this.applications.currentView.installApp(this.navigationModel.nextStep);
                }

                this.navigationModel.trigger('unblock');
            },
            previous: function () {
                //this is your hook to perform any teardown that must be done before going to the previous step
                this.navigationModel.previousStep();
            }
        });

        return ApplicationView;

});