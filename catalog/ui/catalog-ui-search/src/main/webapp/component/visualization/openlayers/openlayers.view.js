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
    'text!./openlayers.hbs'
], function (wreqr, Marionette, CustomElements, template) {

    return Marionette.LayoutView.extend({
        tagName: CustomElements.register('openlayers'),
        template: template,
        regions: {
            mapDrawingPopup: '#mapDrawingPopup'
        },
        onShow: function () {
            require([
                'js/controllers/openlayers.controller',
                'js/widgets/openlayers.bbox',
                'js/widgets/openlayers.polygon',
                'js/widgets/filter.openlayers.geometry.group'
            ], function (GeoController, DrawBbox, DrawPolygon, FilterCesiumGeometryGroup) {
                var geoController = new GeoController({
                    element: this.el.querySelector('#cesiumContainer'),
                    selectionInterface: this.options.selectionInterface
                });
                this.setupListeners(geoController);
                new FilterCesiumGeometryGroup.Controller({
                    geoController: geoController
                });
                new DrawBbox.Controller({
                    map: geoController.mapViewer,
                    notificationEl: this.mapDrawingPopup.el
                });
                new DrawPolygon.Controller({
                    map: geoController.mapViewer,
                    notificationEl: this.mapDrawingPopup.el
                });
                this.listenTo(wreqr.vent, 'resize', function () {
                    geoController.mapViewer.updateSize();
                });
            }.bind(this));
        },
        setupListeners: function (geoController) {
            geoController.listenTo(this.options.selectionInterface, 'reset:activeSearchResults', geoController.newActiveSearchResults);
            geoController.listenTo(wreqr.vent, 'search:mapshow', geoController.flyToLocation);
            geoController.listenTo(wreqr.vent, 'search:maprectanglefly', geoController.flyToRectangle);
            geoController.listenTo(this.options.selectionInterface.getSelectedResults(), 'update', geoController.zoomToSelected);
            geoController.listenTo(this.options.selectionInterface.getSelectedResults(), 'add', geoController.zoomToSelected);
            geoController.listenTo(this.options.selectionInterface.getSelectedResults(), 'remove', geoController.zoomToSelected);

            if (this.options.selectionInterface.getActiveSearchResults()) {
                geoController.newActiveSearchResults(this.options.selectionInterface.getActiveSearchResults());
            }
            if (this.options.selectionInterface.getSelectedResults()) {
                geoController.zoomToSelected(this.options.selectionInterface.getSelectedResults());
            }
        }
    });

});