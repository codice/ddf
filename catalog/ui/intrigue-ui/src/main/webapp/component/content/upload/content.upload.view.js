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
/*global define, window*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    'js/CustomElements',
    '../content.view',
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
    'component/upload/upload',
    'component/result-selector/result-selector.view',
    'component/visualization/visualization.view'
], function (wreqr, Marionette, _, $, CustomElements, ContentView, MenuView, properties,
             WorkspaceContentTabs, WorkspaceContentTabsView, QueryTabsView, store,
             MetacardTabsView, MetacardsTabsView, Common, MetacardTitleView, uploadInstance,
            ResultSelectorView, VisualizationView) {

    var debounceTime = 25;

    return ContentView.extend({
        className: 'is-upload',
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
                selectionInterface: uploadInstance
            });
            this.listenTo(uploadInstance, 'change:currentUpload', this.updatePanelOne);
            var debouncedUpdatePanelTwo = _.debounce(this.updatePanelTwo, debounceTime);
            this.listenTo(uploadInstance.getSelectedResults(), 'update',debouncedUpdatePanelTwo);
            this.listenTo(uploadInstance.getSelectedResults(), 'add', debouncedUpdatePanelTwo);
            this.listenTo(uploadInstance.getSelectedResults(), 'remove', debouncedUpdatePanelTwo);
            this.listenTo(uploadInstance.getSelectedResults(), 'reset', debouncedUpdatePanelTwo);
        },
        onRender: function(){
            this.updatePanelOne();
            this.hidePanelTwo();
            if (this._mapView){
                this.panelThree.show(this._mapView);
            }
        },
        updatePanelOne: function(){
            this.panelOne.show(new ResultSelectorView({
                model: uploadInstance.get('currentQuery'),
                selectionInterface: uploadInstance
            }));
            this.hidePanelTwo();
        },
        updatePanelTwo: function(){
            var selectedResults = uploadInstance.getSelectedResults();
            if (selectedResults.length === 0){
                this.hidePanelTwo();
            } else if (selectedResults.length === 1) {
                this.showPanelTwo();
                if (!this.panelTwo.currentView || this.panelTwo.currentView.constructor !== MetacardTabsView) {
                    this.panelTwo.show(new MetacardTabsView({
                        selectionInterface: uploadInstance
                    }));
                }
                this.panelTwoTitle.show(new MetacardTitleView({
                    model: selectedResults
                }));
            } else {
                this.showPanelTwo();
                if (!this.panelTwo.currentView || this.panelTwo.currentView.constructor !== MetacardsTabsView) {
                    this.panelTwo.show(new MetacardsTabsView({
                        selectionInterface: uploadInstance
                    }));
                }
                this.panelTwoTitle.show(new MetacardTitleView({
                    model: selectedResults
                }));
            }
            Common.repaintForTimeframe(500, function(){
                wreqr.vent.trigger('resize');
                $(window).trigger('resize');
            });
        },
        unselectQueriesAndResults: function(){
            uploadInstance.clearSelectedResults();
        },
        _mapView: undefined

    });
});