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
    './result-selector.hbs',
    'js/CustomElements',
    'properties',
    'js/store',
    'js/Common',
    'component/result-item/result-item.collection.view',
    'component/paging/paging.view',
    'component/dropdown/result-display/dropdown.result-display.view',
    'component/dropdown/result-filter/dropdown.result-filter.view',
    'component/dropdown/dropdown',
    'js/cql',
    'component/dropdown/result-sort/dropdown.result-sort.view',
    'component/singletons/user-instance',
    'component/result-status/result-status.view',
    'decorator/result-selection.decorator',
    'decorator/Decorators'
], function (Marionette, _, $, resultSelectorTemplate, CustomElements, properties, store, Common,
             ResultItemCollectionView, PagingView, DropdownView, ResultFilterDropdownView,
             DropdownModel, cql, ResultSortDropdownView, user, ResultStatusView,
             ResultSelectionDecorator, Decorators) {

    function mixinBlackListCQL(originalCQL){
        var blackListCQL = {
            filters: [
                {
                    filters: user.get('user').get('preferences').get('resultBlacklist').map(function(blacklistItem){
                        return {
                            property: '"id"',
                            type: '!=',
                            value: blacklistItem.id
                        };
                    }),
                    type: 'AND'
                }
            ],
            type: 'AND'
        };
        if (originalCQL){
            blackListCQL.filters.push(originalCQL);
        }
        return blackListCQL;
    }

    var namespace = CustomElements.getNamespace();
    var resultItemSelector = namespace+'result-item';
    var eventsHash = {
            'mousedown .resultSelector-list': 'stopTextSelection',
            'click > .resultSelector-new .merge': 'mergeNewResults',
            'click > .resultSelector-new .ignore': 'ignoreNewResults'
    };
    eventsHash['click .resultSelector-list '+resultItemSelector] = 'handleClick';

    var ResultSelector = Marionette.LayoutView.extend(Decorators.decorate({
        template: resultSelectorTemplate,
        tagName: CustomElements.register('result-selector'),
        modelEvents: {
        },
        events: eventsHash,
        ui: {
        },
        regions: {
            resultStatus: '.resultSelector-status',
            resultList: '.resultSelector-list',
            resultPaging: '.resultSelector-paging',
            resultDisplay: '.menu-resultDisplay',
            resultFilter: '.menu-resultFilter',
            resultSort: '.menu-resultSort'
        },
        selectionInterface: store,
        initialize: function(options){
            this.selectionInterface = options.selectionInterface || store;
            if (!this.model.get('result')) {
                this.model.startSearch();
            }
            this.model.get('result').set('currentlyViewed', true);
            this.selectionInterface.setCurrentQuery(this.model);
            this.startListeningToFilter();
            this.startListeningToSort();
            this.startListeningToResult();
            this.startListeningToMerged();
            this.startListeningToStatus();
        },
        mergeNewResults: function(){
            this.model.get('result').mergeNewResults();
        },
        ignoreNewResults: function(){
            this.$el.toggleClass('ignore-new', true);
        },
        handleMerged: function(){
            this.$el.toggleClass('ignore-new', false);
            this.$el.toggleClass('has-unmerged', this.model.get('result').isUnmerged());
        },
        startListeningToMerged: function(){
            this.listenTo(this.model.get('result'), 'change:merged', this.handleMerged);
        },
        handleStatus: function(){
            this.$el.toggleClass('is-searching', this.model.get('result').isSearching());
        },
        startListeningToStatus: function(){
            this.listenTo(this.model.get('result'), 'sync request error', this.handleStatus);
        },
        startListeningToBlacklist: function(){
            this.listenTo(user.get('user').get('preferences').get('resultBlacklist'),
             'add remove update reset', this.onBeforeShow);
        },
        startListeningToResult: function(){
            this.listenTo(this.model.get('result'), 'reset:results', this.onBeforeShow);
        },
        startListeningToFilter: function(){
            this.listenTo(user.get('user').get('preferences'), 'change:resultFilter', this.onBeforeShow);
        },
        startListeningToSort: function(){
            this.listenTo(user.get('user').get('preferences'), 'change:resultSort', this.onBeforeShow);
        },
        stopTextSelection: function(event){
            event.preventDefault();
        },
        scrollIntoView: function(metacard){
            var result = this.$el.find('.resultSelector-list '+resultItemSelector+'[data-resultid="'+metacard.id + metacard.get('properties>source-id')+'"]');
            if (result && result.length > 0) {
                //result[0].scrollIntoView();
            }
        },
        onBeforeShow: function(){
            var resultFilter = user.get('user').get('preferences').get('resultFilter');
            if (resultFilter) {
                resultFilter = cql.simplify(cql.read(resultFilter));
            }
            resultFilter = mixinBlackListCQL(resultFilter);
            var filteredResults = this.model.get('result').get('results').generateFilteredVersion(resultFilter);
            var collapsedResults = filteredResults.collapseDuplicates();
            collapsedResults.updateSorting(user.get('user').get('preferences').get('resultSort'));
            this.showResultPaging(collapsedResults);
            this.showResultList(collapsedResults);
            this.showResultStatus(collapsedResults);
            this.showResultDisplayDropdown();
            this.showResultFilterDropdown();
            this.showResultSortDropdown();
            this.handleFiltering(collapsedResults);
            this.handleMerged();
            this.handleStatus();
            let resultCountOnly = this.model.get('result').get('resultCountOnly') === true;
            this.regionManager.forEach((region) => {
                region.currentView.$el.toggleClass("is-hidden", resultCountOnly);
            });
        },
        handleFiltering: function(resultCollection){
            this.$el.toggleClass('has-filter', resultCollection.amountFiltered !== 0);
        },
        showResultStatus: function(resultCollection){
            this.resultStatus.show(new ResultStatusView({
                model: resultCollection
            }));
        },
        showResultPaging: function(resultCollection){
            this.resultPaging.show(new PagingView({
                model: resultCollection,
                selectionInterface: this.selectionInterface
            }));
        },
        showResultList: function(resultCollection){
            this.resultList.show(new ResultItemCollectionView({
                collection: resultCollection,
                selectionInterface: this.selectionInterface
            }));
        },
        showResultFilterDropdown: function(){
            this.resultFilter.show(new ResultFilterDropdownView({
                model: new DropdownModel()
            }));
        },
        showResultSortDropdown: function(){
            this.resultSort.show(new ResultSortDropdownView({
                model: new DropdownModel()
            }));
        },
        showResultDisplayDropdown: function(){
            this.resultDisplay.show(DropdownView.createSimpleDropdown(
                {
                    list: [{
                        label: 'List',
                        value: 'List'
                    }, {
                        label: 'Gallery',
                        value: 'Grid'
                    }],
                    defaultSelection: [user.get('user').get('preferences').get('resultDisplay')]
                }
            ));
            this.stopListening(this.resultDisplay.currentView.model);
            this.listenTo(this.resultDisplay.currentView.model, 'change:value', function(){
                var prefs = user.get('user').get('preferences');
                var value = this.resultDisplay.currentView.model.get('value')[0];
                prefs.set('resultDisplay', value);
                prefs.savePreferences();
            });
        },
        onDestroy: function(){
            Common.queueExecution(function(){
                if (this.model.get('result')) {
                    this.model.get('result').set('currentlyViewed', false);
                }
            }.bind(this));
        }
    }, ResultSelectionDecorator));

    return ResultSelector;
});
