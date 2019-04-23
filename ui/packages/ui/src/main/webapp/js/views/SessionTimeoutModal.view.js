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
/* global define */
define([
  'jquery',
  'backbone',
  'js/wreqr',
  'js/views/Modal',
  'js/models/SessionTimeout',
  'properties',
  'templates/sessionTimeoutModal.handlebars',
], function(
  $,
  Backbone,
  wreqr,
  Modal,
  sessionTimeoutModel,
  properties,
  sessionTimeoutModalTemplate
) {
  return Modal.extend({
    template: sessionTimeoutModalTemplate,
    model: null,

    events: {
      'click button': 'renewSession',
    },
    initialize: function() {},
    onRender: function() {
      setTimeout(this.refreshTimeLeft.bind(this), 1000)
    },
    refreshTimeLeft: function() {
      if (!this.isClosed) {
        this.render()
      }
    },
    serializeData: function() {
      return {
        timeLeft: sessionTimeoutModel.getIdleSeconds(),
      }
    },
    renewSession: function() {
      sessionTimeoutModel.renew()
    },
  })
})
