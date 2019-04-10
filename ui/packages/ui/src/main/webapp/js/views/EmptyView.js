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

define(['backbone.marionette', 'templates/emptyView.handlebars'], function(
  Marionette,
  emptyViewTemplate
) {
  var EmptyView = {}

  EmptyView.view = Marionette.ItemView.extend({
    template: emptyViewTemplate,
    initialize: function(options) {
      this.message = options.message
    },
    serializeData: function() {
      return { message: this.message }
    },
  })

  EmptyView.services = Marionette.ItemView.extend({
    template: emptyViewTemplate,
    serializeData: function() {
      return { message: 'There are no services currently configured.' }
    },
  })

  return EmptyView
})
