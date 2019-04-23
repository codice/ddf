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
  'js/application',
  'js/view/Federation.view.js',
  'js/model/Federation.js',
], function(Application, FederationView, FederationModel) {
  Application.App.module('Federation', function(
    FederationModule,
    App,
    Backbone,
    Marionette
  ) {
    const federationModel = new FederationModel();

    const federationPage = new FederationView({ model: federationModel });

    // Define a controller to run this module
    // --------------------------------------

    const Controller = Marionette.Controller.extend({
      initialize: function(options) {
        this.region = options.region
      },

      show: function() {
        this.region.show(federationPage)
      },
    });

    // Initialize this module when the app starts
    // ------------------------------------------

    FederationModule.addInitializer(function() {
      FederationModule.contentController = new Controller({
        region: App.mainRegion,
      })
      FederationModule.contentController.show()
    })
  })
})
