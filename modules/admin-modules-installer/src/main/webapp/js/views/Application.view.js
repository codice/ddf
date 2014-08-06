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
        'js/application',
        'fileupload'
    ],
    function(Marionette, ich, applicationWrapperTemplate, AppView, AppCSS, $, Application) {
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
                var self = this;
                Application.App.submodules.Installation.installerMainController.fetchInstallProfiles().then(function(profiles){

                    var profileKey = self.navigationModel.get('selectedProfile');
                    var selectedProfile = profiles.findWhere({name: profileKey});
                    var appView  = new AppView({selectedProfile: selectedProfile, modelClass: self.modelClass, showAddUpgradeBtn: false});
                    self.listenToOnce(appView, 'collection:loaded', function(){
                        if(!self.navigationModel.get('isCustomProfile')){
                            self.navigationModel.trigger('next'); // force next step... which will trigger the apps to be installed.
                        }
                    });
                    self.applications.show(appView);
                });

            },
            onClose: function () {
                this.stopListening(this.navigationModel);
            },
            next: function () {
                var that = this;
                this.navigationModel.trigger('block');
                if(this.applications) {
                    this.applications.currentView.installApp(this.navigationModel.nextStep).done(function() {
                        that.navigationModel.trigger('unblock');
                    });
                }
            },
            previous: function () {
                //this is your hook to perform any teardown that must be done before going to the previous step
                this.navigationModel.previousStep();
            }
        });

        return ApplicationView;

});