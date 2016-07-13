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
    './content.hbs',
    'js/CustomElements',
    'component/navigation/workspace/navigation.workspace.view',
    'properties',
    'component/tabs/workspace-content/tabs-workspace-content',
    'component/tabs/workspace-content/tabs-workspace-content.view',
    'component/tabs/query/tabs-query.view',
    'js/store',
    'component/tabs/metacard/tabs-metacard.view',
    'component/tabs/metacards/tabs-metacards.view',
    'js/Common',
    'component/metacard-title/metacard-title.view',
    'component/router/router',
    'component/visualization/visualization.view'
], function (wreqr, Marionette, _, $, contentTemplate, CustomElements, MenuView, properties,
             WorkspaceContentTabs, WorkspaceContentTabsView, QueryTabsView, store,
             MetacardTabsView, MetacardsTabsView, Common, MetacardTitleView, router, VisualizationView) {

    var debounceTime = 25;

    var ContentView = Marionette.LayoutView.extend({
        template: contentTemplate,
        tagName: CustomElements.register('content'),
        modelEvents: {
        },
        events: {
            'click .content-panelTwo-close': 'unselectQueriesAndResults'
        },
        ui: {
        },
        regions: {
            'menu': '.content-menu',
            'panelOne': '.content-panelOne',
            'panelTwo': '.content-panelTwo-content',
            'panelTwoTitle': '.content-panelTwo-title',
            'panelThree': '.content-panelThree'
        },
        initialize: function(){
            this._mapView = new VisualizationView({
                selectionInterface: store.get('content')
            });
            this.listenTo(router, 'change', this.handleRoute);
            this.listenTo(store.get('content'), 'change:currentWorkspace', this.updatePanelOne);
            var debouncedUpdatePanelTwo = _.debounce(this.updatePanelTwo, debounceTime);
            this.listenTo(store.get('content'), 'change:query', debouncedUpdatePanelTwo);
            this.listenTo(store.getSelectedResults(), 'update',debouncedUpdatePanelTwo);
            this.listenTo(store.getSelectedResults(), 'add', debouncedUpdatePanelTwo);
            this.listenTo(store.getSelectedResults(), 'remove', debouncedUpdatePanelTwo);
            this.listenTo(store.getSelectedResults(), 'reset', debouncedUpdatePanelTwo);
            this.handleRoute();
        },
        handleRoute: function(){
            if (router.toJSON().name==='openWorkspace'){
                this.$el.removeClass('is-hidden');
            } else {
                this.$el.addClass('is-hidden');
            }
        },
        onRender: function(){
            this.updatePanelOne();
            this.hidePanelTwo();
            this.menu.show(new MenuView());
            if (this._mapView){
                this.panelThree.show(this._mapView);
            }
        },
        updatePanelOne: function(workspace){
            if (workspace){
                if (Object.keys(workspace.changedAttributes())[0] === 'currentWorkspace'){
                    this.updatePanelOne();
                }
            } else {
                this.panelOne.show(new WorkspaceContentTabsView({
                    model: new WorkspaceContentTabs()
                }));
                this.hidePanelTwo();
            }
        },
        updatePanelTwo: function(){
            var queryRef = store.getQuery();
            var selectedResults = store.getSelectedResults();
            if (queryRef === undefined && selectedResults.length === 0){
                this.hidePanelTwo();
            } else if (selectedResults.length === 1) {
                this.showPanelTwo();
                if (!this.panelTwo.currentView || this.panelTwo.currentView.constructor !== MetacardTabsView) {
                    this.panelTwo.show(new MetacardTabsView());
                }
                this.panelTwoTitle.show(new MetacardTitleView({
                    model: selectedResults
                }));
            } else if (selectedResults.length > 1) {
                this.showPanelTwo();
                if (!this.panelTwo.currentView || this.panelTwo.currentView.constructor !== MetacardsTabsView) {
                    this.panelTwo.show(new MetacardsTabsView());
                }
                this.panelTwoTitle.show(new MetacardTitleView({
                    model: selectedResults
                }));
            } else if (queryRef !== undefined) {
                this.updatePanelTwoQueryTitle();
                this.showPanelTwo();
                if (!this.panelTwo.currentView || this.panelTwo.currentView.constructor !== QueryTabsView) {
                    this.panelTwo.show(new QueryTabsView());
                }
            } else {
                this.hidePanelTwo();
            }
            Common.repaintForTimeframe(500, function(){
                wreqr.vent.trigger('resize');
                $(window).trigger('resize');
            });
        },
        updatePanelTwoQueryTitle: function(){
            var queryRef = store.getQuery();
            var title = queryRef._cloneOf === undefined ? 'New Query' : queryRef.get('title');
            this.$el.find('.content-panelTwo-title').html(title);
        },
        hidePanelTwo: function(){
            this.$el.addClass('hide-panelTwo');
            Common.repaintForTimeframe(500, function(){
                wreqr.vent.trigger('resize');
                $(window).trigger('resize');
            });
        },
        showPanelTwo: function(){
            this.$el.removeClass('hide-panelTwo');
        },
        unselectQueriesAndResults: function(){
            store.get('content').set('query', undefined);
            store.clearSelectedResults();
        },
        _mapView: undefined

    });

    return ContentView;
});
