/*global define, require */

// #Main Application
define(['jquery',
        'underscore',
        'marionette',
        'backbone',
        'icanhaz',
        'properties',
        'maptype',
        'wreqr',
        // Templates
        'text!templates/main.html',
        'text!templates/map.html',
        'text!templates/header.layout.html',
        'text!templates/footer.layout.html',
        'text!templates/classification/classification-banner.html',
        // Load non attached libs and plugins
        'bootstrap',
        'backbonerelational',
        'backbonecometd',
        'jquerycometd'
    ], function ($, _, Marionette, Backbone, ich, properties, maptype, wreqr, main, map, header, footer, banner) {
        'use strict';

        var Application = {},
            ApplicationModel;

        // Setup templates
        ich.addTemplate('main', main);
        ich.addTemplate('map', map);
        ich.addTemplate('headerLayout', header);
        ich.addTemplate('footerLayout', footer);
        ich.addTemplate('classificationBanner', banner);

        Application.App = new Marionette.Application();

        Application.Controllers = {};

        Application.Router = Backbone.Router.extend({
            routes: {
                '': 'index'
            },

            initialize: function () {
                _.bindAll(this);
            },


            index: function () {

            }

        });

        Application.Views = {};

        // Main Application View
        Application.Views.Main = Backbone.View.extend({
            tagName: 'div',
            className: 'height-full',

            initialize: function () {
                var view = this;

                _.bindAll(view);
            },

            render: function () {
                var view = this;

                view.$el.html(ich.main());

                if (maptype.isNone()) {
                    $('#searchControls', this.$el).width('100%');
                }

                return view;
            }

        });

        // Map View
        if (!maptype.isNone()) {
            Application.Views.Map = Backbone.View.extend({
                tagName: 'div',
                className: 'height-full',

                initialize: function () {
                    var view = this;

                    _.bindAll(view);
                },

                render: function () {
                    var view = this;

                    view.$el.html(ich.map());

                    return view;
                }

            });
        }

        Application.showMapView = function () {
            if (maptype.is3d()) {
                var mapView = new Application.Views.Map({
                    model: Application.model
                });

                mapView.on('show', function () {
                    require(['js/controllers/geospatial.controller',
                             'js/widgets/draw.extent',
                             'js/widgets/draw.circle'
                            ], function (GeoController, DrawExtent, DrawCircle) {

                        // Create geo controller
                        var geoController = new GeoController();
                        wreqr.vent.on('map:show', function (model) {
                            geoController.flyToLocation(model);
                        });
                        wreqr.vent.on('search:start', function () {
                            geoController.clearResults();
                        });
                        wreqr.vent.on('search:results', function (result, zoomOnResults) {
                            geoController.showResults(result.get('results'));
                            if (zoomOnResults) {
                                geoController.flyToCenterPoint(result.get('results'));
                            }
                        });
                        wreqr.vent.on('search:clear', function () {
                            geoController.clearResults();
                            geoController.billboardCollection.removeAll();
                        });

                        if (wreqr.reqres.hasHandler('search:results')) {
                            geoController.showResults(wreqr.reqres.request('search:results').get('results'));
                        }

                        // Create draw extent controller
                        var drawExentController = new DrawExtent.Controller({
                            scene: geoController.scene,
                            notificationEl: $('#notificationBar')
                        });
                        wreqr.vent.on('draw:extent', function (model) {
                            drawExentController.draw(model);
                        });
                        wreqr.vent.on('draw:stop', function () {
                            drawExentController.stop();
                        });
                        wreqr.vent.on('draw:end', function () {
                            drawExentController.destroy();
                        });

                        // Create draw circle controller
                        var drawCircleController = new DrawCircle.Controller({
                            scene: geoController.scene,
                            notificationEl: $('#notificationBar')
                        });
                        wreqr.vent.on('draw:circle', function (model) {
                            drawCircleController.draw(model);
                        });
                        wreqr.vent.on('draw:stop', function () {
                            drawCircleController.stop();
                        });
                        wreqr.vent.on('draw:end', function () {
                            drawCircleController.destroy();
                        });
                    });
                });

                Application.App.mapRegion.show(mapView);
            }
        };

        Application.Views.HeaderLayout = Marionette.Layout.extend({
            template: 'headerLayout',
            className: 'header-layout',

            regions: {
                classification: '.classification-container',
            }
        });

        Application.Views.FooterLayout = Marionette.Layout.extend({
            template: 'footerLayout',
            className: 'footer-layout',

            regions: {
                classification: '.classification-container'
            }
        });

        Application.Views.HeaderBanner = Backbone.View.extend({
            className: "classification-banner",

            initialize: function () {
                var view = this;
                _.bindAll(view);
            },

            render: function () {
                var view = this,
                    headerText = properties.header,
                    style = properties.style,
                    textColor = properties.textColor,
                    header = {};

                if (headerText && headerText !== "") {
                    //set up header
                    header.text = headerText;
                    header.style = style;
                    header.textColor = textColor;

                    view.$el.html(ich.classificationBanner(header));
                }
            }
        });

        Application.Views.FooterBanner = Backbone.View.extend({
            className: "classification-banner",

            initialize: function () {
                var view = this;
                _.bindAll(view);
            },

            render: function () {
                var view = this,
                    footerText = properties.footer,
                    style = properties.style,
                    textColor = properties.textColor,
                    footer = {};

                if (footerText && footerText !== "") {
                    //set up footer
                    footer.text = footerText;
                    footer.style = style;
                    footer.textColor = textColor;

                    view.$el.html(ich.classificationBanner(footer));
                }
            }
        });

        ApplicationModel = Backbone.Model.extend({
            defaults: {

            },

            initialize: function () {
            }

        });
        // Set up the application level model for state persistence
        Application.model = new ApplicationModel();

        Application.module = function (props) {
            return _.extend({ Views: {} }, Backbone.Events, props);
        };

        return Application;
    }
);
