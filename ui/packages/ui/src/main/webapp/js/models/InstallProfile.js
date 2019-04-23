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
  const InstallProfile = {}

  InstallProfile.Model = Backbone.Model.extend({
    toJSON: function() {
      const modelJSON = _.clone(this.attributes)
      return _.extend(modelJSON, {
        displayName: modelJSON.name.replace('profile-', ''),
      })
    },
  })

  InstallProfile.Collection = Backbone.Collection.extend({
    model: InstallProfile.Model,
    sortNames: ['standard', 'full'],
    url:
      './jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/InstallationProfiles/',
    parse: function(resp) {
      return resp.value
    },
    comparator: function(model1, model2) {
      let returnValue = 0
      _.every(this.sortNames, function(sortName) {
        if (model1.get('name') === sortName) {
          returnValue = -1
        } else if (model2.get('name') === sortName) {
          returnValue = 1
        }

        return returnValue === 0
      })
      return returnValue
    },
  })

  return InstallProfile
})
