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
    './Welcome.view',
    './Navigation.view.js',
    './Configuration.view.js',
    './GuestClaims.view.js',
    './Finish.view.js',
    './Profile.view.js',
    'icanhaz',
    'text!templates/installer/main.handlebars',
    'js/application'
    ], function (Marionette, WelcomeView, NavigationView, ConfigurationView, GuestClaimsView, FinishView,ProfileView, ich, mainTemplate, Application) {

    ich.addTemplate('mainTemplate', mainTemplate);

    var InstallerMainView = Marionette.Layout.extend({
        template: 'mainTemplate',
        tagName: 'div',
        className: 'container well well-main',
        regions: {
            welcome: '#welcome',
            configuration: '#configuration',
            guestClaims: '#guestClaims',
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
            var welcomeStep = 0, guestClaimsStep = 2, profileStep = 1, configStep = 3,  finishStep = 4;

            if(this.welcome.currentView && this.model.get('stepNumber') !== welcomeStep) {
                this.hideWelcome();
            }
            if(this.configuration.currentView && this.model.get('stepNumber') !== configStep) {
                this.hideConfiguration();
            }
            if(this.guestClaims.currentView && this.model.get('stepNumber') !== guestClaimsStep) {
                this.hideGuestClaims();
            }
            if(this.profiles.currentView && this.model.get('stepNumber') !== profileStep) {
                this.hideProfiles();
            }

            if(this.finish.currentView && this.model.get('stepNumber') !== finishStep) {
                this.hideFinish();
            }
            //show the next or previous view
            if(!this.welcome.currentView && this.model.get('stepNumber') <= welcomeStep) {
                this.showWelcome();
            } else if(!this.configuration.currentView && this.model.get('stepNumber') === configStep) {
                this.showConfiguration();
            } else if(!this.guestClaims.currentView && this.model.get('stepNumber') === guestClaimsStep) {
                this.showGuestClaims();
            } else  if(!this.profiles.currentView && this.model.get('stepNumber') === profileStep) {
                this.showProfiles();
            } else if(!this.finish.currentView && this.model.get('stepNumber') >= finishStep) {
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
        showGuestClaims: function() {
            if(this.guestClaims.currentView) {
                this.guestClaims.show();
            } else {
                this.guestClaims.show(new GuestClaimsView({navigationModel: this.model}));
            }
            this.$(this.guestClaims.el).show();
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
                    if(!profileKey && !profiles.isEmpty()){
                        profileKey = profiles.first().get('name');
                        self.model.set('selectedProfile',profileKey);
                    }

                    self.profiles.show(new ProfileView({
                        navigationModel: self.model,
                        collection: profiles
                    }));
                }).fail(function(error){
                    if(console){
                        console.log(error);
                    }
                }).done();
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
        hideGuestClaims: function() {
            this.guestClaims.close();
            this.$(this.guestClaims.el).hide();
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
