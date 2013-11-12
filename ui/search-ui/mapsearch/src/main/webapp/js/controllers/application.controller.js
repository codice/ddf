/*global define, console */

define(function(require) {
    'use strict';

    // Load attached libs and application modules
    var ddf = require('ddf'),
        $ = require('jquery'),
        _ = require('underscore'),
        Marionette = require('marionette'),
        Application = require('js/application'),
        MapView = require('js/view/Map.view'),
        ApplicationController;

    ApplicationController = Marionette.Controller.extend({
        initialize : function() {
            _.bindAll(this);
        },

        renderApplicationViews : function() {
            var controller = this,
                mainView = new Application.Views.Main({
                    model : ddf.app.model
                }),
                navbarView = new Application.Views.NavBar({
                    model: ddf.app.model
                }),
                navbarLayout = new Application.Views.NavBarLayout(),
                footerLayout = new Application.Views.FooterLayout();

            // Once the main application view has been attached to the DOM, set up the dependent views.
            mainView.on('show', function() {
                controller.renderGeospatialViews(mainView);
            });

            // Set up the menus on the navigation bar once the frame is loaded.
            navbarView.on('show', function() {
                controller.renderNavBarSubviews(navbarView);
            });

            ddf.app.mainRegion.show(mainView);

            ddf.app.headerRegion.show(navbarLayout);
            navbarLayout.classification.show(new Application.Views.ClassificationBanner());
            navbarLayout.navbar.show(navbarView);

            ddf.app.footerRegion.show(footerLayout);
            footerLayout.classification.show(new Application.Views.ClassificationBanner());



        },

        // Render the Geospatial Views within the Main View.
        renderGeospatialViews : function(mainView) {
           // render cesium code here
            console.log('rendering cesium view now');
            ddf.app.mapView = new MapView().render();
            var DrawExtent = require('js/widgets/draw.extent');

            var drawExtent = new DrawExtent.Controller({viewer: ddf.app.mapView.mapViewer})();


        },

        // Render the various menus/sub-views within the nav bar.
        renderNavBarSubviews : function(navBarView) {
            // don't really have any at the moment
//            navBarView.updateSubMenuClasses();
//            navBarView.renderMapFrameworkNavigation();
        }
    });

    return ApplicationController;
});
