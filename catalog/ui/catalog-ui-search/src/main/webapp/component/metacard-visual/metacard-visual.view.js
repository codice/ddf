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
    'text!./metacard-visual.hbs',
    'js/CustomElements',
    'js/store',
    'maptype',
    'text!templates/map.handlebars',
    'component/metacard/metacard'
], function (wreqr, Marionette, _, $, template, CustomElements, store, maptype, map, metacardInstance) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('metacard-visual'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
            metacardVisual: '.metacard-visual'
        },
        initialize: function(){
            this.listenTo(store.get('router'), 'change', this.handleRoute);
            this.handleRoute();
            var contentView = this;
            if (maptype.is3d()) {
                var Map3d = Marionette.LayoutView.extend({
                    template: map,
                    className: 'height-full',
                    regions: { mapDrawingPopup: '#mapDrawingPopup' },
                    onShow: function () {
                        var self = this;
                        require([
                            'js/controllers/cesium.controller'
                        ], function (GeoController) {
                            var geoController = new GeoController({
                                element: self.el.querySelector('#cesiumContainer')
                            });
                            geoController._billboardPromise.then(function(){
                                geoController.showResult(metacardInstance.get('currentMetacard'));
                                geoController.zoomToResult(metacardInstance.get('currentMetacard'));
                                self.setupListeners(geoController);
                            });
                        });
                    },
                    setupListeners: function(geoController){
                        geoController.listenTo(store.get('router'), 'change', this.handleMetacardChange)
                    },
                    handleMetacardChange: function(){
                        this.showResult(metacardInstance.get('currentMetacard'));
                        this.zoomToResult(metacardInstance.get('currentMetacard'));
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
                            'js/controllers/openlayers.controller'
                        ], function (GeoController) {
                            var geoController = new GeoController({
                                element: map2d.el.querySelector('#cesiumContainer')
                            });
                            geoController.showResult(metacardInstance.get('currentMetacard'));
                            geoController.zoomToResult(metacardInstance.get('currentMetacard'));
                            map2d.setupListeners(geoController);
                            map2d.listenTo(wreqr.vent, 'resize', function(){
                                geoController.mapViewer.updateSize();
                            });
                        });
                    },
                    setupListeners: function(geoController){
                        geoController.listenTo(store.get('router'), 'change', this.handleMetacardChange)
                    },
                    handleMetacardChange: function(){
                        this.showResult(metacardInstance.get('currentMetacard'));
                        this.zoomToResult(metacardInstance.get('currentMetacard'));
                    }
                });
                this._mapView = new Map2d();
            }
        },
        handleRoute: function(){
            var router = store.get('router').toJSON();
            if (router.name === 'openMetacard'){
                this.$el.removeClass('is-hidden');
            } else {
                this.$el.addClass('is-hidden');
            }
        },
        onRender: function(){
            if (this._mapView){
                this.metacardVisual.show(this._mapView);
            }
        }
    });
});
