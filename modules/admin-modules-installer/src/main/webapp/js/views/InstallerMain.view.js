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
    '/installer/js/views/Welcome.view.js',
    '/installer/js/views/Navigation.view.js',
    '/installer/js/views/Configuration.view.js',
    '/installer/js/views/Application.view.js',
    '/installer/js/views/Finish.view.js',
    '/installer/js/views/Profile.view.js',
    'icanhaz',
    'text!/installer/templates/main.handlebars',
    '/installer/lib/application-module/js/model/Applications.js',
    'js/application'
    ], function (Marionette, WelcomeView, NavigationView, ConfigurationView, ApplicationView, FinishView,ProfileView, ich, mainTemplate, AppModel, Application) {

    ich.addTemplate('mainTemplate', mainTemplate);

    var InstallerMainView = Marionette.Layout.extend({
        template: 'mainTemplate',
        tagName: 'div',
        className: 'container well well-main',
        regions: {
            welcome: '#welcome',
            configuration: '#configuration',
            applications: '#applications',
            finish: '#finish',
            profiles: '#profiles',
            navigation: '#navigation'
        },
        onRender: function() {
            this.changePage();
            this.navigation.show(new NavigationView({model: this.model}));
            this.listenTo(this.model, 'change', this.changePage);
        },
        changePage: function() {
            //close whatever view is open
            if(this.welcome.currentView && this.model.get('stepNumber') !== 0) {
                this.hideWelcome();
            }
            if(this.configuration.currentView && this.model.get('stepNumber') !== 1) {
                this.hideConfiguration();
            }
            if(this.profiles.currentView && this.model.get('stepNumber') !== 2) {
                this.hideProfiles();
            }
            if(this.applications.currentView && this.model.get('stepNumber') !== 3) {
                this.hideApplications();
            }
            if(this.finish.currentView && this.model.get('stepNumber') !== 4) {
                this.hideFinish();
            }
            //show the next or previous view
            if(!this.welcome.currentView && this.model.get('stepNumber') <= 0) {
                this.showWelcome();
            } else if(!this.configuration.currentView && this.model.get('stepNumber') === 1) {
                this.showConfiguration();
            } else if(!this.profiles.currentView && this.model.get('stepNumber') === 2) {
                this.showProfiles();
            } else if(!this.applications.currentView && this.model.get('stepNumber') === 3) {
                this.showApplications();
            } else if(!this.finish.currentView && this.model.get('stepNumber') >= 4) {
                this.showFinish();
            }
        },
        showWelcome: function() {
            if(this.welcome.currentView) {
                this.welcome.show();
            } else {
                this.welcome.show(new WelcomeView({navigationModel: this.model}));
            }
            this.$(this.welcome.el).show();
        },
        showConfiguration: function() {
            if(this.configuration.currentView) {
                this.configuration.show();
            } else {
                this.configuration.show(new ConfigurationView({navigationModel: this.model}));
            }
            this.$(this.configuration.el).show();
        },
        showApplications: function() {
            if(this.applications.currentView) {
                this.applications.show();
            } else {
                this.applications.show(new ApplicationView({navigationModel: this.model, modelClass: AppModel}));
            }
            this.$(this.applications.el).show();
        },
        showFinish: function() {
            if(this.finish.currentView) {
                this.finish.show();
            } else {
                this.finish.show(new FinishView({navigationModel: this.model}));
            }
            this.$(this.finish.el).show();
        },
        showProfiles: function(){
            var self = this;
            if(this.profiles.currentView) {
                this.profiles.show();
            } else {
                Application.App.submodules.Installation.installerMainController.fetchInstallProfiles().then(function(profiles){
                    // set initial selected profile if null.
                    var profileKey = self.model.get('selectedProfile');
                    if(!profileKey){
                        profileKey = profiles.first().get('name');
                        self.model.set('selectedProfile',profileKey);
                    }

                    self.profiles.show(new ProfileView({
                        navigationModel: self.model,
                        collection: profiles
                    }));
                });
            }
            this.$(this.profiles.el).show();
        },
        hideWelcome: function() {
            this.welcome.close();
            this.$(this.welcome.el).hide();
        },
        hideConfiguration: function() {
            this.configuration.close();
            this.$(this.configuration.el).hide();
        },
        hideApplications: function() {
            this.applications.close();
            this.$(this.applications.el).hide();
        },
        hideFinish: function() {
            this.finish.close();
            this.$(this.finish.el).hide();
        },
        hideProfiles: function() {
            this.profiles.close();
            this.$(this.profiles.el).hide();
        }
    });

    return InstallerMainView;
});