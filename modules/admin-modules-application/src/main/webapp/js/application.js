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

    //add regions
    Application.App.addRegions({
        applications: '#applications'
    });

    //configure the router (we aren't using this yet)
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
