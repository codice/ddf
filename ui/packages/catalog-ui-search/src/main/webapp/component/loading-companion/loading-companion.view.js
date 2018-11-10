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
/*global define, alert, window*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./loading-companion.hbs')
const CustomElements = require('../../js/CustomElements.js')
const Positioning = require('../../js/Positioning.js')

var loadingCompanions = []

function getLoadingCompanion(linkedView) {
  return loadingCompanions.filter(function(loadingCompanion) {
    return loadingCompanion.options.element === linkedView.el
  })[0]
}

function getElementLoadingCompanion(element) {
  return loadingCompanions.filter(function(loadingCompanion) {
    return loadingCompanion.options.element === element
  })[0]
}

var LoadingCompanionView = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('loading-companion'),
  initialize: function() {
    this.render()
    if (this.options.appendTo) {
      this.options.appendTo.append(this.el)
    } else {
      $('body').append(this.el)
      this.updatePosition()
    }
    this.$el.animate(
      {
        opacity: 0.6,
      },
      500,
      function() {
        this.shown = true
        this.$el.trigger('shown.' + this.cid)
      }.bind(this)
    )
    if (this.options.linkedView) {
      this.listenTo(this.options.linkedView, 'destroy', this.destroy)
    }
  },
  shown: false,
  stop: function() {
    this.$el.stop().animate(
      {
        opacity: 0,
      },
      500,
      function() {
        this.destroy()
      }.bind(this)
    )
  },
  onDestroy: function() {
    this.$el.remove()
  },
  updatePosition: function() {
    window.requestAnimationFrame(
      function() {
        if (this.isDestroyed) {
          return
        }
        if (this.options.linkedView && this.options.linkedView.isDestroyed) {
          return
        }
        var boundingBox = this.options.element.getBoundingClientRect()
        this.$el
          .css('left', boundingBox.left)
          .css('top', boundingBox.top)
          .css('width', boundingBox.width)
          .css('height', boundingBox.height)
        this.$el.toggleClass(
          'is-hidden',
          Positioning.isEffectivelyHidden(this.options.element)
        )
        this.updatePosition()
      }.bind(this)
    )
  },
})

module.exports = {
  loadElement(el) {
    if (!el) {
      throw "Must pass the el you're wanting to have a loader on top of."
    }
    loadingCompanions.push(
      new LoadingCompanionView({
        element: el,
      })
    )
  },
  stopLoadingElement(el) {
    if (!el) {
      throw "Must pass the el you're wanting to have a loader on top of."
    }
    const loadingCompanion = getElementLoadingCompanion(el)
    if (loadingCompanion) {
      loadingCompanion.stop()
      loadingCompanions.splice(loadingCompanions.indexOf(loadingCompanion), 1)
    }
  },
  beginLoading: function(linkedView, appendTo) {
    if (!linkedView) {
      throw "Must pass the view you're calling the loader from."
    }
    // only start loader if the view hasn't already been destroyed.
    if (!linkedView.isDestroyed) {
      if (!getLoadingCompanion(linkedView)) {
        loadingCompanions.push(
          new LoadingCompanionView({
            linkedView: linkedView,
            element: linkedView.el,
            appendTo: appendTo,
          })
        )
      }
    }
  },
  endLoading: function(linkedView) {
    if (!linkedView) {
      throw "Must pass the view you're called the loader from."
    }
    var loadingCompanion = getLoadingCompanion(linkedView)
    if (loadingCompanion) {
      loadingCompanion.stop()
      loadingCompanions.splice(loadingCompanions.indexOf(loadingCompanion), 1)
    }
  },
}
