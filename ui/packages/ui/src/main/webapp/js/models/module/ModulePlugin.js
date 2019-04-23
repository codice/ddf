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

define(['backbone', 'underscore'], function(Backbone, _) {
  const ModulePlugin = {}

  ModulePlugin.Model = Backbone.Model.extend({})

  ModulePlugin.Collection = Backbone.Collection.extend({
    model: ModulePlugin.Model,
    url:
      './jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/getPluginsForModule(java.lang.String)/',
    fetchByModuleName: function(moduleName, options) {
      const collection = this

      const newOptions = _.extend(
        {
          url: collection.url + moduleName,
        },
        options
      )
      return this.fetch(newOptions)
    },
    parse: function(resp) {
      return resp.value
    },
    comparator: function(model) {
      return model.get('order')
    },
  })

  return ModulePlugin
})
