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

// eslint-disable-next-line no-extra-semi
;(function() {
  'use strict'

  require('./less/styles.less')
  require([
    'jquery',
    'backbone',
    'marionette',
    'icanhaz',
    'js/application',
    '../../../../ui/src/main/webapp/js/HandlebarsHelpers',
    'modelbinder',
    'bootstrap',
  ], function($, Backbone, Marionette, ich, Application) {
    var app = Application.App
    // Once the application has been initialized (i.e. all initializers have completed), start up
    // Backbone.history.
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
            // no op
          },
        }
      }
    }

    // Actually start up the application.
    app.start()

    require(['js/module'], function() {
      $('#page-loading').addClass('hide')
    })
  })
})()
