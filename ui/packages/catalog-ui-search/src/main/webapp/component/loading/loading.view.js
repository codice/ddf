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
/*global define, alert*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./loading.hbs')
const CustomElements = require('../../js/CustomElements.js')

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('loading'),
  initialize: function() {
    this.render()
    $('body').append(this.el)
    this.$el.animate(
      {
        opacity: 0.6,
      },
      500,
      function() {
        this.shown = true
        this.$el.trigger('shown.' + this.cid)
      }.bind(this)
    )
  },
  shown: false,
  remove: function() {
    if (this.shown) {
      this.$el.animate(
        {
          opacity: 0,
        },
        500,
        function() {
          this.destroy()
          this.$el.remove()
        }.bind(this)
      )
    } else {
      this.$el.one(
        'shown.' + this.cid,
        function() {
          this.remove()
        }.bind(this)
      )
    }
  },
})
