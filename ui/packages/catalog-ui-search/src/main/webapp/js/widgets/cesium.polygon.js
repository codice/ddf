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

const Cesium = require('cesium');
const ShapeUtils = require('js/ShapeUtils');
const Turf = require('@turf/turf');
const DistanceUtils = require('js/DistanceUtils');

const { GeometryRenderView, GeometryController } = require('./cesium.base.line');

"use strict";

const createBufferedPolygonPoints = (polygonPoints, model) => {
    const width = DistanceUtils.getDistanceInMeters(model.toJSON().polygonBufferWidth, model.get('polygonBufferUnits')) || 1;

    return Turf.buffer(Turf.lineString(polygonPoints), Math.max(width, 1), 'meters');
}

class PolygonRenderView extends GeometryRenderView {
    constructor(options) {
        super({...options, modelEvents: {'changed': 'updatePrimitive'}});

        this.cameraMagnitude;
        this.animationFrameId;

        this.init({...options, events: 'change:polygon change:polygonBufferWidth change:polygonBufferUnits'});
    }
    
    init = (options) => {
        const { model, events } = {...options};
        this.listenTo(model, events, this.updatePrimitive);
        this.updatePrimitive();
        this.listenForCameraChange();
    }

    drawGeometry = (model) => {
        const json = model.toJSON();
        const isMultiPolygon =  ShapeUtils.isArray3D(json.polygon);
        const polygons = isMultiPolygon ? json.polygon : [ json.polygon ];

        // first destroy old one
        if (this.primitive && !this.primitive.isDestroyed()) {
            this.map.scene.primitives.remove(this.primitive);
        }

        this.primitive = new Cesium.PolylineCollection();
        this.cameraMagnitude = this.map.camera.getMagnitude();

        (polygons || []).forEach(function(polygonPoints){
            if (!polygonPoints || polygonPoints.length < 3) {
                return;
            }
            if (polygonPoints[0].toString() !== polygonPoints[polygonPoints.length - 1].toString()) {
                polygonPoints.push(polygonPoints[0]);
            }
            const bufferedPolygonPoints = createBufferedPolygonPoints(polygonPoints, this.model);
            const primitive = this.primitive;
            bufferedPolygonPoints.geometry.coordinates.forEach(set => primitive.add(this.constructLinePrimitive(set)));
        }.bind(this));

        this.map.scene.primitives.add(this.primitive);
    }
}

class PolygonController extends GeometryController {
    constructor(options) {
        super({...options, drawingType: 'poly', geometry: 'polygon', geometryModel: 'polygon', geometryDisplay: 'polydisplay'});
    }

    createRenderView = (model) => {
        return new PolygonRenderView({model: model, map: this.map});
    }
}

module.exports = {
    PolygonRenderView: PolygonRenderView,
    Controller: PolygonController
};