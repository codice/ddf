/*global define*/
/*jslint nomen: false*/

define (function (require) {
    'use strict';
    var Backbone = require('backbone'),
        _ = require('underscore'),
        Marionette = require('marionette'),
        app = new Marionette.Application(),
        ApplicationModel;

    ApplicationModel = Backbone.Model.extend({
        defaults : {

        },

        initialize: function() {
        }

    });
    // Set up the application level model for state persistence
    app.model = new ApplicationModel();

    return {
        // This is a factory method used to create the modules of our application.
        // Example:
        //    `var ModuleName = ddf.module();`
        module : function (props) {
            return _.extend({ Views: {} }, Backbone.Events, props);
        },

        app : app
    };
});
