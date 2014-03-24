/*global define*/

// #Main Application
define(function (require) {
    'use strict';

    // Load non attached libs and plugins


    // Load attached libs and application modules
    var _ = require('underscore'),
        Backbone = require('backbone'),
        Marionette = require('marionette'),
        Application = {};
    require('marionette');

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
