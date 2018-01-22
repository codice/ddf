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
    'templates/banner.layout.handlebars',
    // Load non attached libs and plugins
    'bootstrap/dist/js/bootstrap.min',
    'backboneassociations',
    'modelbinder',
    'collectionbinder',
    'multiselect',
], function ($, _, Marionette, Backbone, properties, maptype, banner) {
    var Application = {};
    Application.App = new Marionette.Application();
    Application.AppModel = new Backbone.Model(properties);
    // Set up the main regions that will be available at the Application level.
    Application.App.addRegions({
        favicon: '#favicon',
        loadingRegion: '#loading',
        headerRegion: 'header',
        footerRegion: 'footer',
        workspacesRegion: '#workspaces',
        workspaceRegion: '#workspace',
        metacardRegion: '#metacard',
        sourcesRegion: '#sources',
        alertRegion: '#alert',
        ingestRegion: '#ingest',
        uploadRegion: '#upload',
        controlPanelRegion: '#controlPanel',
        aboutRegion: '#about',
        notFoundRegion: '#notfound'
    });

    const BannerView = Marionette.ItemView.extend({
        tagName: 'header',
        template: banner,
        model: Application.AppModel,
        events: {
            'click .fa-times': 'triggerHide'
        },
        initialize() {
            var message = this.model.get('ui')[this.tagName];
            if (message && message !== '') {
                this.$el.addClass('is-not-blank');
            }
        },
        getMessage() {
            return this.model.get('ui')[this.tagName];
        },
        serializeData() {
            var modelJSON = this.model.toJSON();
            modelJSON.message = this.getMessage();
            return modelJSON;
        },
        triggerHide() {
            $('body').removeClass('has-' + this.tagName);
        }
    });

    //setup the header
    Application.App.addInitializer(function () {
        Application.App.headerRegion.show(new BannerView(), {
            replaceElement: true
        });
        if (process.env.NODE_ENV !== 'production' || (Application.AppModel.get('ui').header && Application.AppModel.get('ui').header !== "")) {
            $('body').addClass('has-header');
        }
    });
    //setup the footer
    Application.App.addInitializer(function () {
        Application.App.footerRegion.show(new (BannerView.extend({
            tagName: 'footer'
        }))(), {
            replaceElement: true
        });
        if (process.env.NODE_ENV !== 'production' || (Application.AppModel.get('ui').footer && Application.AppModel.get('ui').footer !== "")) {
            $('body').addClass('has-footer');
        }
    });

    Application.App.favicon.$el.attr('href', 'data:image/png;base64,' + properties.ui.favIcon);
    Application.App.loadingRegion.$el.find('.welcome-branding').text(properties.branding);
    Application.App.loadingRegion.$el.find('.welcome-branding-name').text(properties.product);
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
