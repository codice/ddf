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
    'js/Common',
    'component/result-item/result-item.collection.view',
    'component/paging/paging.view',
    'component/dropdown/result-display/dropdown.result-display.view',
    'component/dropdown/result-filter/dropdown.result-filter.view',
    'component/dropdown/dropdown',
    'js/cql',
    'component/dropdown/result-sort/dropdown.result-sort.view',
    'component/singletons/user-instance',
    'component/result-status/result-status.view'
], function (Marionette, _, $, resultSelectorTemplate, CustomElements, properties, store, Common,
             ResultItemCollectionView, PagingView, DropdownView, ResultFilterDropdownView,
             DropdownModel, cql, ResultSortDropdownView, user, ResultStatusView) {

    function mixinBlackListCQL(originalCQL){
        var blackListCQL = {
            filters: [
                {
                    filters: user.get('user').get('preferences').get('resultBlacklist').map(function(id){
                        return {
                            property: '"id"',
                            type: '!=',
                            value: id
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
            'mousedown .resultSelector-list': 'stopTextSelection'
    };
    eventsHash['click .resultSelector-list '+resultItemSelector] = 'handleClick';

    var ResultSelector = Marionette.LayoutView.extend({
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

            this.listenTo(this.selectionInterface.getSelectedResults(), 'update', this.handleSelectionChange);
            this.listenTo(this.selectionInterface.getSelectedResults(), 'add', this.handleSelectionChange);
            this.listenTo(this.selectionInterface.getSelectedResults(), 'remove', this.handleSelectionChange);
            this.startListeningToFilter();
            this.startListeningToSort();
            this.startListeningToResult();

            //metacardDefinitions.addMetacardTypes(this.model.get('result').get('metacard-types'));
        },
        startListeningToBlacklist: function(){
            this.listenTo(user.get('user').get('preferences'), 'change:resultBlacklist', this.onBeforeShow);
        },
        startListeningToResult: function(){
            this.listenTo(this.model.get('result'), 'sync', this.onBeforeShow);
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
        handleClick: function(event){
            var resultItems = this.$el.find('.resultSelector-list '+resultItemSelector);
            var indexClicked = resultItems.index(event.currentTarget);
            var resultid = event.currentTarget.getAttribute('data-resultid');
            var alreadySelected = $(event.currentTarget).hasClass('is-selected');
            //shift key wins over all else
            if (event.shiftKey){
                this.handleShiftClick(resultid, indexClicked);
            } else if (event.ctrlKey || event.metaKey){
                this.handleControlClick(resultid, alreadySelected);
            } else {
                this.handleNormalClick(resultid);
            }
        },
        handleShiftClick: function(resultid, indexClicked){
            var resultItems = this.$el.find('.resultSelector-list '+resultItemSelector);
            var firstIndex = resultItems.index(this.$el.find('.resultSelector-list '+resultItemSelector+'.is-selected').first());
            var lastIndex = resultItems.index(this.$el.find('.resultSelector-list '+resultItemSelector+'.is-selected').last());
            if (firstIndex === -1 && lastIndex === -1){
                this.handleNormalClick(resultid);
            } else if (indexClicked <= firstIndex) {
                this.selectBetween(indexClicked, firstIndex);
            } else if (indexClicked >= lastIndex) {
                this.selectBetween(lastIndex, indexClicked + 1);
            } else {
                this.selectBetween(firstIndex, indexClicked + 1);
            }
        },
        selectBetween: function(startIndex, endIndex){
            this.selectionInterface.addSelectedResult(this.resultList.currentView.collection.selectBetween(startIndex, endIndex));
        },
        handleControlClick: function(resultid, alreadySelected){
            if (alreadySelected){
                this.selectionInterface.removeSelectedResult(this.model.get('result').get('results').fullCollection.get(resultid));
            } else {
                this.selectionInterface.addSelectedResult(this.model.get('result').get('results').fullCollection.get(resultid));
            }
        },
        handleNormalClick: function(resultid){
            this.selectionInterface.clearSelectedResults();
            this.selectionInterface.addSelectedResult(this.model.get('result').get('results').fullCollection.get(resultid));
        },
        handleSelectionChange: function(){
            var self = this;
            self.$el.find('.resultSelector-list '+resultItemSelector+'[data-resultid]').removeClass('is-selected');
            this.selectionInterface.getSelectedResults().forEach(function(metacard){
                self.$el.find('.resultSelector-list '+resultItemSelector+'[data-resultid="'+metacard.id+'"]').addClass('is-selected');
            });
            if (this.selectionInterface.getSelectedResults().length === 1) {
               // this.scrollIntoView(store.getSelectedResults().at(0).get('metacard'));
            }
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
            this.handleSelectionChange();
            this.handleFiltering(collapsedResults);
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
                model: resultCollection
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
                        label: 'Grid',
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
        }
    });

    return ResultSelector;
});
