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

require.config({
  paths: {
    bootstrap: '../../webjars/bootstrap/3.3.7/dist/js/bootstrap.min',
    bootstrapselect:
      '../../webjars/bootstrap-select/1.6.4/dist/js/bootstrap-select.min',

    moment: '../../webjars/moment/2.20.1/min/moment.min',
    perfectscrollbar:
      '../../webjars/perfect-scrollbar/0.7.0/js/perfect-scrollbar.jquery.min',
    q: '../../webjars/q/1.4.1/q',

    // backbone
    backbone: '../../webjars/backbone/1.1.2/backbone',
    backboneassociations:
      '../../webjars/backbone-associations/0.6.2/backbone-associations-min',
    underscore: '../../webjars/underscore/1.8.3/underscore-min',
    marionette: '../../webjars/marionette/2.4.5/lib/backbone.marionette',
    // TODO test combining
    modelbinder:
      '../../webjars/backbone.modelbinder/1.1.0/Backbone.ModelBinder.min',
    collectionbinder:
      '../../webjars/backbone.modelbinder/1.1.0/Backbone.CollectionBinder.min',

    // application
    application: 'js/application',
    properties: 'properties',

    // jquery
    jquery: '../../webjars/jquery/2.2.4/dist/jquery.min',
    jqueryui: '../../webjars/jquery-ui/1.12.1/jquery-ui.min',
    multiselect:
      '../../webjars/jquery-ui-multiselect-widget/1.14/src/jquery.multiselect',
    multiselectfilter:
      '../../webjars/lib/multiselect/src/jquery.multiselect.filter',
    fileupload: '../../webjars/blueimp-file-upload/9.28.0/js/jquery.fileupload',

    // handlebars
    handlebars: '../../webjars/handlebars/4.0.10/handlebars.min',
    icanhaz: 'js/ich',

    // require plugins
    text: '../../webjars/requirejs-plugins/1.0.3/lib/text',
    css: '../../webjars/require-css/0.1.10/css.min',
  },
  map: {
    '*': {
      'jquery.ui.widget': 'jqueryui',
      'jquery-ui/ui/widget': 'jqueryui',
    },
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
    backboneassociations: ['backbone'],
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
      deps: ['jquery', 'handlebars'],
      exports: 'ich',
    },

    moment: {
      exports: 'moment',
    },

    datepicker: ['jquery', 'jqueryuiCore'],
    datepickerOverride: ['datepicker'],
    datepickerAddon: ['datepicker'],
    progressbar: ['jquery', 'jqueryuiCore', 'jquery.ui.widget'],
    multiselect: ['jquery', 'jquery.ui.widget'],
    multiselectfilter: ['jquery', 'multiselect'],
    fileupload: ['jquery', 'jquery.ui.widget'],

    perfectscrollbar: ['jquery'],

    bootstrap: ['jquery'],

    bootstrapselect: ['bootstrap'],
  },

  waitSeconds: 0,
})

require.onError = function(err) {
  if (typeof console !== 'undefined') {
    console.error('RequireJS failed to load a module', err)
  }
}

require([
  'underscore',
  'backbone',
  'application',
  'marionette',
  'icanhaz',
  'js/HandlebarsHelpers',
  'bootstrap',
], function(_, Backbone, app, Marionette, ich) {
  'use strict'

  Marionette.Renderer.render = function(template, data) {
    if (!template) {
      return ''
    }
    return ich[template](data)
  }

  // Add anti-CSRF header to outgoing calls
  Backbone.$.ajaxSetup({
    headers: {
      'X-Requested-With': 'XMLHttpRequest',
    },
  })

  // Actually start up the application.
  app.App.start({})
})
