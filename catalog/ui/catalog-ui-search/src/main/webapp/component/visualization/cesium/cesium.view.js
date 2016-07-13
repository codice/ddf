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
    './cesium.hbs'
], function (wreqr, Marionette, CustomElements, template) {

    return Marionette.LayoutView.extend({
        tagName: CustomElements.register('cesium'),
        template: template,
        regions: {
            mapDrawingPopup: '#mapDrawingPopup'
        },
        events: {
            'click .cluster-results': 'toggleClustering'
        },
        onShow: function () {
            require([
                'js/controllers/cesium.controller',
                'js/widgets/cesium.bbox',
                'js/widgets/cesium.circle',
                'js/widgets/cesium.polygon',
                'js/widgets/filter.cesium.geometry.group'
            ], function (GeoController, DrawBbox, DrawCircle, DrawPolygon, FilterCesiumGeometryGroup) {
                var geoController = new GeoController({
                    element: this.el.querySelector('#cesiumContainer'),
                    selectionInterface: this.options.selectionInterface
                });
                geoController._billboardPromise.then(function() {
                    this.setupListeners(geoController);
                }.bind(this));
                new FilterCesiumGeometryGroup.Controller({ geoController: geoController });
                new DrawBbox.Controller({
                    scene: geoController.scene,
                    notificationEl: this.mapDrawingPopup.el
                });
                new DrawCircle.Controller({
                    scene: geoController.scene,
                    notificationEl: this.mapDrawingPopup.el
                });
                new DrawPolygon.Controller({
                    scene: geoController.scene,
                    notificationEl: this.mapDrawingPopup.el,
                    drawHelper: geoController.drawHelper,
                    geoController: geoController
                });
                this.geoController = geoController;
            }.bind(this));
        },
        toggleClustering: function () {
            this.geoController.toggleClustering();
        },
        setupListeners: function (geoController) {
            geoController.listenTo(this.options.selectionInterface, 'reset:activeSearchResults', geoController.newActiveSearchResults);
            geoController.listenTo(this.options.selectionInterface.getSelectedResults(), 'update', geoController.zoomToSelected);
            geoController.listenTo(this.options.selectionInterface.getSelectedResults(), 'add', geoController.zoomToSelected);
            geoController.listenTo(this.options.selectionInterface.getSelectedResults(), 'remove', geoController.zoomToSelected);
            geoController.listenTo(wreqr.vent, 'search:mapshow', geoController.flyToLocation);

            if (this.options.selectionInterface.getActiveSearchResults()) {
                geoController.newActiveSearchResults(this.options.selectionInterface.getActiveSearchResults());
            }
            if (this.options.selectionInterface.getSelectedResults()) {
                geoController.zoomToSelected(this.options.selectionInterface.getSelectedResults());
            }
        }
    });

});