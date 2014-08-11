/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/* jshint unused: false */
/*global define, console */
define([
    'js/application',
    '/installer/js/views/InstallerMain.view.js',
    '/installer/js/models/Installer.js',
    '/installer/js/controllers/InstallerMain.controller.js',
    '/installer/templateConfig.js'
    ], function(Application, InstallerMainView, InstallerModel, InstallerMainController) {

    Application.App.module('Installation', function(AppModule, App, Backbone, Marionette, $, _) {

        var installerModel = new InstallerModel.Model();



        // Define a view to show
        // ---------------------

        var installerPage = new InstallerMainView({model: installerModel});

        // Define a controller to run this module
        // --------------------------------------

        var Controller = Marionette.Controller.extend({

            initialize: function(options){
                this.region = options.region;
            },

            show: function(){
                this.region.show(installerPage);
            }

        });

        //            contentRegion: '#content-region'
        // Initialize this module when the app starts
        // ------------------------------------------

        AppModule.addInitializer(function(){

            AppModule.installerMainController = new InstallerMainController();

            AppModule.contentController = new Controller({
                region: App.installation
            });

            // We determine how many steps we need based on if there are any profiles in the stystem.
            AppModule.installerMainController.fetchInstallProfiles().then(function(profiles){
                if(profiles.isEmpty()){
                    installerModel.set('showInstallProfileStep', false);
                    installerModel.setTotalSteps(3);
                } else {
                    installerModel.set('showInstallProfileStep', true);
                    installerModel.setTotalSteps(4);
                }
            }).fail(function(error){
                // fallback: just don't show the install profile steps.
                installerModel.set('showInstallProfileStep', false);
                installerModel.setTotalSteps(3);
                console.log(error);
            }).done(function(){
                // regardless of success, lets display the page.
                AppModule.contentController.show();
            });
        });
    });
});