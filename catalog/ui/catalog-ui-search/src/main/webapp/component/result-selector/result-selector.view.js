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
    'text!./result-selector.hbs',
    'js/CustomElements',
    'properties',
    'js/store',
    'js/Common'
], function (Marionette, _, $, resultSelectorTemplate, CustomElements, properties, store, Common) {

    var ResultSelector = Marionette.LayoutView.extend({
        template: resultSelectorTemplate,
        tagName: CustomElements.register('result-selector'),
        modelEvents: {
        },
        events: {
            'click .resultSelector-list > .resultSelector-result': 'handleClick',
            'mousedown .resultSelector-list > .resultSelector-result': 'stopTextSelection',
            'click .first': 'firstPage',
            'click .previous': 'previousPage',
            'click .next': 'nextPage',
            'click .last': 'lastPage'
        },
        ui: {
        },
        regions: {
        },
        initialize: function(options){
            if (!this.model.get('result')) {
                this.model.startSearch();
            }

            this.listenTo(this.model.get('result'), 'sync', this.handleUpdate);
            this.listenTo(store.getSelectedResults(), 'update', this.handleSelectionChange);
            this.listenTo(store.getSelectedResults(), 'add', this.handleSelectionChange);
            this.listenTo(store.getSelectedResults(), 'remove', this.handleSelectionChange);

            this.updateActiveRecords();
            store.addMetacardTypes(this.model.get('result').get('metacard-types'));
        },
        handleUpdate: function(){
            if (!this.isDestroyed) {
                this.render();
            }
        },
        updateActiveRecords: function(){
            var searchResult = this.model.get('result');
            if (searchResult){
                store.get('content').setActiveSearchResult(searchResult);
            }
        },
        firstPage: function() {
            this.model.get('result').get('results').getFirstPage();
            this.render();
        },
        previousPage: function() {
            this.model.get('result').get('results').getPreviousPage();
            this.render();
        },
        nextPage: function() {
            this.model.get('result').get('results').getNextPage();
            this.render();
        },
        lastPage: function() {
            this.model.get('result').get('results').getLastPage();
            this.render();
        },
        serializeData: function(){
            var status = _.filter(this.model.get('result').get('status').toJSON(), function (status) {
                return status.id !== 'cache';
            });
            var resultsCollection = this.model.get('result').get('results');
            var results = _.map(resultsCollection.models, function (model) {
                return this.massageResult(model.toJSON());
            }, this);
            return {
                results: results,
                status: status,
                resultCount: this.resultsFound(resultsCollection.state.totalRecords, status),
                pages: this.currentPages(resultsCollection.state.currentPage, resultsCollection.state.totalPages),
                pending: this.someStatusSuccess(status, undefined),
                failed: this.someStatusSuccess(status, false),
                hasPreviousPage: resultsCollection.hasPreviousPage(),
                hasNextPage: resultsCollection.hasNextPage()
            };
        },
        massageResult: function(result){
            //make a nice date
            result.local = Boolean(result.metacard.properties['source-id'] === 'ddf.distribution');
            var dateModified = new Date(result.metacard.properties.modified);
            result.niceDiff = Common.getMomentDate(dateModified);
            //check validation errors
            var validationErrors = result.metacard.properties['validation-errors'];
            var validationWarnings = result.metacard.properties['validation-warnings'];
            if (validationErrors){
                result.hasError = true;
                result.error = validationErrors;
            }
            if (validationWarnings){
                result.hasWarning = true;
                result.warning = validationWarnings;
            }
            return result;
        },
        currentPages: function(current, total){
            var pages = '';
            if (current && total && total > 1) {
                pages = current + ' of ' + total;
            }
            return pages;
        },
        resultsFound: function(total, data){
            var hits = _.reduce(data, function(hits, status) {
                return status.hits ? hits + status.hits : hits;
            }, 0);
            var count = total ? total : 0;
            var searching = _.every(data, function(status) {
                return _.isUndefined(status.count);
            });
            if (searching) {
                return 'Seraching...'
            }
            if (hits === count || count > hits) {
                return count + ' results';
            } else {
                return 'Top ' + count + ' of ' + hits + ' results';
            }
        },
        someStatusSuccess: function(status, value) {
            return _.some(status, function(s) {
                return s.successful === value;
            });
        },
        stopTextSelection: function(event){
            event.preventDefault();
        },
        handleClick: function(event){
            var indexClicked = parseInt(event.currentTarget.getAttribute('data-index'));
            var metacardId = event.currentTarget.getAttribute('data-metacard-id');
            var alreadySelected = $(event.currentTarget).hasClass('is-selected');
            //shift key wins over all else
            if (event.shiftKey){
                this.handleShiftClick(metacardId, indexClicked);
            } else if (event.ctrlKey || event.metaKey){
                this.handleControlClick(metacardId, alreadySelected);
            } else {
                this.handleNormalClick(metacardId);
            }
        },
        handleShiftClick: function(metacardId, indexClicked){
            var firstIndex = parseInt(this.$el.find('.resultSelector-list > .resultSelector-result.is-selected').first().attr('data-index'));
            var lastIndex = parseInt(this.$el.find('.resultSelector-list > .resultSelector-result.is-selected').last().attr('data-index'));
            if (_.isNaN(firstIndex) && _.isNaN(lastIndex)){
                this.handleNormalClick(metacardId);
            } else if (indexClicked <= firstIndex) {
                this.selectBetween(indexClicked, firstIndex);
            } else if (indexClicked >= lastIndex) {
                this.selectBetween(lastIndex, indexClicked + 1);
            } else {
                this.selectBetween(firstIndex, indexClicked + 1);
            }
        },
        selectBetween: function(startIndex, endIndex){
            store.addSelectedResult(this.model.get('result').get('results').slice(startIndex, endIndex));
        },
        handleControlClick: function(metacardId, alreadySelected){
            if (alreadySelected){
                store.removeSelectedResult(this.model.get('result').get('results').get(metacardId));
            } else {
                store.addSelectedResult(this.model.get('result').get('results').get(metacardId));
            }
        },
        handleNormalClick: function(metacardId){
            store.clearSelectedResults();
            console.log('clear on click');
            store.addSelectedResult(this.model.get('result').get('results').get(metacardId));
            console.log('add on click');
        },
        handleSelectionChange: function(){
            var self = this;
            self.$el.find('.resultSelector-list > .resultSelector-result[data-metacard-id]').removeClass('is-selected');
            store.getSelectedResults().forEach(function(metacard){
                self.$el.find('.resultSelector-list > .resultSelector-result[data-metacard-id="'+metacard.id+'"]').addClass('is-selected');
            });
            if (store.getSelectedResults().length === 1) {
               // this.scrollIntoView(store.getSelectedResults().at(0).get('metacard'));
            }
        },
        scrollIntoView: function(metacard){
            var result = this.$el.find('.resultSelector-list > .resultSelector-result[data-metacard-id="'+metacard.id + metacard.get('properties>source-id')+'"]');
            if (result && result.length > 0) {
                //result[0].scrollIntoView();
            }
        },
        onRender: function(){
            this.handleSelectionChange();
        }
    });

    return ResultSelector;
});
