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
    'icanhaz',
    'text!/installer/templates/main.handlebars',
    '/installer/lib/application-module/js/model/Applications.js'
    ], function (Marionette, WelcomeView, NavigationView, ConfigurationView, ApplicationView, FinishView, ich, mainTemplate, AppModel) {

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
            if(this.applications.currentView && this.model.get('stepNumber') !== 2) {
                this.hideApplications();
            }
            if(this.finish.currentView && this.model.get('stepNumber') !== 3) {
                this.hideFinish();
            }
            //show the next or previous view
            if(!this.welcome.currentView && this.model.get('stepNumber') <= 0) {
                this.showWelcome();
            } else if(!this.configuration.currentView && this.model.get('stepNumber') === 1) {
                this.showConfiguration();
            } else if(!this.applications.currentView && this.model.get('stepNumber') === 2) {
                this.showApplications();
            } else if(!this.finish.currentView && this.model.get('stepNumber') >= 3) {
                this.showFinish();
            }
        },
        showWelcome: function() {
            if(this.welcome.currentView) {
                this.welcome.show();
            } else {
                this.welcome.show(new WelcomeView({navigationModel: this.model}));
            }
            var elem = this.$(this.welcome.el);
            elem.show();
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
        }
    });

    return InstallerMainView;
});