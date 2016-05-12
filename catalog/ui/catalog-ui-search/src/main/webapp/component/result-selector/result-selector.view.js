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
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    'text!./result-selector.hbs',
    'js/CustomElements',
    'properties',
    'js/store',
    'js/Common'
], function (wreqr, Marionette, _, $, resultSelectorTemplate, CustomElements, properties, store, Common) {

    var ResultSelector = Marionette.LayoutView.extend({
        template: resultSelectorTemplate,
        tagName: CustomElements.register('result-selector'),
        modelEvents: {
        },
        events: {
            'click .resultSelector-list > .resultSelector-result': 'handleClick',
            'mousedown .resultSelector-list > .resultSelector-result': 'stopTextSelection'
        },
        ui: {
        },
        regions: {
        },
        initialize: function(options){
            var self = this;
            this.listenTo(this.model, 'nested-change', _.debounce(this.render,200));
            this.listenTo(store.getSelectedResults(), 'update', this.handleSelectionChange);
            this.listenTo(store.getSelectedResults(), 'add', this.handleSelectionChange);
            this.listenTo(store.getSelectedResults(), 'remove', this.handleSelectionChange);
            this.listenTo(wreqr.vent, 'metacard:selected', function(direction, metacard){
                self.handleMapSelection(metacard);
            });
            wreqr.vent.trigger('map:clear');
            this.updateMap();
            store.addMetacardTypes(this.model.get('result').get('metacard-types'));
        },
        handleUpdate: function(){
            if (!this.isDestroyed) {
                wreqr.vent.trigger('map:clear');
                this.updateMap();
                store.addMetacardTypes(this.model.get('result').get('metacard-types'));
                this.render();
            }
        },
        serializeData: function(){
            var status = _.filter(this.model.get('result').get('status').toJSON(), function (status) {
                return status.id !== 'cache';
            });
            var results = _.map(this.model.get('result').get('results').slice(0, properties.resultCount), function (model) {
                return this.massageResult(model.toJSON());
            }, this);
            return {
                results: results,
                status: status,
                resultCount: this.resultsFound(status)
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
        resultsFound: function(data){
            var hits = _.reduce(data, function(hits, status) {
                return hits + status.hits;
            }, 0);
            var count = _.reduce(data, function(count, status) {
                return count + status.count;
            }, 0);
            if (hits === count) {
                return count + " results";
            } else {
                var displayed = count > properties.resultCount ? properties.resultCount : count;
                return "Top " + displayed + " of " + hits + " results displayed";
            }
        },
        updateMap: function(){
            var searchResult = this.model.get('result');
            if (searchResult){
                wreqr.vent.trigger('map:results', searchResult, false);
            }
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
                this.scrollIntoView(store.getSelectedResults().at(0).get('metacard'));
            }
        },
        handleMapSelection: function(metacard){
            this.handleNormalClick(metacard.id + metacard.get('properties>source-id'));
        },
        scrollIntoView: function(metacard){
            var result = this.$el.find('.resultSelector-list > .resultSelector-result[data-metacard-id="'+metacard.id + metacard.get('properties>source-id')+'"]');
            if (result && result.length > 0) {
                result[0].scrollIntoView();
            }
        },
        onRender: function(){
            this.handleSelectionChange();
        }
    });

    return ResultSelector;
});
