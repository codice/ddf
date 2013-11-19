/*global define*/

define(function (require) {
    "use strict";

    var Backbone = require('backbone'),
        ich = require('icanhaz'),
        ddf = require('ddf'),
        Metacard = {};

    ich.addTemplate('metacardTemplate', require('text!templates/metacard.handlebars'));

    Metacard.MetacardDetailView = Backbone.View.extend({
        className : 'slide-animate',
        events: {
            'click .location-link': 'viewLocation',
            'click .nav-tabs' : 'onTabClick'
        },
        initialize: function (options) {
            // options should be -> { metacard: metacard }
            this.model = options.metacard;
            this.listenTo(this.model, 'change', this.render);
        },
        render: function () {
            this.$el.html(ich.metacardTemplate(this.model.toJSON()));
            return this;
        },

        onTabClick : function(){
            this.trigger('content-update');
        },
        viewLocation: function () {
            ddf.app.controllers.geoController.flyToLocation(this.model);
        },
        close: function () {
            this.remove();
            this.stopListening();
            this.unbind();
        }
    });

    return Metacard;

});