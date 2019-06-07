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
define([
  'marionette',
  'backbone',
  'js/view/Login.view',
  'text!templates/appHeader.handlebars',
  'icanhaz',
], function(Marionette, Backbone, Login, appHeader, ich) {
  'use strict'

  ich.addTemplate('appHeader', appHeader)

  var Application = {}

  Application.App = new Marionette.Application()

  Application.AppModel = new Backbone.Model()

  //add regions
  Application.App.addRegions({
    mainRegion: 'main',
    headerRegion: '#appHeader',
  })

  Application.App.addInitializer(function() {
    Application.App.mainRegion.show(new Login.LoginForm())
  })

  Application.App.addInitializer(function() {
    Application.AppModel.fetch({ url: '../services/platform/config/ui' }).done(
      function() {
        Application.App.headerRegion.show(
          new Marionette.ItemView({
            template: 'appHeader',
            model: Application.AppModel,
            className: 'app-header',
          })
        )
      }
    )
  })

  return Application
})
