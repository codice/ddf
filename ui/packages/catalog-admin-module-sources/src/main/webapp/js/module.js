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
  'js/view/Source.view.js',
  'poller',
  'js/model/Source.js',
  'js/model/Status.js',
  'js/model/Service.js',
], function(wreqr, Application, SourceView, poller, Source, Status, Service) {
  Application.App.module('Sources', function(
    SourceModule,
    App,
    Backbone,
    Marionette
  ) {
    const serviceModel = new Service.Response();
    serviceModel.fetch()

    const sourceResponse = new Source.Response({
      model: serviceModel,
    });

    const sourcePage = new SourceView.SourcePage({
      model: sourceResponse,
    });

    // Define a controller to run this module
    // --------------------------------------

    const Controller = Marionette.Controller.extend({
      initialize: function(options) {
        this.region = options.region
      },

      show: function() {
        this.region.show(sourcePage)
      },
    });

    // Initialize this module when the app starts
    // ------------------------------------------

    SourceModule.addInitializer(function() {
      SourceModule.contentController = new Controller({
        region: App.mainRegion,
      })
      SourceModule.contentController.show()
    })
  })
})
