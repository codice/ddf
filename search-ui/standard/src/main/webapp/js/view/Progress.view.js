/*global define*/

define(function (require) {
    "use strict";

    var $ = require('jquery'),
        Backbone = require('backbone'),
        Marionette = require('marionette'),
        ich = require('icanhaz'),
        Progress = {};

    ich.addTemplate('progressTemplate', require('text!templates/progress.handlebars'));
    require('modelbinder');

    Progress.ProgressModel = Backbone.Model.extend({
        defaults: {
            current: 0,
            total: 0
        },
        increment: function(val) {
            this.set({ 'current': this.get('current') + val });
        },
        isComplete: function() {
            return this.get('current') >= this.get('total');
        },
        setTotal: function(val) {
            this.set({ 'total': val });
        }
    });

    Progress.ProgressView = Marionette.ItemView.extend({
        template: 'progressTemplate',
        events: {
            'click #progress-text': 'merge'
        },
        initialize: function(options) {
            this.model = options.model;
            this.queryModel = options.queryModel;
            this.sources = options.sources;
            this.modelBinder = new Backbone.ModelBinder();
        },
        onRender: function() {
            this.configureProgress();

            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');

            this.modelBinder.bind(this.model, this.$el, bindings);

            this.listenTo(this.model, 'change', this.updateProgress);
        },
        onClose: function() {
            this.stopListening(this.model, 'change', this.updateProgress);
        },
        updateProgress: function() {
            var view = this;
            this.$el.find('#progressbar').show();
            $("#progressbar .ui-progressbar-value").animate({width: ((this.model.get('current') / this.model.get('total'))*100)+'%'}, 400, 'swing', function() {
                if(view.model.isComplete()) {
                    view.$el.find('.progress-panel').addClass('pulse');
                    view.$el.find('#progress-text').animate({'color': '#33CC33'}, 400);
                    view.$el.find('#progressbar').hide({
                        duration: 1500,
                        effect: 'blind',
                        complete: function () {
                            view.$el.find('#progressbar').unbind();
                        }
                    });
                }
            });
        },
        configureProgress: function() {
            //the progress bar is animated using jquery, if we want to swap this
            //out for a progress bar library at some point, it should be pretty simple
            this.$el.find('#progressbar').progressbar({value: 0.001});
            this.$el.find('#progressbar').hide();
            if (this.queryModel.get("src")) {
                this.model.setTotal(this.queryModel.get("src").split(",").length);
            } else {
                this.model.setTotal(this.sources);
            }
        },
        merge: function() {
            //merge the models somehow
            if(this.model.isComplete()) {
                this.close();
            }
        }
    });

    return Progress;

});