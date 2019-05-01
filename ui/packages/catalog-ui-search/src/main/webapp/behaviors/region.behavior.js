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

const Behaviors = require('./Behaviors')
const Marionette = require('marionette')
const $ = require('jquery')
import React from 'react'
import { renderToString } from 'react-dom/server'

const regionIsInitialized = region => region._region !== undefined

const regionExists = ($el, region) => {
  return $el.find(region.selector).length > 0
}

const destroyRegion = region => {
  if (regionIsInitialized(region)) {
    region._region.empty()
    region._region.destroy()
    region._region.$el.remove()
    delete region._region
  }
}

const getOptionsForRegion = region => {
  return region.viewOptions instanceof Function
    ? region.viewOptions()
    : region.viewOptions
}

const getViewForRegion = region => {
  return region.view.prototype._isMarionetteView ? region.view : region.view()
}

/*
    JSX is supported for templating, so it's possible we get jsx here.  However, to attach
    regions we'll need to transform it to static markup.
*/
const transformRenderingToHTMLString = rendering => {
  return React.isValidElement(rendering) ? renderToString(rendering) : rendering
}

const handleRegionsThatExist = ($wrapper, regions) => {
  regions.filter(region => regionExists($wrapper, region)).forEach(region => {
    if (regionIsInitialized(region)) {
      $wrapper
        .find(region.selector)
        .html(region._region.currentView.$el[0].outerHTML)
    } else {
      const view = getViewForRegion(region)
      if (view === null || view === undefined) {
        return
      }
      const options = getOptionsForRegion(region)
      region._region = new Marionette.Region({
        el: $wrapper.find(region.selector),
      })
      region._region.show(new view(options))
    }
  })
}

const handleMissingRegions = ($wrapper, regions) => {
  regions
    .filter(region => regionIsInitialized(region))
    .filter(region => !regionExists($wrapper, region))
    .filter(region => region.destroyIfMissing)
    .forEach(region => destroyRegion(region))
}

const handleRegionShouldUpdate = regions => {
  regions
    .filter(region => region.shouldRegionUpdate)
    .filter(region =>
      region.shouldRegionUpdate(
        region._region ? region._region.currentView : undefined
      )
    )
    .forEach(region => destroyRegion(region))
}

/* 
    An alternative to default marionette regions that allows rerendering without
    loss of regions.  This depends on the react renderer to work.

    This behavior works across handlebars and JSX, but try to transition to JSX if possible.
    To define:
    region: {
        regions: [
            {
                selector: '', // string (REQUIRED) should be unique as well
                view: MarionetteView, // can be a Marionette view or a function that returns one (or undefined),
                viewOptions: {}, // options to pass the view or a function that returns options
                destroyIfMissing: false, // defaults to false if missing, set to true to destroy if a render results in the selector disappearing
                shouldRegionUpdate: false, // defaults to false if missing, set to function if you want to be notified on renders (function will recieve currentView of region if it exists and should return true or false) 
            }
        ]
    }
    If you're dealing with a handlebars template and want to avoid other elements rerendering when you render and remove some part of the DOM, wrap that
    part of the DOM that sometimes disappears in a wrapper element such as a div.
    Alternatively, use JSX and you're free to disappear parts of the dom with much lower probability that untouched parts of the DOM get rerendered as well.
*/
Behaviors.addBehavior(
  'region',
  Marionette.Behavior.extend({
    onBeforeReactAttach(rendering) {
      const $wrapper = $('<div></div>')
      $wrapper.html(transformRenderingToHTMLString(rendering))
      handleRegionShouldUpdate(this.options.regions)
      handleRegionsThatExist($wrapper, this.options.regions)
      handleMissingRegions($wrapper, this.options.regions)
      return $wrapper.html()
    },
    onAfterReactAttach() {
      this.options.regions
        .filter(region => regionIsInitialized(region))
        .filter(region => regionExists(this.view.$el, region))
        .forEach(region => {
          if (
            this.view.$el.find(region.selector).children()[0] !==
            region._region.currentView.el
          ) {
            this.attachRegion(region)
          }
        })
    },
    attachRegion(region) {
      this.view.$el.find(region.selector).html('')
      this.view.$el.find(region.selector).append(region._region.currentView.el)
      region._region.currentView.undelegateEvents()
      region._region.currentView.delegateEvents()
    },
    /* mirror the behavior of marionette (otherwise children views that listen to onAttach will break T_T) */
    onBeforeAttach() {
      this.options.regions
        .filter(region => regionIsInitialized(region))
        .filter(region => regionExists(this.view.$el, region))
        .forEach(region => {
          const displayedViews = Marionette.Region.prototype._displayedViews(
            region._region.currentView
          )
          Marionette.Region.prototype._triggerAttach(displayedViews, 'before:')
        })
    },
    /* mirror the behavior of marionette */
    onAttach() {
      this.options.regions
        .filter(region => regionIsInitialized(region))
        .filter(region => regionExists(this.view.$el, region))
        .forEach(region => {
          const displayedViews = Marionette.Region.prototype._displayedViews(
            region._region.currentView
          )
          Marionette.Region.prototype._triggerAttach(displayedViews)
        })
    },
    onDestroy() {
      this.options.regions.forEach(region => destroyRegion(region))
    },
  })
)
