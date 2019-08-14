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

  require.config({
    paths: {
      bootstrap: '../../../webjars/bootstrap/3.3.7/dist/js/bootstrap.min',
      q: '../../../webjars/q/1.4.1/q',

      // backbone
      backbone: '../../../webjars/backbone/1.1.2/backbone',
      backboneassociation:
        '../../../webjars/backbone-associations/0.6.2/backbone-associations-min',
      underscore: '../../../webjars/underscore/1.8.3/underscore-min',
      marionette:
        '../../../webjars/marionette/1.8.8/lib/backbone.marionette.min',
      modelbinder:
        '../../../webjars/backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
      poller: '../../../webjars/backbone-poller/1.1.3/backbone.poller',

      // jquery
      jquery: '../../../webjars/jquery/3.2.1/dist/jquery.min',
      jqueryui: '../../../webjars/jquery-ui/1.12.1/jquery-ui.min',

      // handlebars
      handlebars: '../../../webjars/handlebars/4.0.10/handlebars.min',
      icanhaz: 'js/ich',

      // require plugins
      text: '../../../webjars/requirejs-plugins/1.0.3/lib/text',
    },

    shim: {
      backbone: {
        deps: ['underscore', 'jquery'],
        exports: 'Backbone',
      },
      modelbinder: {
        deps: ['underscore', 'jquery', 'backbone'],
      },
      collectionbinder: {
        deps: ['modelbinder'],
      },
      poller: {
        deps: ['underscore', 'backbone'],
      },
      backbonerelational: ['backbone'],
      backboneassociation: ['backbone'],
      marionette: {
        deps: ['jquery', 'underscore', 'backbone'],
        exports: 'Marionette',
      },
      underscore: {
        exports: '_',
      },
      handlebars: {
        exports: 'Handlebars',
      },
      icanhaz: {
        deps: ['handlebars', 'jquery'],
        exports: 'ich',
      },

      perfectscrollbar: ['jquery'],

      multiselect: ['jquery'],

      jqueryui: ['jquery'],
      bootstrap: ['jqueryui'],
    },

    waitSeconds: 200,
  })

  require([
    'jquery',
    'backbone',
    'marionette',
    'icanhaz',
    'js/application',
    'js/HandlebarsHelpers',
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

    require(['js/module'], function() {})
  })
})()
