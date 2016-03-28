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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'text!./tabs-results.hbs',
    '../tabs.view',
    './tabs-results',
    'js/store',
    'component/result-selector/result-selector.view',
    'js/Common'
], function (Marionette, _, $, ResultsTabsTemplate, TabsView, ResultsTabModel, store, ResultSelectorView, Common) {

    var ResultsTabsView = TabsView.extend({
        template: ResultsTabsTemplate,
        setDefaultModel: function(){
            this.model = new ResultsTabModel();
        },
        initialize: function(options){
            if (options.model === undefined){
                this.setDefaultModel();
            }
            TabsView.prototype.initialize.call(this);
            this.listenTo(store.getCurrentQueries(), 'add', this.addTab);
            this.listenTo(store.getCurrentQueries(), 'remove', this.removeTab);
            this.listenTo(store.getFilteredQueries(), 'add', this.filterTab);
            this.listenTo(store.getFilteredQueries(), 'remove', this.unfilterTab);
            this.determineInitialTabs();
        },
        determineContent: function(){
            var activeTab = this.model.getActiveView();
            var activeView = activeTab.getView();
            var activeModel = activeTab.getModel();
            var compiledView;
            if (activeModel){
                compiledView = new activeView({
                    model: activeModel
                });
            } else {
                compiledView = new activeView();
            }
            this.tabsContent.show(compiledView);
        },
        determineInitialTabs: function(){
            store.getCurrentQueries().forEach(this.addTab.bind(this));
        },
        addTab: function(query){
            this.model.addTab(query.getId(), {
                tooltip: Common.cqlToHumanReadable(query.get('cql')),
                color: query.getColor(),
                getModel: function(){
                    return query;
                },
                getView: function(){
                    return ResultSelectorView;
                }
            });
            this.render();
        },
        removeTab: function(query){
            console.log(query);
            this.render();
        },
        filterTab: function(query){
            this.model.filterTab(query.getId());
            this.render();
        },
        unfilterTab: function(query){
            this.model.unfilterTab(query.getId());
            this.render();
        },
        onRender: function(){
            Common.setupPopOver(this.$el.find('> .tabs-list'));
            TabsView.prototype.onRender.call(this);
        }
    });

    return ResultsTabsView;
});