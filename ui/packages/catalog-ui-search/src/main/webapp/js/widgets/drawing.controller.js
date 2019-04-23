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

const Marionette = require('marionette');
const wreqr = require('../wreqr.js');

module.exports = Marionette.Controller.extend({
  enabled: true,
  drawingType: undefined,
  initialize: function() {
    if (typeof this.drawingType === 'undefined') {
      throw 'drawingType needs to be overwritten'
    }
    this.listenTo(wreqr.vent, `search:${this.drawingType}display`, function(
      model
    ) {
      this.show(model)
    })
    this.listenTo(wreqr.vent, `search:draw${this.drawingType}`, function(
      model
    ) {
      this.draw(model)
    })
    this.listenTo(wreqr.vent, 'search:drawstop', function(model) {
      this.stop(model)
    })
    this.listenTo(wreqr.vent, 'search:drawend', function(model) {
      this.destroyByModel(model)
    })
    this.listenTo(wreqr.vent, 'search:destroyAllDraw', function(model) {
      this.destroyAll(model)
    })
  },
  views: [],
  destroyAll: function() {
    for (let i = this.views.length - 1; i >= 0; i -= 1) {
      this.destroyView(this.views[i])
    }
  },
  getViewForModel: function(model) {
    return this.views.filter(
      function(view) {
        return view.model === model && view.options.map === this.options.map
      }.bind(this)
    )[0]
  },
  removeViewForModel: function(model) {
    const view = this.getViewForModel(model);
    if (view) {
      this.views.splice(this.views.indexOf(view), 1)
    }
  },
  removeView: function(view) {
    this.views.splice(this.views.indexOf(view), 1)
  },
  addView: function(view) {
    this.views.push(view)
  },
  show: function() {
    throw 'show needs to be overwritten'
  },
  draw: function() {
    throw 'draw needs to be overwritten'
  },
  stop: function(model) {
    const view = this.getViewForModel(model);
    if (view && view.stop) {
      view.stop()
    }
    if (this.enabled && this.options.drawHelper) {
      this.options.drawHelper.stopDrawing()
    }
    if (this.notificationView) {
      this.notificationView.destroy()
    }
  },
  destroyView: function(view) {
    if (view.stop) {
      view.stop()
    }
    if (view.destroyPrimitive) {
      view.destroyPrimitive()
    }
    if (view.destroy) {
      view.destroy()
    }
    this.removeView(view)
  },
  destroyByModel: function(model) {
    this.stop(model)
    const view = this.getViewForModel(model);
    if (view) {
      this.destroyView(view)
      if (this.notificationView) {
        this.notificationView.destroy()
      }
    }
  },
  onBeforeDestroy: function() {
    this.destroyAll()
  },
})
