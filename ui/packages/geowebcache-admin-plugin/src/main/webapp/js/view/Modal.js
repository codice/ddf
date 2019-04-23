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
define(['backbone.marionette'], function(Marionette) {
  'use strict'
  /**
   * This provides us with a base view for modals. It contains the base close/hide and destroy functionality.
   */
  const BaseModal = Marionette.LayoutView.extend({
    constructor: function() {
      this.className = 'modal fade ' + this.className // add on modal specific classes.
      Marionette.LayoutView.prototype.constructor.apply(this, arguments)
    },
    initialize: function() {
      // destroy view after animation completes.
      // extending views must call: Modal.prototype.initialize.apply(this, arguments);
      const view = this
      this.$el.one('hidden.bs.modal', function() {
        view.destroy()
      })
    },
    show: function() {
      this.$el.modal({
        backdrop: 'static',
        keyboard: false,
      })
    },
    hide: function() {
      this.$el.modal('hide')
    },
  })
  return BaseModal
})
