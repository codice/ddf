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
  'backbone',
  'marionette',
  'icanhaz',
  'underscore',
  'text!templates/login.handlebars',
  'jquery',
  'purl',
], function(Backbone, Marionette, ich, _, loginTemplate, $) {
  ich.addTemplate('loginTemplate', loginTemplate)

  var Login = {}

  Login.LoginForm = Marionette.ItemView.extend({
    template: 'loginTemplate',
    events: {
      'click .btn-upsignin': 'logInUser',
      'click .btn-pkisignin': 'logInPki',
      'click .btn-guestsignin': 'logInGuest',
      'click .btn-upclear': 'clearFields',
      'keypress #username': 'logInEnter',
      'keypress #password': 'logInEnter',
    },
    initialize: function() {
      this.model = new Backbone.Model(window.idpState)
    },
    onRender: function() {
      this.selectors = [
        '#username',
        '#password',
        '.btn-upsignin',
        '.btn-upclear',
        '.btn-pkisignin',
        '.btn-guestsignin',
      ]
      setTimeout(() => {
        this.$el.find('#username').focus()
      }, 0)
    },
    logInEnter: function(e) {
      this.$('#loginerrorup').hide()
      if (e.keyCode === 13) {
        this.logInUser()
      }
    },
    logInUser: function() {
      this.model.set('AuthMethod', 'up')
      this.logIn()
    },
    logInPki: function() {
      this.model.set('AuthMethod', 'pki')
      this.logIn()
    },
    logInGuest: function() {
      this.model.set('AuthMethod', 'guest')
      this.logIn()
    },
    logIn: function() {
      var view = this
      var sentData
      $.ajax({
        type: 'GET',
        url: './login/sso',
        data: this.model.toJSON(),
        beforeSend: function(xhr) {
          view.showSpinnerAndDisableInput()
          if (view.model.get('AuthMethod') === 'up') {
            var base64 = window.btoa(
              view.$('#username').val() + ':' + view.$('#password').val()
            )
            xhr.setRequestHeader('Authorization', 'Basic ' + base64)
          }
        },
        error: function() {
          $('#loginerror' + view.model.get('AuthMethod')).show()
          view.hideSpinnerAndEnableInput()
        },
        success: function(data) {
          sentData = data
        },
      }).then(function() {
        if (sentData) {
          window.document.open('text/html', 'replace')
          window.document.write(sentData)
          window.document.close()
        }
      })
    },
    showSpinnerAndDisableInput: function() {
      $('#loginspinner').show()
      this.selectors.forEach(function(selector) {
        $(selector).attr('disabled', true)
      })
    },
    hideSpinnerAndEnableInput: function() {
      $('#loginspinner').hide()
      this.selectors.forEach(function(selector) {
        $(selector).attr('disabled', false)
      })
    },
    setErrorState: function() {
      this.$('#password').focus(function() {
        this.select()
      })
    },
    clearFields: function() {
      this.$('#username').val('')
      this.$('#password').val('')
      this.$('#loginerrorup').hide()
    },
  })

  return Login
})
