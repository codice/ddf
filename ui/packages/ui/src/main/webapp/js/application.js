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

/* jslint browser:true */
// #Main Application
import Theme from '../components/container/theme'
import { patchListenTo } from '@connexta/atlas/extensions/backbone'
import {
  addOnFirstRender,
  switchRenderToReact,
} from '@connexta/atlas/extensions/marionette'
patchListenTo()
addOnFirstRender()
switchRenderToReact({ aggressive: false, Provider: Theme })

define([
  'underscore',
  'backbone',
  'backbone.marionette',
  '@connexta/ace/handlebars',
  'jquery',
  'poller',
  'js/wreqr',
  'js/models/Module',
  'templates/appHeader.handlebars',
  'js/controllers/Modal.controller',
  'js/controllers/SystemUsage.controller',
  'properties',
], function(
  _,
  Backbone,
  Marionette,
  hbs,
  $,
  poller,
  wreqr,
  Module,
  appHeader,
  ModalController,
  SystemUsageController,
  Properties
) {
  var Application = {}

  Application.App = new Marionette.Application()

  Application.Controllers = {
    modalController: new ModalController({ application: Application.App }),
  }

  //add regions
  Application.App.addRegions({
    pageHeader: '#pageHeader',
    headerRegion: 'header',
    footerRegion: 'footer',
    mainRegion: 'main',
    appHeader: '#appHeader',
    modalRegion: '#modalRegion',
    sessionTimeoutModalRegion: '#sessionTmeoutModalRegion',
    alertsRegion: '.alerts',
    applications: '#applications',
    docs: '#docs',
    installation: '#installation',
    configuration: '#configurations',
  })

  //setup models
  var options = {
    delay: 30000,
  }

  var addModuleRegions = function() {
    //add tab regions
    Application.ModuleModel.get('value').each(function(module) {
      var obj = {}
      obj[module.get('id')] = '#' + module.get('id')
      if (!Application.App.getRegion(module.get('id'))) {
        Application.App.addRegions(obj)
      }
    })
  }

  Application.ModuleModel = new Module.Model()
  Application.ModuleModel.fetch().done(addModuleRegions)
  Application.AppModel = new Backbone.Model(Properties)
  Application.App.appHeader.show(
    new (Backbone.Marionette.ItemView.extend({
      template: appHeader,
      className: 'app-header',
      tagName: 'div',
      model: Application.AppModel,
      events: {
        'click button': 'logout',
      },
      logout: function() {
        window.location.href = '../logout?service=' + window.location.href
      },
    }))()
  )
  var modulePoller = poller.get(Application.ModuleModel, options)
  modulePoller.on('success', addModuleRegions)

  modulePoller.start()

  wreqr.vent.on('modulePoller:stop', function() {
    modulePoller.stop()
  })

  // show System Notification Banner
  Application.App.addInitializer(function() {
    new SystemUsageController()
  })

  //configure the router (we aren't using this yet)
  Application.Router = Backbone.Router.extend({
    routes: {
      '': 'index',
    },

    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
    },

    index: function() {},
  })

  return Application
})
