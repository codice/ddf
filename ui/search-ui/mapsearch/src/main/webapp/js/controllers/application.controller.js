/*global define */

define(function(require) {
    'use strict';

    // Load attached libs and application modules
    var aviture = require('aviture'),
        $ = require('jquery'),
        _ = require('underscore'),
        Marionette = require('marionette'),
        Application = require('app/application'),
        ApplicationController;

    ApplicationController = Marionette.Controller.extend({
        initialize : function() {
            _.bindAll(this);
        },

        renderApplicationViews : function() {
            var controller = this,
                mainView = new Application.Views.Main({
                    model : aviture.app.model
                }),
                navbarView = new Application.Views.NavBar({
                    model: aviture.app.model
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

            aviture.app.mainRegion.show(mainView);

            aviture.app.headerRegion.show(navbarLayout);
            navbarLayout.classification.show(new Application.Views.ClassificationBanner());
            navbarLayout.navbar.show(navbarView);

            aviture.app.footerRegion.show(footerLayout);
            footerLayout.classification.show(new Application.Views.ClassificationBanner());



        },

        // Render the Geospatial Views within the Main View.
        renderGeospatialViews : function(mainView) {
           // render cesium code here
            console.log('rendering cesium view now');


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
