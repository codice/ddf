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

define(function(require) {
  const Backbone = require('backbone'),
    $ = require('jquery'),
    _ = require('underscore')

  require('backboneassociation')

  const Service = {}

  Service.Metatype = Backbone.AssociatedModel.extend({})

  Service.Properties = Backbone.AssociatedModel.extend({})

  Service.MetatypeList = Backbone.Collection.extend({
    model: Service.Metatype,
  })

  Service.Configuration = Backbone.AssociatedModel.extend({
    configUrl:
      '../jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',

    defaults: function() {
      return {
        properties: new Service.Properties(),
      }
    },
    relations: [
      {
        type: Backbone.One,
        key: 'properties',
        relatedModel: Service.Properties,
        includeInJSON: true,
      },
    ],

    initialize: function(options) {
      if (options.service) {
        this.initializeFromService(options.service)
      } else {
        this.set('service', this.getService())
      }
    },
    /**
     * Collect all the data to save.
     * @param pid The pid id.
     * @returns {{type: string, mbean: string, operation: string}}
     */
    collectedData: function(pid) {
      const model = this
      const data = {
        type: 'EXEC',
        mbean:
          'org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
        operation: 'update',
      }
      data.arguments = [pid]
      data.arguments.push(model.get('properties').toJSON())
      return data
    },

    /**
     * Get the serviceFactoryPid PID
     * @param model, this is really this model.
     * @returns an ajax promise
     */
    makeConfigCall: function(model) {
      if (!model) {
        return
      }
      const configUrl = [
        model.configUrl,
        'createFactoryConfiguration',
        model.get('fpid'),
      ].join('/')
      return $.ajax({
        type: 'GET',
        url: configUrl,
      })
    },

    makeEnableCall: function() {
      const model = this
      const pid = model.get('id')
      const url = [model.configUrl, 'enableConfiguration', pid].join('/')
      if (pid) {
        return $.ajax({
          url: url,
          dataType: 'json',
        }).done(function() {
          // massage some data to match the new backend pid.
          model.trigger('enabled')
          //enabling the model means the PID will be regenerated. This model no longer exists on the server.
          model.destroy()
        })
      }

      return $.Deferred().fail(
        new Error('Cannot enable since this model has no pid.')
      )
    },

    makeDisableCall: function() {
      const model = this
      const pid = model.get('id')
      const url = [model.configUrl, 'disableConfiguration', pid].join('/')
      if (pid) {
        return $.ajax({
          url: url,
          dataType: 'json',
        }).done(function() {
          model.trigger('disabled')
          //disabling the model means the PID will be regenerated. This model no longer exists on the server.
          model.destroy()
        })
      }

      return $.Deferred().reject(
        new Error('Cannot enable since this model has no pid.')
      )
    },

    // Used to make a call to the backend to disable the service that corresponds to the pid.
    // Note: this does NOT affect the service the function is called on.
    // Treat this as a static function
    makeDisableCallByPid: function(pid) {
      const url = [this.configUrl, 'disableConfiguration', pid].join('/')
      return $.ajax({
        url: url,
        dataType: 'json',
      })
    },
    /**
     * When a model calls save the sync is called in Backbone.  I override it because this isn't a typical backbone
     * object
     * @return Return a deferred which is a handler with the success and failure callback.
     */
    sync: function() {
      const deferred = $.Deferred(),
        model = this
      //if it has a pid we are editing an existing record
      if (model.id) {
        const collect = model.collectedData(model.id)
        const jData = JSON.stringify(collect)

        return $.ajax({
          type: 'POST',
          contentType: 'application/json',
          data: jData,
          url: model.configUrl,
        })
          .done(function(result) {
            deferred.resolve(result)
          })
          .fail(function(error) {
            deferred.fail(error)
          })
        //no pid means this is a new record
      } else {
        model
          .makeConfigCall(model)
          .done(function(data) {
            const collect = model.collectedData(JSON.parse(data).value)
            const jData = JSON.stringify(collect)

            return $.ajax({
              type: 'POST',
              contentType: 'application/json',
              data: jData,
              url: model.configUrl,
            })
              .done(function(result) {
                deferred.resolve(result)
              })
              .fail(function(error) {
                deferred.fail(error)
              })
          })
          .fail(function(error) {
            deferred.fail(error)
          })
      }
      return deferred
    },
    createNewFromServer: function(deferred) {
      const model = this,
        addUrl = [model.configUrl, 'add'].join('/')

      model
        .makeConfigCall(model)
        .done(function(data) {
          const collect = model.collectedData(JSON.parse(data).value)
          const jData = JSON.stringify(collect)

          return $.ajax({
            type: 'POST',
            contentType: 'application/json',
            data: jData,
            url: addUrl,
          })
            .done(function(result) {
              deferred.resolve(result)
            })
            .fail(function(error) {
              deferred.fail(error)
            })
        })
        .fail(function(error) {
          deferred.fail(error)
        })
    },
    destroy: function() {
      const deferred = $.Deferred(),
        model = this,
        deleteUrl = [model.configUrl, 'delete', model.id].join('/')

      if (!model.id) {
        throw "No ID defined for model '" + model.get('name') + "'."
      }
      return $.ajax({
        type: 'GET',
        url: deleteUrl,
      })
        .done(function(result) {
          deferred.resolve(result)
        })
        .fail(function(error) {
          deferred.fail(error)
        })
    },
    initializeFromMSF: function(msf) {
      this.set({
        fpid: msf.get('id'),
      })
      this.set({
        name: msf.get('name'),
      })
      this.get('properties').set({
        'service.factoryPid': msf.get('id'),
      })
      this.initializeFromService(msf)
    },
    initializeFromService: function(service) {
      const fpid = service.get('id')
      const name = service.get('name')
      this.initializeFromMetatype(service.get('metatype'))
      this.set('service', service)
      this.set('fpid', fpid)
      this.set('name', name)
      this.get('properties').set('service.factoryPid', fpid)
    },
    initializeFromMetatype: function(metatype) {
      const model = this

      const idModel = _.find(metatype.models, function(item) {
        return item.get('id') === 'id' || item.get('id') === 'shortname'
      })
      if (!_.isUndefined(idModel)) {
        model.set(
          'properties',
          new Service.Properties(idModel.get('defaultValue'))
        )
      }
      metatype.forEach(function(obj) {
        const id = obj.get('id')
        const val = obj.get('defaultValue')
        if (id !== 'id') {
          model.get('properties').set(id, val ? val.toString() : null)
        }
      })
    },
    /**
     * Returns the Service.Model used to create this configuration. If the 'service' property is set, that value
     * is returned. Otherwise, the service is retrieved by looking it up from the configuration collection relation.
     */
    getService: function() {
      return this.get('service') || this.collection.parents[0]
    },
  })

  Service.ConfigurationList = Backbone.Collection.extend({
    model: Service.Configuration,
    comparator: function(model) {
      return model.get('id')
    },
  })

  Service.Model = Backbone.AssociatedModel.extend({
    configUrl:
      '../jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',

    defaults: function() {
      return {
        configurations: new Service.ConfigurationList(),
      }
    },
    relations: [
      {
        type: Backbone.Many,
        key: 'configurations',
        relatedModel: Service.Configuration,
        collectionType: Service.ConfigurationList,
        includeInJSON: true,
      },
      {
        type: Backbone.Many,
        key: 'metatype',
        relatedModel: Service.Metatype,
        includeInJSON: false,
      },
    ],

    hasConfiguration: function() {
      if (this.configuration) {
        return true
      }
      return false
    },
    initializeFromMSF: function(msf) {
      this.set({
        fpid: msf.get('id'),
      })
      this.set({
        name: msf.get('name'),
      })
      this.initializeConfigurationFromMetatype(msf.get('metatype'))
      this.configuration.set({
        'service.factoryPid': msf.get('id'),
      })
    },
    initializeConfigurationFromMetatype: function(metatype) {
      const src = this
      src.configuration = new Service.Configuration()
      metatype.forEach(function(obj) {
        const id = obj.id
        const val = obj.defaultValue
        src.configuration.set(id, val ? val.toString() : null)
      })
    },
  })

  Service.Response = Backbone.AssociatedModel.extend({
    url:
      '../jolokia/exec/org.codice.ddf.catalog.admin.poller.AdminPollerServiceBean:service=admin-source-poller-service/allSourceInfo',
    relations: [
      {
        type: Backbone.Many,
        key: 'value',
        relatedModel: Service.Model,
        includeInJSON: false,
      },
    ],
  })

  return Service
})
