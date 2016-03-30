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
    'text!./content.hbs',
    'js/CustomElements',
    'component/tabs/workspace-content/tabs-workspace-content',
    'component/tabs/workspace-content/tabs-workspace-content.view',
    'component/tabs/query/tabs-query.view',
    'maptype',
    'text!templates/map.handlebars',
    'js/store',
    'component/tabs/metacard/tabs-metacard.view'
], function (wreqr, Marionette, _, $, contentTemplate, CustomElements, WorkspaceContentTabs,
             WorkspaceContentTabsView, QueryTabsView, maptype, map, store, MetacardTabsView) {

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
            'panelOne': '.content-panelOne',
            'panelTwo': '.content-panelTwo-content',
            'panelThree': '.content-panelThree'
        },
        initialize: function(){
            $('body').append(this.el);
            var contentView = this;
            if (maptype.is3d()) {
                var Map3d = Marionette.LayoutView.extend({
                    template: map,
                    className: 'height-full',
                    regions: { mapDrawingPopup: '#mapDrawingPopup' },
                    onShow: function () {
                        require([
                            'js/controllers/cesium.controller',
                            'js/widgets/cesium.bbox',
                            'js/widgets/cesium.circle',
                            'js/widgets/cesium.polygon',
                            'js/widgets/filter.cesium.geometry.group'
                        ], function (GeoController, DrawBbox, DrawCircle, DrawPolygon, FilterCesiumGeometryGroup) {
                            var geoController = new GeoController();
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
                        });
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
                            var geoController = new GeoController();
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
                    }
                });
                this._mapView = new Map2d();
            }
            this.listenTo(store.get('workspaces'), 'change:currentWorkspace', this.updatePanelOne);
            this.listenTo(store.get('content'), 'change:query', this.updatePanelTwo);
            this.listenTo(store.getSelectedResults(), 'update', this.updatePanelTwo);
            this.listenTo(store.getSelectedResults(), 'add', this.updatePanelTwo);
            this.listenTo(store.getSelectedResults(), 'remove', this.updatePanelTwo);
            this.listenTo(store.getSelectedResults(), 'reset', this.updatePanelTwo);
        },
        onRender: function(){
            this.updatePanelOne();
            this.hidePanelTwo();
            this.panelThree.show(this._mapView);
        },
        onBeforeShow: function(){
        },
        updatePanelOne: function(){
            this.panelOne.show(new WorkspaceContentTabsView({
                model: new WorkspaceContentTabs()
            }));
            this.hidePanelTwo();
        },
        updatePanelTwo: function(){
            var queryRef = store.getQuery();
            var selectedResults = store.getSelectedResults();
            if (queryRef === undefined && selectedResults.length === 0){
                this.hidePanelTwo();
            } else if (queryRef !== undefined) {
                console.log('query selected');
                this.updatePanelTwoQueryTitle();
                this.showPanelTwo();
                this.panelTwo.show(new QueryTabsView());
            } else {
                console.log('results selected');
                this.updatePanelTwoSelectedResultsTitle();
                this.showPanelTwo();
                this.panelTwo.show(new MetacardTabsView());
            }
            wreqr.vent.trigger('resize');
        },
        updatePanelTwoQueryTitle: function(){
            var queryRef = store.getQuery();
            var title = queryRef._cloneOf === undefined ? 'New Query' : queryRef.get('cql');
            this.$el.find('.content-panelTwo-title').html(title);
        },
        updatePanelTwoSelectedResultsTitle: function(){
            var queryRef = store.getQuery();
            var title = 'Metacard';
            this.$el.find('.content-panelTwo-title').html(title);
        },
        hidePanelTwo: function(){
            this.$el.addClass('hide-panelTwo');
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
