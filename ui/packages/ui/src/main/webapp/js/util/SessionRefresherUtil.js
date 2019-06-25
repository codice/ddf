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

define(['backbone', 'jquery'], function(Backbone, $) {
  var sessionRenewUrl = '../services/internal/session/renew'
  var sessionExpiryUrl = '../services/internal/session/expiry'

  var sessionAutoRenewModel = new (Backbone.Model.extend({
    defaults: {
      sessionRenewDate: undefined,
    },
    initialize: function() {
      this.initializeSessionRenewDate()
      this.listenTo(
        this,
        'change:sessionRenewDate',
        this.handleSessionRenewDate
      )
    },
    initializeSessionRenewDate: function() {
      $.get(sessionExpiryUrl).done(this.handleExpiryTimeResponse.bind(this))
    },
    handleExpiryTimeResponse: function(response) {
      var msUntilTimeout = parseInt(response)
      var msUntilAutoRenew = Math.max(
        msUntilTimeout * 0.7,
        msUntilTimeout - 60000
      ) // 70% or at least one minute before
      this.set('sessionRenewDate', Date.now() + msUntilAutoRenew)
    },
    handleSessionRenewDate: function() {
      this.clearSessionRenewTimer()
      this.setSessionRenewTimer()
    },
    setSessionRenewTimer: function() {
      this.sessionRenewTimer = setTimeout(
        this.renewSession.bind(this),
        this.get('sessionRenewDate') - Date.now()
      )
    },
    clearSessionRenewTimer: function() {
      clearTimeout(this.sessionRenewTimer)
    },
    renewSession: function() {
      $.get(sessionRenewUrl).done(this.handleExpiryTimeResponse.bind(this))
    },
  }))()

  return sessionAutoRenewModel
})
