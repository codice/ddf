/*global define */

define(function (require) {
    'use strict';

    // Load attached libs and application modules
    var ddf = require('ddf'),
        $ = require('jquery'),
        _ = require('underscore'),
        Marionette = require('marionette'),
        Application = require('js/application'),
        SearchControlView = require('js/view/SearchControl.view'),
        GeoController = require('js/controllers/geospatial.controller'),
        DrawExtent = require('js/widgets/draw.extent'),
        DrawCircle = require('js/widgets/draw.circle'),
        Source = require('js/model/source'),
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
                });
//                navbarView = new Application.Views.NavBar({
//                    model: ddf.app.model
//                }),
//                navbarLayout = new Application.Views.NavBarLayout(),
//                footerLayout = new Application.Views.FooterLayout();

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

//            ddf.app.headerRegion.show(navbarLayout);
//            navbarLayout.classification.show(new Application.Views.ClassificationBanner());
//            navbarLayout.navbar.show(navbarView);

//            ddf.app.footerRegion.show(footerLayout);
//            footerLayout.classification.show(new Application.Views.ClassificationBanner());


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
            var searchControlView = new SearchControlView({
                sources : this.sources,
                el: $('#searchControls')
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
