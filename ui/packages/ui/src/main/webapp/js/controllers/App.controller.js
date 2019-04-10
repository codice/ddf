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
  'backbone.marionette',
  'js/wreqr',
  'components/applications/applications.view',
  'js/models/ApplicationsLayout',
], function(Marionette, wreqr, ApplicationGrid, ApplicationsLayout) {
  var AppController = Marionette.Controller.extend({
    initialize: function(options) {
      this.regions = options.regions
      this.listenTo(wreqr.vent, 'navigateTo:applicationHome', this.show)
    },

    show: function() {
      this.regions.applications.show(
        new ApplicationGrid({
          modelClass: ApplicationsLayout,
          enableApplicationRemoval: true,
        })
      )
    },
  })

  return AppController
})
