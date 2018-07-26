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

const Marionette = require('marionette');
const Cesium = require('cesium');
const _ = require('underscore');
const wreqr = require('wreqr');
const NotificationView = require('./notification.view');
const ShapeUtils = require('js/ShapeUtils');
const DrawingController = require('./drawing.controller');
const Turf = require('@turf/turf');
const DistanceUtils = require('js/DistanceUtils');

"use strict";

const THRESHOLD = 8000000;

const createBufferedPolygonPoints = (polygonPoints, model) => {
    const width = DistanceUtils.getDistanceInMeters(model.toJSON().polygonBufferWidth, model.get('polygonBufferUnits')) || 1;

    return Turf.buffer(Turf.lineString(polygonPoints), Math.max(width, 1), 'meters');
}

const getCurrentMagnitudeFromView = (view) => {
    return view.options.map.camera.getMagnitude();
}

const needsRedraw = (view) => {
    const currentMagnitude = getCurrentMagnitudeFromView(view);

    if (view.cameraMagnitude < THRESHOLD && currentMagnitude > THRESHOLD) {
        return true;
    }
    if (view.cameraMagnitude > THRESHOLD && currentMagnitude < THRESHOLD) {
        return true;
    }

    return false
}

const Draw = {};

Draw.PolygonRenderView = Marionette.View.extend({
    cameraMagnitude: undefined,
    animationFrameId: undefined,
    initialize: function() {
        this.listenTo(this.model, 'change:polygon change:polygonBufferWidth, change:polygonBufferUnits', this.updatePrimitive);
        this.updatePrimitive(this.model);
        this.listenForCameraChange();
    },
    modelEvents: {
        'changed': 'updatePrimitive'
    },
    updatePrimitive() {
        this.drawPolygon(this.model);
    },
    drawPolygon(model) {
        const json = model.toJSON();
        const isMultiPolygon =  ShapeUtils.isArray3D(json.polygon);
        const polygons = isMultiPolygon ? json.polygon : [ json.polygon ];

        const color = this.model.get('color');

        // first destroy old one
        if (this.primitive && !this.primitive.isDestroyed()) {
            this.options.map.scene.primitives.remove(this.primitive);
        }

        this.primitive = new Cesium.PolylineCollection();
        this.cameraMagnitude = this.options.map.camera.getMagnitude();

        (polygons || []).forEach(function(polygonPoints){
            if (!polygonPoints || polygonPoints.length < 3) {
                return;
            }
            if (polygonPoints[0].toString() !== polygonPoints[polygonPoints.length - 1].toString()) {
                polygonPoints.push(polygonPoints[0]);
            }
            const bufferedPolygonPoints = createBufferedPolygonPoints(polygonPoints, this.model);
            const primitive = this.primitive;
            bufferedPolygonPoints.geometry.coordinates.forEach(set => 
                primitive.add({
                    width: 8,
                    material: Cesium.Material.fromType('PolylineOutline', {
                        color: color ? Cesium.Color.fromCssColorString(color) : Cesium.Color.KHAKI,
                        outlineColor: Cesium.Color.WHITE,
                        outlineWidth: 4
                    }),
                    id: 'userDrawing',
                    positions: Cesium.Cartesian3.fromDegreesArray(_.flatten(set))
                })
            );
        }.bind(this));

        this.options.map.scene.primitives.add(this.primitive);
    },
    listenForCameraChange() {
        this.options.map.scene.camera.moveStart.addEventListener(this.handleCameraMoveStart, this);
        this.options.map.scene.camera.moveEnd.addEventListener(this.handleCameraMoveEnd, this);
    },
    handleCameraMoveStart() {
        this.animationFrameId = window.requestAnimationFrame(function() {
            if (needsRedraw(this)){
                this.updatePrimitive();
            }
            this.handleCameraMoveStart();
        }.bind(this));
    },
    handleCameraMoveEnd() {
        window.cancelAnimationFrame(this.animationFrameId);
        if (needsRedraw(this)) {
            this.updatePrimitive();
        }
    },
    destroy() {
        this.options.map.scene.camera.moveStart.removeEventListener(this.handleCameraMoveStart, this);
        this.options.map.scene.camera.moveEnd.removeEventListener(this.handleCameraMoveEnd, this);
        window.cancelAnimationFrame(this.animationFrameId);
        if (this.primitive) {
            this.options.map.scene.primitives.remove(this.primitive);
        }
        this.remove(); // backbone cleanup.
    }
});

Draw.Controller = DrawingController.extend({
    drawingType: 'poly',
    show(model) {
        if (!this.enabled) {
            return;
        }
        this.options.drawHelper.stopDrawing();
        // remove old polygon
        const existingView = this.getViewForModel(model);
        if (existingView) {
            existingView.destroy();
            this.removeViewForModel(model);
        }
        const view = new Draw.PolygonRenderView({
            model: model,
            map: this.options.map
        });
        this.addView(view);
    },
    draw(model) {
        const controller = this;
        const toDeg = Cesium.Math.toDegrees;
        if (!this.enabled) {
            return;
        }
        // start polygon draw.
        this.notificationView = new NotificationView({
            el: this.options.notificationEl
        }).render();
        this.options.drawHelper.startDrawingPolygon({
            callback: function (positions) {

                if (controller.notificationView) {
                    controller.notificationView.destroy();
                }
                const latLonRadPoints = _.map(positions, function (cartPos) {
                    const latLon = controller.options.map.scene.globe.ellipsoid.cartesianToCartographic(cartPos);
                    return [toDeg(latLon.longitude), toDeg(latLon.latitude)];
                });

                //this shouldn't ever get hit because the draw library should protect against it, but just in case it does, remove the point
                if (latLonRadPoints.length > 3 && latLonRadPoints[latLonRadPoints.length - 1][0] === latLonRadPoints[latLonRadPoints.length - 2][0] &&
                    latLonRadPoints[latLonRadPoints.length - 1][1] === latLonRadPoints[latLonRadPoints.length - 2][1]) {
                    latLonRadPoints.pop();
                }

                model.set('polygon', latLonRadPoints);

                // doing this out of formality since bbox/circle call this after drawing has ended.
                model.trigger('EndExtent', model);

                // lets go ahead and show our new shiny polygon.
                wreqr.vent.trigger('search:polydisplay', model);
            }
        });
    }
});

module.exports = Draw;