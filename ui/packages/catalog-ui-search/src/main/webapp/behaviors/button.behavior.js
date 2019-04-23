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

const Behaviors = require('./Behaviors');
const Marionette = require('marionette');

Behaviors.addBehavior(
  'button',
  Marionette.Behavior.extend({
    modelEvents: {
      'change:isEditing': 'onRender',
    },
    events: {
      keydown: 'emulateClick',
      click: 'blur',
    },
    emulateClick: function(e) {
      if (e.target === this.el && (e.keyCode === 13 || e.keyCode === 32)) {
        e.preventDefault()
        e.stopPropagation()
        this.$el.mousedown().click()
      }
    },
    /*
        If there is such a thing as being in edit mode, make sure to only give a tabindex 
        to the element when it is in edit mode.  Otherwise, users will be able to tab to it
        when it is disabled (so to speak).
    */
    onRender: function() {
      if (this.view.model.toJSON().isEditing === false) {
        this.$el.removeAttr('tabindex')
      } else {
        this.$el.attr('tabindex', 0)
      }
    },
  })
)
