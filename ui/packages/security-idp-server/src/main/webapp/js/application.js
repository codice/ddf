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
  'text!templates/header.layout.handlebars',
  'text!templates/footer.layout.handlebars',
  'jquery',
  'icanhaz',
  'js/HandlebarsHelpers',
], function(
  Marionette,
  Backbone,
  Login,
  appHeaderTemplate,
  headerTemplate,
  footerTemplate,
  $,
  ich
) {
  'use strict'

  ich.addTemplate('appHeaderTemplate', appHeaderTemplate)
  ich.addTemplate('headerTemplate', headerTemplate)
  ich.addTemplate('footerTemplate', footerTemplate)

  var Application = {}

  Application.App = new Marionette.Application()

  Application.App.props = {}

  $.ajax({
    async: false, // must be synchronous to guarantee that no tests are run before fixture is loaded
    cache: false,
    dataType: 'json',
    url: '../platform/config/ui',
  })
    .done(function(uiConfig) {
      Application.App.props.ui = uiConfig
    })
    .fail(function(jqXHR, status, errorThrown) {
      if (console) {
        console.log(
          'Platform UI Configuration could not be loaded: (status: ' +
            status +
            ', message: ' +
            errorThrown.message +
            ')'
        )
      }
    })

  //add regions
  Application.App.addRegions({
    mainRegion: 'main',
    header: 'header',
    footer: 'footer',
    appHeader: '#appHeader',
  })

  Application.App.addInitializer(function() {
    Application.App.mainRegion.show(new Login.LoginForm())
  })

  Application.App.addInitializer(function() {
    Application.App.appHeader.show(
      new Marionette.ItemView({
        template: 'appHeaderTemplate',
        model: new Backbone.Model(Application.App.props.ui),
      })
    )
  })

  Application.App.addInitializer(function() {
    if (
      Application.App.props.ui.header &&
      Application.App.props.ui.header !== ''
    ) {
      $('html').addClass('has-header')
    }
    Application.App.header.show(
      new Marionette.ItemView({
        template: 'headerTemplate',
        model: new Backbone.Model(Application.App.props.ui),
      })
    )
  })

  Application.App.addInitializer(function() {
    if (
      Application.App.props.ui.footer &&
      Application.App.props.ui.footer !== ''
    ) {
      $('html').addClass('has-footer')
    }
    Application.App.footer.show(
      new Marionette.ItemView({
        template: 'footerTemplate',
        model: new Backbone.Model(Application.App.props.ui),
      })
    )
  })

  return Application
})
