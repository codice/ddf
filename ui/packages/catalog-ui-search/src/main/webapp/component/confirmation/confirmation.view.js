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

const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./confirmation.hbs')
const Confirmation = require('./confirmation')
const CustomElements = require('../../js/CustomElements.js')

module.exports = Marionette.LayoutView.extend(
  {
    template: template,
    tagName: CustomElements.register('confirmation'),
    modelEvents: {
      'change:choice': 'close',
    },
    events: {
      click: 'handleOutsideClick',
      mousedown: 'handleMousedown',
      'click .confirmation-no': 'handleNo',
      'click .confirmation-yes': 'handleYes',
    },
    initialize: function(options) {
      $('body').append(this.el)
      this.render()
      this.handleChoices()
    },
    handleMousedown: function(event) {
      event.stopPropagation()
    },
    handleChoices: function() {
      this.$el.toggleClass(
        'has-two-choices',
        this.model.get('no') !== undefined
      )
    },
    handleOutsideClick: function(event) {
      if (event.target === this.el) {
        this.model.makeChoice(false)
      }
    },
    handleNo: function() {
      this.model.makeChoice(false)
    },
    handleYes: function() {
      this.model.makeChoice(true)
    },
    onRender: function() {
      this.center()
    },
    center: function() {
      var $confirmationContainer = this.$el.find('.confirmation-container')
      var height = $confirmationContainer.height() / 2
      $confirmationContainer.css('top', 'calc(40% - ' + height + 'px)')
    },
    close: function() {
      this.destroy()
    },
  },
  {
    generateConfirmation: function(attributes) {
      var confirmation = new Confirmation(attributes)
      new this({
        model: confirmation,
      })
      return confirmation
    },
  }
)
