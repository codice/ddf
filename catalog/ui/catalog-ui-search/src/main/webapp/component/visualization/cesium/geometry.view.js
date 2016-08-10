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
/*global require*/
var Marionette = require('marionette');
var store = require('js/store');
var shapes = require('component/visualization/cesium/shapes');

var GeometryView = Marionette.ItemView.extend({
  template: false,
  geometry: undefined,
  selectionInterface: store,
  geoController: undefined,
  initialize: function (options) {
    this.selectionInterface = options.selectionInterface || this.selectionInterface;
    this.geoController = options.geoController;
    var geometry = this.model.get('metacard').get('geometry');
    if (geometry) {
      this.geometry = [];
      this.handleGeometry(geometry.toJSON());
      this.listenTo(this.selectionInterface.getSelectedResults(), 'update', this.updateSelected);
      this.listenTo(this.selectionInterface.getSelectedResults(), 'add', this.updateSelected);
      this.listenTo(this.selectionInterface.getSelectedResults(), 'remove', this.updateSelected);
      this.listenTo(this.selectionInterface.getSelectedResults(), 'reset', this.updateSelected);
    }
  },
  handleGeometry: function (geometry) {
    switch (geometry.type) {
    case 'Point':
      this.handlePoint(geometry.coordinates);
      break;
    case 'Polygon':
      geometry.coordinates.forEach(function (polygon) {
        this.handlePoint(polygon[0]);
        this.handleLine(polygon);
        //this.handlePolygon(polygon);
      }.bind(this));
      break;
    case 'LineString':
      this.handlePoint(geometry.coordinates[0]);
      this.handleLine(geometry.coordinates);
      break;
    case 'MultiLineString':
      geometry.coordinates.forEach(function (line) {
        this.handlePoint(line[0]);
        this.handleLine(line);
      }.bind(this));
      break;
    case 'MultiPoint':
      geometry.coordinates.forEach(function (point) {
        this.handlePoint(point);
      }.bind(this));
      break;
    case 'MultiPolygon':
      geometry.coordinates.forEach(function (multipolygon) {
        multipolygon.forEach(function (polygon) {
          this.handlePoint(polygon[0]);
          this.handleLine(polygon);
          //this.handlePolygon(polygon);
        }.bind(this));
      }.bind(this));
      break;
    case 'GeometryCollection':
      geometry.geometries.forEach(function (subgeometry) {
        this.handleGeometry(subgeometry);
      }.bind(this));
      break;
    }
  },
  handlePoint: function (point) {
    this.geometry.push(shapes.addPoint(point, this.geoController, {
      id: this.model.id,
      color: this.model.get('metacard').get('color'),
      view: this
    }));
  },
  handleLine: function (line) {
    this.geometry.push(shapes.addLine(line, this.geoController, {
      id: this.model.id,
      color: this.model.get('metacard').get('color'),
      view: this
    }));
  },
  handlePolygon: function (polygon) {
    this.geometry = this.geometry.concat(shapes.addPolygon(polygon, this.geoController, {
      id: this.model.id,
      color: this.model.get('metacard').get('color'),
      view: this
    }));
  },
  updateSelected: function () {
    var selected = this.selectionInterface.getSelectedResults().some(function (result) {
      return result.id === this.model.id;
    }, this);
    if (selected) {
      this.updateDisplay(true);
    } else {
      this.updateDisplay(false);
    }
  },
  updateDisplay: function (isSelected) {
    this.geometry.forEach(function (geometry) {
      shapes.updateGeometry(geometry, {
        color: this.model.get('metacard').get('color'),
        isSelected: isSelected
      });
    }.bind(this));
  },
  onDestroy: function () {
    if (this.geometry) {
      this.geometry.forEach(function (geometry) {
        this.geoController.billboardCollection.remove(geometry);
        this.geoController.scene.primitives.remove(geometry);
        this.geoController.mapViewer.entities.remove(geometry);
      }.bind(this));
    }
  }
});

module.exports = GeometryView;