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

;(function() {
  'use strict'
  require('less/styles.less')
  require([
    'jquery',
    'backbone',
    'backbone.marionette',
    'js/application',
    'js/views/Module.view',
    'js/models/Alerts.js',
    'js/views/Alerts.view',
    'properties',
    'templates/header.handlebars',
    'templates/footer.handlebars',
    'js/models/SessionTimeout',
    'js/util/SessionRefresherUtil',
    'js/HandlebarsHelpers',
    'modelbinder',
    'bootstrap',
  ], function(
    $,
    Backbone,
    Marionette,
    Application,
    ModuleView,
    AlertsModel,
    AlertsView,
    Properties,
    headerLayout,
    footerLayout
  ) {
    var app = Application.App

    // setup the area that the modules will load into and asynchronously require in each module
    // so that it can render itself into the area that was just constructed for it
    app.addInitializer(function() {
      Application.App.mainRegion.show(
        new ModuleView({ model: Application.ModuleModel })
      )
    })

    // add anti-csrf header to outgoing requests
    $.ajaxSetup({
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
      },
    })

    // setup the header
    app.addInitializer(function() {
      if (Properties.ui.header && Properties.ui.header !== '') {
        $('html').addClass('has-header')
      }
      Application.App.headerRegion.show(
        new Marionette.ItemView({
          template: headerLayout,
          className: 'header-layout',
          model: Application.AppModel,
        })
      )
    })

    // setup alert banners
    app.addInitializer(function() {
      var alerts = new Backbone.Collection([])

      var backendAlerts = new AlertsModel.BackendAlerts()
      backendAlerts.fetch()
      alerts = backendAlerts

      $(document).ajaxError(function(_, jqxhr) {
        if (jqxhr.status === 401 || jqxhr.status === 403) {
          var sessionTimeoutAlert = AlertsModel.Jolokia({
            stacktrace: 'Forbidden',
          })
          // do not show any other alerts if session timeout
          alerts = new Backbone.Collection([sessionTimeoutAlert])
        }
      })

      var AlertsCollectionView = Marionette.CollectionView.extend({
        itemView: AlertsView.View,
        comparator: function(model) {
          return model.get('priority')
        },
      })

      Application.App.alertsRegion.show(
        new AlertsCollectionView({
          collection: alerts,
        })
      )
    })

    // setup the footer
    app.addInitializer(function() {
      if (Properties.ui.footer && Properties.ui.footer !== '') {
        $('html').addClass('has-footer')
      }
      Application.App.footerRegion.show(
        new Marionette.ItemView({
          template: footerLayout,
          className: 'footer-layout',
          model: Application.AppModel,
        })
      )
    })

    // Start up the main Application Router
    app.addInitializer(function() {
      app.router = new Application.Router()
    })

    // once the application has been initialized (i.e. all initializers have completed), start up
    // Backbone.history
    app.on('initialize:after', function() {
      Backbone.history.start()
      //bootstrap call for tabs
      $('tabs').tab()
    })

    if (window) {
      // make ddf object available on window.  Makes debugging in chrome console much easier
      window.app = app
      if (!window.console) {
        window.console = {
          log: function() {
            //no op
          },
        }
      }
    }

    // actually start up the application
    app.start()
  })
})()
