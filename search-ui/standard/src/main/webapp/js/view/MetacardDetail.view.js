/*global define*/

define(function (require) {
    "use strict";

    var $ = require('jquery'),
        _ = require('underscore'),
        Backbone = require('backbone'),
        ich = require('icanhaz'),
        ddf = require('ddf'),
        dir = require('direction'),
        Metacard = {};

    ich.addTemplate('metacardTemplate', require('text!templates/metacard.handlebars'));

    Metacard.MetacardDetailView = Backbone.View.extend({
        className : 'slide-animate',
        events: {
            'click .location-link': 'viewLocation',
            'click .nav-tabs' : 'onTabClick',
            'click #prevRecord' : 'previousRecord',
            'click #nextRecord' : 'nextRecord'
        },
        initialize: function (options) {
            // options should be -> { metacard: metacard }
            this.model = options.metacard;

            if(this.model.hash) {
                this.hash = this.model.hash;
            }

            var metacardResult = this.model.get("metacardResult").at(0);
            var searchResult = metacardResult.get("searchResult");
            var collection = searchResult.get("results");
            var index = collection.indexOf(metacardResult);

            if (index !== 0) {
                this.prevModel = collection.at(index - 1);
            }
            if (index < collection.length - 1) {
                this.nextModel = collection.at(index + 1);
            }

            this.listenTo(this.model, 'change', this.render);
        },
        render: function () {
            this.$el.html(ich.metacardTemplate(this.model.toJSON()));

            if (_.isUndefined(this.prevModel)) {
                $('#prevRecord', this.$el).addClass('disabled');
            }
            if (_.isUndefined(this.nextModel)) {
                $('#nextRecord', this.$el).addClass('disabled');
            }

            return this;
        },
        onShow : function () {
            if (this.hash) {
                $.find('a[href$=' + this.hash + ']').pop().click();
            }
        },
        onTabClick : function(e){
            this.trigger('content-update');
            this.hash = e.target.hash;
        },
        viewLocation: function () {
            ddf.app.controllers.geoController.flyToLocation(this.model);
        },
        previousRecord: function () {
            if (this.prevModel) {
                this.prevModel.get("metacard").hash = this.hash;
                this.model.set('context', false);
                this.prevModel.get("metacard").set('direction', dir.downward);
                this.prevModel.get("metacard").set('context', true);
            }
        },
        nextRecord: function () {
            if (this.nextModel) {
                this.nextModel.get("metacard").hash = this.hash;
                this.model.set('context', false);
                this.nextModel.get("metacard").set('direction', dir.upward);
                this.nextModel.get("metacard").set('context', true);
            }
        },
        close: function () {
            this.remove();
            this.stopListening();
            this.unbind();
        }
    });

    return Metacard;

});