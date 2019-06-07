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
  'jquery',
  'underscore',
  'backbone.marionette',
  'js/wreqr',
  'js/views/SessionTimeoutModal.view',
  'js/models/SessionTimeout',
], function($, _, Marionette, wreqr, SessionTimeoutView, SessionTimeout) {
  'use strict'
  var ModalController
  var sessionTimeoutView = null

  ModalController = Marionette.Controller.extend({
    initialize: function(options) {
      this.application = options.application
      this.listenTo(wreqr.vent, 'showModal', this.showModal)
      this.listenTo(
        SessionTimeout,
        'change:showPrompt',
        this.showSessionTimeoutModal
      )
    },
    showModal: function(modalView) {
      this.application.modalRegion.show(modalView)
      modalView.show()
    },
    showSessionTimeoutModal: function() {
      if (SessionTimeout.get('showPrompt')) {
        sessionTimeoutView = new SessionTimeoutView()
        this.application.sessionTimeoutModalRegion.show(sessionTimeoutView)
        sessionTimeoutView.show()
      } else {
        sessionTimeoutView.destroy()
      }
    },
  })

  return ModalController
})
