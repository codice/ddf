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

/** Main view page for add. */
define(['backbone', 'jquery', 'backboneassociations'], function(Backbone, $) {
  const Configuration = {};

  /*
* MODEL
*/
  Configuration.SystemProperty = Backbone.Model.extend({
    validate: function(attrs) {
      const validation = [];
      let errorMessage = '';
      if (attrs.title.indexOf('Port') > -1) {
        var value = attrs.value
        if (value && !$.isNumeric(value)) {
          errorMessage = 'Port must contain only digits.'
          validation.push({
            message: errorMessage,
            id: attrs.key,
          })
        }
      }
      this.set('errorMessage', errorMessage)
      // Force setting the invalid value to the model so it can remain
      // in the input box on render.
      this.set('value', attrs.value)

      if (validation.length > 0) {
        return validation
      }
    },
  })

  /*
* COLLECTION
*/
  Configuration.SystemProperties = Backbone.Collection.extend({
    model: Configuration.SystemProperty,
    url:
      './jolokia/exec/org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean/readSystemProperties',
    saveUrl:
      './jolokia/exec/org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean/writeSystemProperties',
    parse: function(response) {
      // Return the value which will be the list of system property objects
      return response.value
    },

    save: function() {
      const mbean = 'org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean';
      const operation = 'writeSystemProperties';

      let data = {
        type: 'EXEC',
        mbean: mbean,
        operation: operation,
      };

      const propertiesMap = {};
      this.models.forEach(function(model) {
        propertiesMap[model.get('key')] = model.get('value')
      })

      data.arguments = [propertiesMap]
      data = JSON.stringify(data)

      return $.ajax({
        type: 'POST',
        contentType: 'application/json',
        data: data,
        url: this.saveUrl,
      })
    },
  })

  Configuration.SystemPropertiesWrapped = Backbone.AssociatedModel.extend({
    defaults: function() {
      return {
        systemProperties: [],
        fetched: false,
      }
    },
    initialize: function() {
      this.listenTo(
        this,
        'sync:systemProperties',
        function() {
          this.set('fetched', true)
        }.bind(this)
      )
    },
    relations: [
      {
        type: Backbone.Many,
        key: 'systemProperties',
        collectionType: Configuration.SystemProperties,
      },
    ],
    fetch: function() {
      this.get('systemProperties').fetch()
    },
  })

  return Configuration
})
