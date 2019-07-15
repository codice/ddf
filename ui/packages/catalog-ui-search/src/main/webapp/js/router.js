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

/* eslint-disable no-undefined */
const wreqr = require('./wreqr.js')
const _ = require('underscore')
const $ = require('jquery')
const Backbone = require('backbone')
const Application = require('./application.js')
const router = require('../component/router/router')
import ReactRouter from '../react-component/router'
import React from 'react'
import { render } from 'react-dom'
import { routes } from '../extension-points'

// notfound route needs to come at the end otherwise no other routes will work
render(
  <ReactRouter routeDefinitions={routes} />,
  Application.App.router.$el[0]
)

const Router = Backbone.Router.extend({
  preloadRoutes() {
    Object.keys(routes).forEach(this.preloadRoute)
  },
  preloadFragment(fragment) {
    this.preloadRoute(this.getRouteNameFromFragment(fragment))
  },
  preloadRoute(routeName) {
    routes[routeName].preload()
  },
  getRouteNameFromFragment(fragment) {
    return this.routes[
      _.find(Object.keys(this.routes), routePattern => {
        return this._routeToRegExp(routePattern).test(fragment)
      })
    ]
  },
  routes: Object.keys(routes).reduce((routesBlob, key) => {
    const { patterns } = routes[key]
    patterns.forEach(pattern => (routesBlob[pattern] = key))
    return routesBlob
  }, {}),
  initialize() {
    this.listenTo(wreqr.vent, 'router:preload', this.handlePreload)
    this.listenTo(wreqr.vent, 'router:navigate', this.handleNavigate)
    this.on('route', this.onRoute, this)
    /*
            HACK:  listeners for the router aren't setup (such as the onRoute or controller)
                until after initialize is done.  SetTimeout (with timeout of 0) pushes this
                navigate onto the end of the current execution queue
            */
    setTimeout(() => {
      const currentFragment = location.hash
      Backbone.history.fragment = undefined
      this.navigate(currentFragment, { trigger: true })
    }, 0)
  },
  handlePreload({ fragment }) {
    this.preloadFragment(fragment)
  },
  handleNavigate(args) {
    this.navigate(args.fragment, args.options)
  },
  onRoute(name, args) {
    this.updateRoute(name, _.invert(this.routes)[name], args)
  },
  updateRoute(name, path, args) {
    router.set({
      name,
      path,
      args,
    })
    $(window).trigger('resize')
    wreqr.vent.trigger('resize')
  },
})

module.exports = new Router()
