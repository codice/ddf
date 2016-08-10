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
var $ = require('jquery');
var _ = require('underscore');
var store = require('js/store');
var GeometryView = require('./geometry.view');

var GeometryCollectionView = Marionette.CollectionView.extend({
  childView: GeometryView,
  selectionInterface: store,
  childViewOptions: function () {
    return {
      geoController: this.options.geoController,
      selectionInterface: this.selectionInterface
    };
  },
  initialize: function (options) {
    this.render = _.debounce(this.render, 200);
    this.selectionInterface = options.selectionInterface || this.selectionInterface;
    this.listenTo(this.options.geoController, 'click:left', this.onMapLeftClick);
    this.listenTo(this.options.geoController, 'hover', this.onMapHover);
    this.render();
  },
  onMapHover: function (event) {
    $(this.options.geoController.mapViewer.canvas).toggleClass('is-hovering', Boolean(event.id));
  },
  onMapLeftClick: function (event) {
    if (event.id) {
      if (event.shiftKey) {
        this.handleShiftClick(event.id);
      } else if (event.ctrlKey) {
        this.handleCtrlClick(event.id);
      } else {
        this.handleClick(event.id);
      }
    }
  },
  handleClick: function (id) {
    this.selectionInterface.clearSelectedResults();
    this.selectionInterface.addSelectedResult(this.collection.get(id));
  },
  handleCtrlClick: function (id) {
    this.selectionInterface.addSelectedResult(this.collection.get(id));
  },
  handleShiftClick: function (id) {
    this.selectionInterface.addSelectedResult(this.collection.get(id));
  }
});

module.exports = GeometryCollectionView;