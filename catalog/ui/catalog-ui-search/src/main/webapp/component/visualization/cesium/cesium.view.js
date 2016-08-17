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
    'js/CustomElements',
    './cesium.hbs',
    'component/loading-companion/loading-companion.view',
    'js/store'
], function (wreqr, Marionette, CustomElements, template, LoadingCompanionView, store) {
    return Marionette.LayoutView.extend({
        tagName: CustomElements.register('cesium'),
        template: template,
        regions: {
            mapDrawingPopup: '#mapDrawingPopup'
        },
        events: {
            'click .cesium-cluster-button': 'toggleClustering'
        },
        clusterCollection: undefined,
        clusterCollectionView: undefined,
        geometryCollectionView: undefined,
        geocontroller: undefined,
        initialize: function(){
            this.listenTo(store.get('content'), 'change:drawing', this.handleDrawing);
            this.handleDrawing();
        },
        onShow: function () {
            LoadingCompanionView.beginLoading(this);
            setTimeout(function () {
                require([
                    'js/controllers/cesium.controller',
                    'js/widgets/cesium.bbox',
                    'js/widgets/cesium.circle',
                    'js/widgets/cesium.polygon',
                    'js/widgets/cesium.line',
                    'js/widgets/filter.cesium.geometry.group',
                    'component/visualization/cesium/geometry.collection.view',
                    'component/visualization/cesium/cluster.collection.view',
                    'component/visualization/cesium/cluster.collection'
                ], function (GeoController, DrawBbox, DrawCircle, DrawPolygon, DrawLine, FilterCesiumGeometryGroup,
                             GeometryCollectionView, ClusterCollectionView, ClusterCollection) {
                    this.geocontroller = new GeoController({
                        element: this.el.querySelector('#cesiumContainer'),
                        selectionInterface: this.options.selectionInterface
                    });
                    new FilterCesiumGeometryGroup.Controller({geoController: this.geocontroller});
                    new DrawBbox.Controller({
                        scene: this.geocontroller.scene,
                        notificationEl: this.mapDrawingPopup.el
                    });
                    new DrawCircle.Controller({
                        scene: this.geocontroller.scene,
                        notificationEl: this.mapDrawingPopup.el
                    });
                    new DrawPolygon.Controller({
                        scene: this.geocontroller.scene,
                        notificationEl: this.mapDrawingPopup.el,
                        drawHelper: this.geocontroller.drawHelper,
                        geoController: this.geocontroller
                    });
                    new DrawLine.Controller({
                        scene: this.geocontroller.scene,
                        notificationEl: this.mapDrawingPopup.el,
                        drawHelper: this.geocontroller.drawHelper,
                        geoController: this.geocontroller
                    });
                    this.clusterCollection = new ClusterCollection();
                    this.geometryCollectionView = new GeometryCollectionView({
                        collection: this.options.selectionInterface.getActiveSearchResults(),
                        geoController: this.geocontroller,
                        selectionInterface: this.options.selectionInterface,
                        clusterCollection: this.clusterCollection
                    });
                    this.clusterCollectionView = new ClusterCollectionView({
                        collection: this.clusterCollection,
                        geoController: this.geocontroller,
                        selectionInterface: this.options.selectionInterface
                    });
                    LoadingCompanionView.endLoading(this);
                }.bind(this));
            }.bind(this), 1000);
        },
        toggleClustering: function () {
            this.$el.toggleClass('is-clustering');
            this.clusterCollectionView.toggleActive();
        },
        handleDrawing: function(){
            this.$el.toggleClass('is-drawing', store.get('content').get('drawing'));
        }
    });
});
