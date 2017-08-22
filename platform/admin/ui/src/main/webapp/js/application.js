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
/*global define, encodeURI*/
/* jslint browser:true */
// #Main Application
define([
    'underscore',
    'backbone',
    'marionette',
    'icanhaz',
    'jquery',
    'poller',
    'js/wreqr',
    'js/models/Module',
    'text!templates/tabs.handlebars',
    'text!templates/appHeader.handlebars',
    'text!templates/header.handlebars',
    'text!templates/footer.handlebars',
    'js/controllers/Modal.controller',
    'js/controllers/SystemUsage.controller',
    'text!templates/moduleTab.handlebars',
    'properties'
], function (_, Backbone, Marionette, ich, $, poller, wreqr, Module, tabs, appHeader, header, footer, ModalController, SystemUsageController, moduleTab, Properties) {
    'use strict';

    var Application = {};

    // This was moved from the main.js file into here.
    // Since this modules has ui components, and it gets loaded before main.js, we need to init the renderer here for now until we sort this out.
    Marionette.Renderer.render = function (template, data) {
        if (!template) {
            return '';
        }
        return ich[template](data);
    };

    // Setup initial templates that we know we'll need
    ich.addTemplate('tabs', tabs);
    ich.addTemplate('appHeader', appHeader);
    ich.addTemplate('headerLayout', header);
    ich.addTemplate('footerLayout', footer);
    ich.addTemplate('moduleTab', moduleTab);

    Application.App = new Marionette.Application();

    Application.Controllers = {
        modalController: new ModalController({application: Application.App})
    };

    //add regions
    Application.App.addRegions({
        pageHeader: '#pageHeader',
        headerRegion: 'header',
        footerRegion: 'footer',
        mainRegion: 'main',
        appHeader: '#appHeader',
        modalRegion: '#modalRegion',
        sessionTimeoutModalRegion: '#sessionTmeoutModalRegion',
        alertsRegion: '.alerts'
    });

    //setup models
    var options = {
        delay: 30000
    };

    var addModuleRegions = function () {
        //add tab regions
        Application.ModuleModel.get('value').each(function (module) {
            var obj = {};
            obj[module.get('id')] = '#' + module.get('id');
            if (!Application.App.getRegion(module.get('id'))) {
                Application.App.addRegions(obj);
            }
        });
    };

    Application.ModuleModel = new Module.Model();
    Application.ModuleModel.fetch().done(addModuleRegions);
    Application.AppModel = new Backbone.Model(Properties);
    Application.App.appHeader.show(new (Backbone.Marionette.ItemView.extend({
        template: 'appHeader',
        className: 'app-header',
        tagName: 'div',
        model: Application.AppModel,
        events: {
            'click button': 'logout'
        },
        logout: function () {
            window.location = '/logout?prevurl=' + encodeURI(window.location.pathname);
        }
    }))());
    var modulePoller = poller.get(Application.ModuleModel, options);
    modulePoller.on('success', addModuleRegions);

    modulePoller.start();

    wreqr.vent.on('modulePoller:stop', function () {
        modulePoller.stop();
    });

    // show System Notification Banner
    Application.App.addInitializer(function () {
        new SystemUsageController();
    });

    //configure the router (we aren't using this yet)
    Application.Router = Backbone.Router.extend({
        routes: {
            '': 'index'
        },

        initialize: function () {
            _.bindAll.apply(_, [this].concat(_.functions(this)));
        },


        index: function () {

        }

    });

    return Application;
});
