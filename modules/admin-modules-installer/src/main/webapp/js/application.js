/*global define*/

// #Main Application
define([
    'underscore',
    'backbone',
    'marionette'
    ], function (_, Backbone, Marionette) {
    'use strict';

    var Application = {};

    Application.App = new Marionette.Application();

    Application.App.addRegions({
        headerRegion: '#header-region',
        footerRegion: '#footer-region',
        installation: '#installation'
    });

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

    return Application;
});
