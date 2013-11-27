/*global define */
var window = this;
define(function (require) {
    'use strict';
    var document = window.document;
    // Load attached libs and application modules
    var ddf = require('ddf'),
        $ = require('jquery'),
        _ = require('underscore'),
        Marionette = require('marionette'),
        Application = require('js/application'),
        SearchControl = require('js/view/SearchControl.view'),
        GeoController = require('js/controllers/geospatial.controller'),
        DrawExtent = require('js/widgets/draw.extent'),
        DrawCircle = require('js/widgets/draw.circle'),
        Source = require('js/model/source'),
        Properties = require('properties'),
//        SlidingRegion = require('js/view/sliding.region'),
        ApplicationController;

    ApplicationController = Marionette.Controller.extend({
        initialize: function () {
            _.bindAll(this);
        },

        renderApplicationViews: function () {
            var controller = this,
                mainView = new Application.Views.Main({
                    model: ddf.app.model
                }),
                navbarLayout = new Application.Views.HeaderLayout(),
                footerLayout = new Application.Views.FooterLayout();

            // Once the main application view has been attached to the DOM, set up the dependent views.
            mainView.on('show', function () {
                controller.sources = new Source.Collection();
                controller.sources.fetch({
                    success : function(){
                        controller.renderGeospatialViews(mainView);
                    }
                });

            });

//            // Set up the menus on the navigation bar once the frame is loaded.
//            navbarView.on('show', function() {
//                controller.renderNavBarSubviews(navbarView);
//            });

            ddf.app.mainRegion.show(mainView);

            ddf.app.headerRegion.show(navbarLayout);
            navbarLayout.classification.show(new Application.Views.HeaderBanner());

            ddf.app.footerRegion.show(footerLayout);
            footerLayout.classification.show(new Application.Views.FooterBanner());

            $(document).ready(function () {
                document.title = Properties.branding;
            });

            //TODO: this hack here is to fix the issue of the main div not resizing correctly
            //when the header and footer are in place
            //remove this code when the correct way to get the div to resize is discovered
            $(window).resize(function() {
                var height = $("body").height();
                if(Properties.header && Properties.header !== "") {
                    height = height - 20;
                }
                if(Properties.footer && Properties.footer !== "") {
                    height = height - 20;
                }
                $("#main").height(height);
            });

            $(window).trigger('resize');
        },

        // Render the Geospatial Views within the Main View.
        renderGeospatialViews: function () {
            // render cesium code here

//            var slidingRegion = new SlidingRegion({el : "#searchControls"});
//            ddf.app.addRegions({
//                leftRegion: {
//                    selector: "#searchControls",
//                    regionType:  SlidingRegion
//                }
//            });sium view now');

            var geoController = ddf.app.controllers.geoController = new GeoController();
            var searchControlView = new SearchControl.SearchControlLayout({
                sources : this.sources,
                el: $('#searchControls'),
                model: new SearchControl.SearchControlModel(Properties)
            });
            //            ddf.app.leftRegion.show(searchControlView);
            searchControlView.render();


            ddf.app.controllers.drawExentController = new DrawExtent.Controller({
                scene: geoController.scene,
                notificationEl: $("#notificationBar")
            });
            ddf.app.controllers.drawCircleController = new DrawCircle.Controller({
                scene: geoController.scene,
                notificationEl: $("#notificationBar")
            });

        },

        // Render the various menus/sub-views within the nav bar.
        renderNavBarSubviews: function (/*navBarView*/) {
            // don't really have any at the moment
//            navBarView.updateSubMenuClasses();
//            navBarView.renderMapFrameworkNavigation();
        }
    });

    return ApplicationController;
});
