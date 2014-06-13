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
/*global define, require */

// #Main Application
define(['jquery',
        'underscore',
        'marionette',
        'backbone',
        'icanhaz',
        'properties',
        'maptype',
        // Templates
        'text!templates/main.handlebars',
        'text!templates/map.handlebars',
        'text!templates/header.layout.handlebars',
        'text!templates/footer.layout.handlebars',
        'js/view/Menu.view',
        // Load non attached libs and plugins
        'bootstrap',
        'backbonerelational',
        'backbonecometd',
        'jquerycometd'
    ], function ($, _, Marionette, Backbone, ich, properties, maptype, main, map, header, footer, Menu) {
        'use strict';

        var Application = {};

        // Setup templates
        ich.addTemplate('main', main);
        ich.addTemplate('map', map);
        ich.addTemplate('headerLayout', header);
        ich.addTemplate('footerLayout', footer);

        Application.App = new Marionette.Application();

        Application.AppModel = new Backbone.Model(properties);

        Application.Controllers = {};

        Application.Router = new Marionette.AppRouter({
            routes: {
                '': 'index'
            }
        });

        Application.Views = {};

        // Main Application View
        Application.Views.Main = Marionette.Layout.extend({
            template: 'main',
            className: 'height-full-menu',
            regions: {
                menu: '#menu'
            },

            onRender: function () {
                if (maptype.isNone()) {
                    $('#searchControls', this.$el).addClass('full-screen-search');
                }
                this.menu.show(new Menu.Bar({model: this.model}));
            }
        });

        // Map View
        if (!maptype.isNone()) {
            Application.Views.Map = new Marionette.ItemView({
                template: 'map',
                className: 'height-full'
            });
        }

        Application.showMapView = function () {
            if (maptype.is3d()) {
                var mapView = Application.Views.Map;

                mapView.on('show', function () {
                    require(['js/controllers/geospatial.controller',
                        'js/widgets/draw.extent',
                        'js/widgets/draw.circle'
                    ], function (GeoController, DrawExtent, DrawCircle) {

                        var geoController = new GeoController();

                        new DrawExtent.Controller({
                            scene: geoController.scene,
                            notificationEl: $('#notificationBar')
                        });

                        new DrawCircle.Controller({
                            scene: geoController.scene,
                            notificationEl: $('#notificationBar')
                        });
                    });
                });

                Application.App.mapRegion.show(mapView);
            }
        };

        return Application;
    }
);
