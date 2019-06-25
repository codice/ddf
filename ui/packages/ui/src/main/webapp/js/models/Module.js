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

define(['backbone', 'backbone-relational'], function(Backbone) {
  var Module = {}
  var module = Backbone.RelationalModel.extend({})
  var moduleCollection = Backbone.Collection.extend({ model: module })
  Module.Model = Backbone.RelationalModel.extend({
    url:
      './jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listModules',
    relations: [
      {
        type: Backbone.HasMany,
        key: 'value',
        relatedModel: module,
        collectionType: moduleCollection,
        includeInJSON: false,
      },
    ],
  })

  return Module
})
