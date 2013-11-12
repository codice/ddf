/*global define*/

define(function (require) {
    "use strict";
    var Marionette = require('marionette'),
        Backbone = require('backbone'),
        ddf = require('ddf'),

        Draw = {};

    Draw.Views = {};

    Draw.Model = Backbone.Model.extend({

    });

    Draw.Controller = Marionette.Controller.extend({
        initialize : function(options){
            this.viewer = options.viewer;

        },

        method1 : function(){

        }


    });

    Draw.Views.ButtonView = Backbone.View.extend({



    });

    Draw.Views.CesiumView = Backbone.View.extend({

    });



    return Draw;
});