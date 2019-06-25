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

// #Main Application
define(['marionette', 'icanhaz'], function(Marionette, ich) {
  'use strict'

  var Application = {}

  // This was moved from the main.js file into here.
  // Since this modules has ui components, and it gets loaded before main.js, we need to init the renderer here for now until we sort this out.
  Marionette.Renderer.render = function(template, data) {
    if (!template) {
      return ''
    }
    return ich[template](data)
  }

  Application.App = new Marionette.Application()

  //add regions
  Application.App.addRegions({
    mainRegion: 'main',
  })

  Application.App.addInitializer(function() {
    require(['js/module'])
  })

  return Application
})
