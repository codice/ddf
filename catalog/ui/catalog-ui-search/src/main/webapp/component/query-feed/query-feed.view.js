/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'marionette',
    'underscore',
    'jquery',
    './query-feed.hbs',
    'js/CustomElements',
    'js/store',
    'component/dropdown/query-status/dropdown.query-status.view',
    'moment',
    'component/dropdown/dropdown'
], function (Marionette, _, $, template, CustomElements, store, 
        QueryStatusView, moment, DropdownModel) {

    function getResultsFound(total, data){
        var hits = data.reduce(function(hits, status){
            return status.hits ? hits + status.hits : hits;
        }, 0);
        var searching = _.every(data, function(status) {
            return _.isUndefined(status.successful);
        });
        if (searching && data.length > 0) {
            return 'Searching...';
        } else if (total >= hits) {
            return total + ' results';
        } else {
            return hits + ' results';
        }
    }

    function getSomeStatusSuccess(status, value){
        return _.some(status, function(s) {
            return s.successful === value;
        });
    }

    function getPending(status){
        return getSomeStatusSuccess(status, undefined);
    }

    function getFailed(status){
        return getSomeStatusSuccess(status, false);
    }

    function getLastRan(initiated) {
        if (!_.isUndefined(initiated)) {
            return moment(initiated).fromNow();
        } else {
            return '';
        }
    }

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-feed'),
        regions: {
            detailsView: '.details-view'
        },
        initialize: function(options){
            this.updateQuery = _.throttle(this.updateQuery, 200);
            this.listenTo(this.model, 'change', this.updateQuery);
            if (this.model.has('result')) {
                this.listenToStatus(this.model);
            } else {
                this.listenTo(this.model, 'change:result', this.resultAdded);
            }
            this.listenTo(store.get('content'), 'change:query', this.highlight);
        },
        onRender: function(){
            this.detailsView.show(new QueryStatusView({
                model: new DropdownModel(),
                modelForComponent: this.model
            }));
        },
        updateQuery: function() {
            if (!this.isDestroyed){
                this.render();
            }
        },
        resultAdded: function(model) {
            if (model.has('result') && _.isUndefined(model.previous('result'))) {
                this.listenToStatus(model);
            }
        },
        listenToStatus: function(model) {
            this.$el.toggleClass('has-been-run');
            this.listenTo(model.get('result>results'), 'reset', this.updateQuery);
            this.listenTo(model.get('result>results'), 'add', this.updateQuery);
            this.listenTo(model.get('result'), 'sync', this.updateQuery);
            this.listenTo(model.get('result>status'), 'change', this.updateQuery);
        },
        serializeData: function(){
            var query = this.model.toJSON({
                additionalProperties: ['cid', 'color']
            });
            if (this.model.get('result')){
                var status = _.filter(this.model.get('result').get('status').toJSON(), function (status) {
                    return status.id !== 'cache';
                });
                return {
                    resultCount: getResultsFound(this.model.get('result').get('results').fullCollection.length, status),
                    pending: getPending(status),
                    failed: getFailed(status),
                    queryStatus: getLastRan(this.model.get('result>initiated'))
                };
            } else {
                return {
                    resultCount: 'Has not been run',
                    queryStatus: ''
                };
            }
        }
    });
});
