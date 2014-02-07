/*global define*/

define(function (require) {
    "use strict";

    var $ = require('jquery'),
        Backbone = require('backbone'),
        Marionette = require('marionette'),
        ich = require('icanhaz'),
        Q = require('q'),
        Spinner = require('spin'),
        spinnerConfig = require('spinnerConfig'),
        Progress = {};

    ich.addTemplate('progressTemplate', require('text!templates/progress.handlebars'));

    Progress.ProgressModel = Backbone.Model.extend({
        defaults: {
            current: 0,
            total: 0,
            hits: 0
        },
        increment: function(obj) {
            this.set({ 'current': this.get('current') + obj.value });
            if(obj.response && obj.response.data && obj.response.data.hits) {
                if(!this.lastMergeHits) {
                    this.lastMergeHits = obj.response.data.hits;
                } else {
                    this.set({'hits': obj.response.data.hits - this.lastMergeHits});
                }
            }
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
        className: 'progress-view',
        events: {
            'click #progress-btn': 'merge'
        },
        initialize: function(options) {
            this.model = options.model;
            this.queryModel = options.queryModel;
            this.sources = options.sources;
            this.resultList = options.resultList;
        },
        onRender: function() {
            this.configureProgress();

            this.listenTo(this.model, 'change', this.updateProgress);
        },
        onClose: function() {
            this.stopListening(this.model, 'change', this.updateProgress);
        },
        updateProgress: function() {
            var view = this;
            if(this.model.get('current') > 0) {
                this.$el.find('#progressbar').show();
            }
            if(this.model.get('hits') > 0) {
                this.$el.find('#searching-text').hide();
                this.$el.find('#progress-text').show();
            } else {
                this.$el.find('#progress-text').hide();
                this.$el.find('#searching-text').show();
            }
            $("#progressbar .ui-progressbar-value").animate({width: ((this.model.get('current') / this.model.get('total'))*100)+'%'}, 400, 'swing', function() {
                if(view.model.isComplete()) {
                    view.$el.find('.progress-btn').removeClass('btn-info');
                    view.$el.find('.progress-btn').addClass('btn-primary');
                    view.$el.find('#progress-text').addClass('pulse');
                }
            });
        },
        configureProgress: function() {
            //the progress bar is animated using jquery, if we want to swap this
            //out for a progress bar library at some point, it should be pretty simple
            this.$el.find('#progressbar').progressbar({value: 0.001});
            this.$el.find('#progressbar').hide();
            this.$el.find('#progress-text').hide();
            if (this.queryModel.get("src")) {
                this.model.setTotal(this.queryModel.get("src").split(",").length);
            } else {
                this.model.setTotal(this.sources);
            }
        },
        merge: function() {
            var view = this;
            //merge the models somehow
            var page = $.find('#searchPages').pop();
            this.model.lastMergeHits = this.model.get('hits') + this.model.lastMergeHits;
            this.$el.find('#progress-text').hide();
            this.$el.find('#searching-text').show();
            var spinner = new Spinner(spinnerConfig).spin(page);
            if(this.model.isComplete()) {
                this.close();
            }

            var deferred = Q.defer();
            deferred.promise.done(function() {
                view.resultList.mergeLatest();
                spinner.stop();
            });

            return deferred.resolve();
        }
    });

    return Progress;

});