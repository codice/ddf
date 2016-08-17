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
var Cesium = require('cesium');
var shapes = require('component/visualization/cesium/shapes');
var utility = require('./utility.js');
var _ = require('underscore');
var calculateConvexHull = require('geo-convex-hull');

var ClusterView = Marionette.ItemView.extend({
  template: false,
  geometry: undefined,
  convexHull: undefined,
  selectionInterface: store,
  geoController: undefined,
  initialize: function (options) {
    this.geometry = [];
    this.selectionInterface = options.selectionInterface || this.selectionInterface;
    this.geoController = options.geoController;
    this.handleCluster();
    this.addConvexHull();
    this.updateSelected();
    this.listenTo(this.selectionInterface.getSelectedResults(), 'update add remove reset', this.updateSelected);
  },
  handleCluster: function () {
    var center = utility.calculateCartographicCenterOfGeometriesInDegrees(this.model.get('results').map(function (result) {
      return result.get('metacard').get('geometry');
    }));
    this.geometry.push(shapes.addPointWithText(center, this.geoController, {
      id: this.model.get('results').map(function (result) {
        return result.id;
      }),
      color: this.model.get('results').first().get('metacard').get('color'),
      view: this
    }));
  },
  addConvexHull: function () {
    var points = this.model.get('results').map(function (result) {
      return result.get('metacard').get('geometry').getAllPoints();
    });
    var data = _.flatten(points, true).map(function(coord){
      return {
        longitude: coord[0],
        latitude: coord[1]
      };
    });
    var convexHull = calculateConvexHull(data).map(function(coord){
      return [coord.longitude, coord.latitude];
    });
    convexHull.push(convexHull[0]);
    var geometry = shapes.addLine(convexHull, this.geoController, {
      id: this.model.get('results').map(function (result) {
        return result.id;
      }),
      color: this.model.get('results').first().get('metacard').get('color'),
      view: this
    });
    this.convexHull = geometry.get(0);
    this.convexHull.show = false;
    this.geometry.push(geometry);
  },
  handleHover: function(id){
    if (id && (this.convexHull.id.toString() === id.toString())) {
      this.convexHull.show = true;
    } else {
      this.convexHull.show = false;
    }
  },
  updateSelected: function () {
    var selected = 0;
    this.model.get('results').forEach(function(result){
        if (this.selectionInterface.getSelectedResults().get(result)){
          selected++;
        }
    }.bind(this));
    if (selected === this.model.get('results').length){
        this.showFullySelected();
    } else if (selected > 0){
        this.showPartiallySelected();
    } else {
        this.showUnselected();
    }
  },
  showFullySelected: function () {
    shapes.updateCluster(this.geometry, {
      color: this.model.get('results').first().get('metacard').get('color'),
      isSelected: true,
      count: this.model.get('results').length,
      outline: 'black',
      textFill: 'black'
    });
  },
  showPartiallySelected: function(){
    shapes.updateCluster(this.geometry, {
      color: this.model.get('results').first().get('metacard').get('color'),
      isSelected: false,
      count: this.model.get('results').length,
      outline: 'black',
      textFill: 'white'
    });
  },
  showUnselected: function(){
    shapes.updateCluster(this.geometry, {
      color: this.model.get('results').first().get('metacard').get('color'),
      isSelected: false,
      count: this.model.get('results').length,
      outline: 'white',
      textFill: 'white'
    });
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

module.exports = ClusterView;