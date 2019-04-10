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

define([
  'wreqr',
  'js/application',
  'js/view/Registry.view.js',
  'js/model/Node.js',
], function(wreqr, Application, RegistryView, Node) {
  Application.App.module('Registry', function(
    RegistryModule,
    App,
    Backbone,
    Marionette
  ) {
    var nodeModels = new Node.Models()
    nodeModels.fetch()

    var registryPage = new RegistryView.RegistryPage({ model: nodeModels })

    var Controller = Marionette.Controller.extend({
      initialize: function(options) {
        this.region = options.region
      },

      show: function() {
        this.region.show(registryPage)
      },
    })

    RegistryModule.addInitializer(function() {
      RegistryModule.contentController = new Controller({
        region: App.mainRegion,
      })
      RegistryModule.contentController.show()
    })
  })
})
