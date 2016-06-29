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
    'js/CustomElements',
    '../content.view',
    'component/navigation/workspace/navigation.workspace.view',
    'properties',
    'component/tabs/workspace-content/tabs-workspace-content',
    'component/tabs/workspace-content/tabs-workspace-content.view',
    'component/tabs/query/tabs-query.view',
    'maptype',
    'text!templates/map.handlebars',
    'js/store',
    'component/tabs/metacard/tabs-metacard.view',
    'component/tabs/metacards/tabs-metacards.view',
    'js/Common',
    'component/metacard-title/metacard-title.view',
    'component/alert/alert',
    'component/result-selector/result-selector.view'
], function (wreqr, Marionette, _, $, CustomElements, ContentView, MenuView, properties,
             WorkspaceContentTabs, WorkspaceContentTabsView, QueryTabsView, maptype, map, store,
             MetacardTabsView, MetacardsTabsView, Common, MetacardTitleView, alertInstance,
            ResultSelectorView) {

    var debounceTime = 25;

    return ContentView.extend({
        className: 'is-alert',
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
            var contentView = this;
            if (maptype.is3d()) {
                var Map3d = Marionette.LayoutView.extend({
                    template: map,
                    className: 'height-full',
                    regions: { mapDrawingPopup: '#mapDrawingPopup' },
                    events: { 'click .cluster-results': 'toggleClustering' },
                    onShow: function () {
                        var self = this;
                        require([
                            'js/controllers/cesium.controller',
                            'js/widgets/cesium.bbox',
                            'js/widgets/cesium.circle',
                            'js/widgets/cesium.polygon',
                            'js/widgets/filter.cesium.geometry.group'
                        ], function (GeoController, DrawBbox, DrawCircle, DrawPolygon, FilterCesiumGeometryGroup) {
                            var geoController = new GeoController({
                                element: self.el.querySelector('#cesiumContainer'),
                                selectionInterface: alertInstance
                            });
                                geoController._billboardPromise.then(function() {
                                    self.setupListeners(geoController);
                                });
                            new FilterCesiumGeometryGroup.Controller({ geoController: geoController });
                            new DrawBbox.Controller({
                                scene: geoController.scene,
                                notificationEl: contentView._mapView.mapDrawingPopup.el
                            });
                            new DrawCircle.Controller({
                                scene: geoController.scene,
                                notificationEl: contentView._mapView.mapDrawingPopup.el
                            });
                            new DrawPolygon.Controller({
                                scene: geoController.scene,
                                notificationEl: contentView._mapView.mapDrawingPopup.el,
                                drawHelper: geoController.drawHelper,
                                geoController: geoController
                            });
                            self.geoController = geoController;
                        });
                    },
                    toggleClustering: function () {
                        this.geoController.toggleClustering();
                    },
                    setupListeners: function(geoController){
                        geoController.listenTo(alertInstance, 'reset:activeSearchResults', geoController.newActiveSearchResults);
                        if (alertInstance.getActiveSearchResults()) {
                            geoController.newActiveSearchResults(alertInstance.getActiveSearchResults());
                        }
                        geoController.listenTo(alertInstance.getSelectedResults(), 'update', geoController.zoomToSelected);
                        geoController.listenTo(alertInstance.getSelectedResults(), 'add', geoController.zoomToSelected);
                        geoController.listenTo(alertInstance.getSelectedResults(), 'remove', geoController.zoomToSelected);

                        geoController.listenTo(wreqr.vent, 'search:mapshow', geoController.flyToLocation);
                    }
                });
                this._mapView = new Map3d();
            } else if (maptype.is2d()) {
                var Map2d = Marionette.LayoutView.extend({
                    template: map,
                    className: 'height-full',
                    regions: { mapDrawingPopup: '#mapDrawingPopup' },
                    onShow: function () {
                        var map2d = this;
                        require([
                            'js/controllers/openlayers.controller',
                            'js/widgets/openlayers.bbox',
                            'js/widgets/openlayers.polygon',
                            'js/widgets/filter.openlayers.geometry.group'
                        ], function (GeoController, DrawBbox, DrawPolygon, FilterCesiumGeometryGroup) {
                            var geoController = new GeoController({
                                element: map2d.el.querySelector('#cesiumContainer'),
                                selectionInterface: alertInstance
                            });
                            map2d.setupListeners(geoController);
                            new FilterCesiumGeometryGroup.Controller({ geoController: geoController });
                            new DrawBbox.Controller({
                                map: geoController.mapViewer,
                                notificationEl: contentView._mapView.mapDrawingPopup.el
                            });
                            new DrawPolygon.Controller({
                                map: geoController.mapViewer,
                                notificationEl: contentView._mapView.mapDrawingPopup.el
                            });
                            map2d.listenTo(wreqr.vent, 'resize', function(){
                                geoController.mapViewer.updateSize();
                            });
                        });
                    },
                    setupListeners: function(geoController){
                        geoController.listenTo(alertInstance, 'reset:activeSearchResults', geoController.newActiveSearchResults);
                        if (alertInstance.getActiveSearchResults()) {
                            geoController.newActiveSearchResults(alertInstance.getActiveSearchResults());
                        }

                        geoController.listenTo(wreqr.vent, 'search:mapshow', geoController.flyToLocation);
                        geoController.listenTo(wreqr.vent, 'search:maprectanglefly', geoController.flyToRectangle);
                        geoController.listenTo(alertInstance.getSelectedResults(), 'update', geoController.zoomToSelected);
                        geoController.listenTo(alertInstance.getSelectedResults(), 'add', geoController.zoomToSelected);
                        geoController.listenTo(alertInstance.getSelectedResults(), 'remove', geoController.zoomToSelected);
                    }
                });
                this._mapView = new Map2d();
            }
            this.listenTo(alertInstance, 'change:currentAlert', this.updatePanelOne);
            var debouncedUpdatePanelTwo = _.debounce(this.updatePanelTwo, debounceTime);
            this.listenTo(alertInstance.getSelectedResults(), 'update',debouncedUpdatePanelTwo);
            this.listenTo(alertInstance.getSelectedResults(), 'add', debouncedUpdatePanelTwo);
            this.listenTo(alertInstance.getSelectedResults(), 'remove', debouncedUpdatePanelTwo);
            this.listenTo(alertInstance.getSelectedResults(), 'reset', debouncedUpdatePanelTwo);
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
                model: alertInstance.get('currentQuery'),
                selectionInterface: alertInstance
            }));
            this.hidePanelTwo();
        },
        updatePanelTwo: function(){
            var selectedResults = alertInstance.getSelectedResults();
            if (selectedResults.length === 0){
                this.hidePanelTwo();
            } else if (selectedResults.length === 1) {
                this.showPanelTwo();
                if (!this.panelTwo.currentView || this.panelTwo.currentView.constructor !== MetacardTabsView) {
                    this.panelTwo.show(new MetacardTabsView({
                        selectionInterface: alertInstance
                    }));
                }
                this.panelTwoTitle.show(new MetacardTitleView({
                    model: selectedResults
                }));
            } else {
                this.showPanelTwo();
                if (!this.panelTwo.currentView || this.panelTwo.currentView.constructor !== MetacardsTabsView) {
                    this.panelTwo.show(new MetacardsTabsView({
                        selectionInterface: alertInstance
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
            alertInstance.clearSelectedResults();
        },
        _mapView: undefined

    });
});