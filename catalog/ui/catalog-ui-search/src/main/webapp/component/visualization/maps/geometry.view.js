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
var _ = require('underscore');

var GeometryView = Marionette.ItemView.extend({
    template: false,
    geometry: undefined,
    initialize: function(options) {
        var geometry = this.model.get('metacard').get('geometry');
        if (geometry) {
            this.geometry = [];
            this.handleGeometry(geometry.toJSON());
            this.updateSelected();
            this.listenTo(this.options.selectionInterface.getSelectedResults(), 'update add remove reset', this.updateSelected);
            this.listenTo(this.options.clusterCollection, 'add remove update', this.checkIfClustered);
        }
    },
    handleGeometry: function(geometry) {
        switch (geometry.type) {
            case 'Point':
                this.handlePoint(geometry.coordinates);
                break;
            case 'Polygon':
                geometry.coordinates.forEach(function(polygon) {
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
                geometry.coordinates.forEach(function(line) {
                    this.handlePoint(line[0]);
                    this.handleLine(line);
                }.bind(this));
                break;
            case 'MultiPoint':
                geometry.coordinates.forEach(function(point) {
                    this.handlePoint(point);
                }.bind(this));
                break;
            case 'MultiPolygon':
                geometry.coordinates.forEach(function(multipolygon) {
                    multipolygon.forEach(function(polygon) {
                        this.handlePoint(polygon[0]);
                        this.handleLine(polygon);
                        //this.handlePolygon(polygon);
                    }.bind(this));
                }.bind(this));
                break;
            case 'GeometryCollection':
                geometry.geometries.forEach(function(subgeometry) {
                    this.handleGeometry(subgeometry);
                }.bind(this));
                break;
        }
    },
    handlePoint: function(point) {
        this.geometry.push(this.options.map.addPoint(point, {
            id: this.model.id,
            color: this.model.get('metacard').get('color'),
            view: this
        }));
    },
    handleLine: function(line) {
        this.geometry.push(this.options.map.addLine(line, {
            id: this.model.id,
            color: this.model.get('metacard').get('color'),
            view: this
        }));
    },
    handlePolygon: function(polygon) {
        this.geometry = this.geometry.concat(this.options.map.addPolygon(polygon, {
            id: this.model.id,
            color: this.model.get('metacard').get('color'),
            view: this
        }));
    },
    updateSelected: function() {
        var selected = this.options.selectionInterface.getSelectedResults().some(function(result) {
            return result.id === this.model.id;
        }, this);
        if (selected) {
            this.updateDisplay(true);
        } else {
            this.updateDisplay(false);
        }
    },
    updateDisplay: function(isSelected) {
        this.geometry.forEach(function(geometry) {
            this.options.map.updateGeometry(geometry, {
                color: this.model.get('metacard').get('color'),
                isSelected: isSelected
            });
        }.bind(this));
    },
    checkIfClustered: function() {
        if (this.options.clusterCollection.isClustered(this.model)) {
            this.hideGeometry();
        } else {
            this.showGeometry();
        }
    },
    showGeometry: function() {
        this.geometry.forEach(function(geometry) {
            this.options.map.showGeometry(geometry);
        }.bind(this));
    },
    hideGeometry: function() {
        this.geometry.forEach(function(geometry) {
            this.options.map.hideGeometry(geometry);
        }.bind(this));
    },
    onDestroy: function() {
        if (this.geometry) {
            this.geometry.forEach(function(geometry) {
                this.options.map.removeGeometry(geometry);
            }.bind(this));
        }
    }
});

module.exports = GeometryView;