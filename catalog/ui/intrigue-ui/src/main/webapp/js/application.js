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
/*global define, require, setTimeout */
// #Main Application
define([
    'jquery',
    'underscore',
    'marionette',
    'backbone',
    'properties',
    'maptype',
    // Templates
    'templates/header.layout.handlebars',
    'templates/footer.layout.handlebars',
    // Load non attached libs and plugins
    'bootstrap/dist/js/bootstrap.min',
    'backboneassociations',
    'modelbinder',
    'collectionbinder',
    'datepicker',
    'multiselect',
    'multiselectfilter'
], function ($, _, Marionette, Backbone, properties, maptype, header, footer) {
    var Application = {};
    Application.App = new Marionette.Application();
    Application.AppModel = new Backbone.Model(properties);
    Application.Controllers = { };
    // Set up the main regions that will be available at the Application level.
    Application.App.addRegions({
        loadingRegion: '#loading',
        headerRegion: 'header',
        footerRegion: 'footer',
        workspacesRegion: '#workspaces',
        workspaceRegion: '#workspace',
        metacardRegion: '#metacard',
        alertRegion: '#alert',
        ingestRegion: '#ingest',
        uploadRegion: '#upload',
        controlPanelRegion: '#controlPanel',
        modalRegion: '#modalRegion'
    });

    //setup the header
    Application.App.addInitializer(function () {
        Application.App.headerRegion.show(new Marionette.ItemView({
            tagName: 'header',
            template: header,
            model: Application.AppModel
        }), {
            replaceElement: true
        });
        if (Application.AppModel.get('ui').header && Application.AppModel.get('ui').header !== ""){
            $('body').addClass('has-header');
        }
    });
    //setup the footer
    Application.App.addInitializer(function () {
        Application.App.footerRegion.show(new Marionette.ItemView({
            tagName: 'footer',
            template: footer,
            model: Application.AppModel
        }), {
            replaceElement: true
        });
        if (Application.AppModel.get('ui').footer &&Application.AppModel.get('ui').footer !== ""){
            $('body').addClass('has-footer');
        }
    });

    Application.App.loadingRegion.$el.find('.welcome-branding').text(properties.branding);
    Application.App.loadingRegion.$el.addClass('show-welcome');

    //load all modules
    Application.App.addInitializer(function () {
        require([
            'js/router'
        ], function(){
            setTimeout(function(){
                Application.App.loadingRegion.$el.removeClass('is-open');
            }, 0);
        });
    });


    // Once the application has been initialized (i.e. all initializers have completed), start up
    // Backbone.history.
    Application.App.on('start', function () {
        Backbone.history.start();
    });


    return Application;
});
