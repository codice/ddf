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

const NotificationView = Marionette.View.extend({
  render() {
    if (this.rendered) {
      this.$el.hide('fast')
    }
    this.$el.empty()
    // if it gets any more complicated than this, then we should move to templates
    this.$el.append('<span>Please draw area of interest.</span>')
    this.$el.animate(
      {
        height: 'show',
      },
      425
    )
    this.rendered = true
    return this
  },
  destroy() {
    this.$el.animate(
      {
        height: 'hide',
      },
      425
    )
  },
})

module.exports = NotificationView
