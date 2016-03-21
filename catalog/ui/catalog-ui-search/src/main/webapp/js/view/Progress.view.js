/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'jquery',
    'backbone',
    'marionette',
    'wreqr',
    'q',
    'underscore',
    'spin',
    'spinnerConfig',
    'text!templates/search/progress.handlebars',
    'direction',
    'progressbar'
], function ($, Backbone, Marionette, wreqr, Q, _, Spinner, spinnerConfig, progressTemplate, dir) {
    'use strict';
    var Progress = {};
    Progress.ProgressModel = Backbone.Model.extend({
        defaults: {
            current: 0,
            total: 0,
            hits: 0,
            lastMergeHits: 0
        },
        update: function (obj) {
            var setObj = {};
            var hits = 0;
            var done = 0;
            _.each(obj.model.lastResponse.data.status, function (srcStatus) {
                if (!_.isUndefined(srcStatus.hits)) {
                    hits += srcStatus.hits;
                }
                if (!_.isUndefined(srcStatus.state) && srcStatus.state !== 'ACTIVE') {
                    done++;
                }
            });
            setObj.hits = hits;
            setObj.current = done;
            //we need to only call set once, because every call will fire the listeners
            //need to be very careful about calling set multiple times on a backbone model within a single method
            //it is much much safer to call set a single time at the end of your method
            this.set(setObj);
        },
        isComplete: function () {
            return this.get('current') >= this.get('total');
        },
        setTotal: function (val) {
            this.set({ 'total': val });
        }
    });
    Progress.ProgressView = Marionette.ItemView.extend({
        template: progressTemplate,
        className: 'progress-view',
        events: {
            'click #progress-btn': 'merge',
            'click #progress-cancel': 'cancel'
        },
        modelEvents: { 'change': 'updateProgress' },
        initialize: function (options) {
            this.model = options.model;
            this.queryModel = options.queryModel;
            this.resultList = options.resultList;
            this.listenTo(wreqr.vent, 'search:clear', this.destroy);
            this.listenTo(wreqr.vent, 'search:error', this.destroy);
        },
        onRender: function () {
            this.configureProgress();
        },
        updateProgress: function () {
            var view = this;
            if ((this.model.get('total') === 1 || this.model.get('hits') <= 0) && this.model.isComplete()) {
                this.merge();
                this.destroy();
            }
            if (this.model.get('current') > 0) {
                this.$el.find('#progressbar').show();
            }
            if (this.model.get('hits') > 0) {
                if (this.model.get('lastMergeHits') === 0) {
                    this.merge();
                } else if (this.model.get('hits') !== this.model.get('lastMergeHits')) {
                    this.$el.find('#searching-text').hide();
                    this.$el.find('#progress-text').show();
                }
            } else {
                this.$el.find('#progress-text').hide();
                this.$el.find('#searching-text').show();
            }
            $('#progressbar .ui-progressbar-value').animate({ width: this.model.get('current') / this.model.get('total') * 100 + '%' }, 400, 'swing', function () {
                if (view.model.isComplete()) {
                    view.$el.find('#searching-text').hide();
                    view.$el.find('#progress-text').show();
                    view.$el.find('.progress-btn').removeClass('btn-info');
                    view.$el.find('.progress-btn').addClass('btn-primary');
                    view.$el.find('#progress-text').addClass('pulse');
                }
            });
        },
        configureProgress: function () {
            //the progress bar is animated using jquery, if we want to swap this
            //out for a progress bar library at some point, it should be pretty simple
            this.$el.find('#progressbar').progressbar({ value: 0.001 });
            this.$el.find('#progressbar').hide();
            this.$el.find('#progress-text').hide();
            if (this.queryModel.get('src')) {
                this.model.setTotal(this.queryModel.get('src').split(',').length);
            }
        },
        merge: function () {
            wreqr.vent.trigger('search:beginMerge');
            var view = this;
            //merge the models somehow
            this.model.set('lastMergeHits', this.model.get('hits'));
            var page = $.find('#searchPages').pop();
            this.$el.find('#progress-text').hide();
            this.$el.find('#searching-text').show();
            var spinner = new Spinner(spinnerConfig).spin(page);
            if (this.model.isComplete()) {
                this.destroy();
            }
            var deferred = Q.defer();
            deferred.promise.done(function () {
                view.resultList.mergeLatest();
                spinner.stop();
            });
            return deferred.resolve();
        },
        cancel: function () {
            this.resultList.cancel();
            wreqr.vent.trigger('search:show', dir.backward);
            wreqr.vent.trigger('search:clear');
        }
    });
    return Progress;
});
