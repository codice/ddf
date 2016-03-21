/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/* global define, require */
define([
    'application',
    'cometdinit',
    'marionette',
    'maptype',
    'text!templates/map.handlebars'
], function (Application, Cometd, Marionette, maptype, map) {
    Application.App.module('MapModule', function (MapModule) {
        var mapView;
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
                            notificationEl: mapView.mapDrawingPopup.el
                        });
                        new DrawCircle.Controller({
                            scene: geoController.scene,
                            notificationEl: mapView.mapDrawingPopup.el
                        });
                        new DrawPolygon.Controller({
                            scene: geoController.scene,
                            notificationEl: mapView.mapDrawingPopup.el,
                            drawHelper: geoController.drawHelper,
                            geoController: geoController
                        });
                    });
                }
            });
            mapView = new Map3d();
        } else if (maptype.is2d()) {
            var Map2d = Marionette.LayoutView.extend({
                template: map,
                className: 'height-full',
                regions: { mapDrawingPopup: '#mapDrawingPopup' },
                onShow: function () {
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
                            notificationEl: mapView.mapDrawingPopup.el
                        });
                        new DrawPolygon.Controller({
                            map: geoController.mapViewer,
                            notificationEl: mapView.mapDrawingPopup.el
                        });
                    });
                }
            });
            mapView = new Map2d();
        }
        if (mapView) {
            MapModule.addInitializer(function () {
                MapModule.contentController = new Controller({ region: Application.App.mapRegion });
                MapModule.contentController.show();
            });
            var Controller = Marionette.Controller.extend({
                initialize: function (options) {
                    this.region = options.region;
                },
                show: function () {
                    this.region.show(mapView);
                }
            });
        }
    });
});
