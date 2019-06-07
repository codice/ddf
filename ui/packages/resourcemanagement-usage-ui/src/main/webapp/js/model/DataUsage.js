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

define(['backbone', 'jquery', 'underscore'], function(Backbone, $, _) {
  var GB_SIZE = 1000 * 1000 * 1000

  var MB_SIZE = 1000 * 1000

  var KB_SIZE = 1000

  var CONFIGURATION_ADMIN_URL =
    '../jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/'

  var DataUsage = {}

  DataUsage.UsageModel = Backbone.Model.extend({
    initialize: function() {
      this.set({ users: [] })
      this.set({ saving: false })
      this.getUsageData()
      this.getCronTime()
    },
    getUsageData: function() {
      var url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/userMap/'
      var that = this
      $.ajax({
        url: url,
        dataType: 'json',
        success: function(data) {
          that.set({ users: that.parseDataModel(data.value) })
        },
      })
      $.ajax({
        url:
          CONFIGURATION_ADMIN_URL +
          'getProperties/org.codice.ddf.resourcemanagement.usage',
        dataType: 'json',
        success: function(data) {
          that.set({ monitorLocalSources: data.value.monitorLocalSources })
        },
      })
    },
    getCronTime: function() {
      var url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/cronTime/'
      var that = this
      $.ajax({
        url: url,
        dataType: 'json',
        success: function(data) {
          that.set({ cronTime: data.value })
        },
      })
    },
    updateCronTime: function(time) {
      var url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/updateCronTime/' +
        time
      var that = this
      $.ajax({
        url: url,
        dataType: 'json',
        success: function() {
          that.getCronTime()
        },
      })
    },
    parseDataModel: function(data) {
      var dataModel = []
      var that = this

      _.object(
        _.map(data, function(value, key) {
          var dataUsage = value[0]
          var dataLimit = value[1]

          var usagePercent = 0
          if (dataLimit >= 0) {
            usagePercent = Math.round((dataUsage / dataLimit) * 100)
            if (usagePercent > 100) {
              usagePercent = 100
            }
          }

          var usageRemaining = that.constructUsageRemainingString(
            dataLimit,
            dataUsage
          )
          var displayUsage = that.constructUsageRemainingString(dataUsage, 0)

          var displayLimit
          var displaySize

          if (dataLimit >= GB_SIZE) {
            displayLimit = (dataLimit / GB_SIZE).toFixed(1)
            displaySize = 'GB'
          } else {
            displayLimit = Math.round(dataLimit / MB_SIZE)
            displaySize = 'MB'
          }

          var object = {
            user: key,
            usagePercent: usagePercent,
            usageRemaining: usageRemaining,
            usageLimit: dataLimit,
            displayLimit: displayLimit,
            displaySize: displaySize,
            notify: usagePercent >= 100,
            usage: displayUsage,
          }
          dataModel.push(object)
        })
      )
      return dataModel
    },
    submitUsageData: function(data) {
      var that = this
      var url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/updateUserDataLimit/' +
        encodeURIComponent(JSON.stringify(data))
      $.ajax({
        url: url,
        dataType: 'json',
        success: function() {
          that.pollUntilUpdated()
        },
      })
    },
    pollUntilUpdated: function() {
      var that = this
      var updatedModel = false
      this.set({ saving: true })
      ;(function poll() {
        var currentModel = that.get('users')

        if (!updatedModel) {
          setTimeout(function() {
            $.ajax({
              url:
                '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/userMap/',
              success: function(data) {
                var receivedData = that.parseDataModel(data.value)
                var current = JSON.stringify(currentModel)
                var newData = JSON.stringify(receivedData)

                if (newData !== current) {
                  that.set({ users: receivedData })
                  updatedModel = true
                  that.set({ saving: false })
                }
              },
              dataType: 'json',
              complete: poll,
            })
          }, 500)
        }
      })()
    },
    isLimitChanged: function(user, value, dataSize) {
      var model = this.get('users')
      var dataInBytes = value
      var isLimitChanged = false

      if (dataInBytes !== -1) {
        if (dataSize === 'GB') {
          dataInBytes = dataInBytes * GB_SIZE
        } else {
          dataInBytes = dataInBytes * MB_SIZE
        }
      }

      $.each(model, function(index, object) {
        if (object.user === user && dataInBytes !== object.usageLimit) {
          isLimitChanged = true
        }
      })
      return isLimitChanged
    },
    constructUsageRemainingString: function(dataLimit, dataUsage) {
      var usageRemaining = 'Unlimited'

      if (dataLimit >= 0) {
        var bytesRemaining = dataLimit - dataUsage

        if (bytesRemaining >= GB_SIZE) {
          usageRemaining = (bytesRemaining / GB_SIZE).toFixed(1) + ' GB'
        } else if (bytesRemaining >= MB_SIZE) {
          usageRemaining = Math.round(bytesRemaining / MB_SIZE) + ' MB'
        } else if (bytesRemaining >= 0) {
          usageRemaining = Math.round(bytesRemaining / KB_SIZE) + ' KB'
        } else {
          usageRemaining = '0 MB'
        }
      }

      return usageRemaining
    },
    updateMonitorLocalSources: function(updateMonitorLocalSources) {
      var properties = { monitorLocalSources: updateMonitorLocalSources }
      var that = this
      $.ajax({
        url:
          CONFIGURATION_ADMIN_URL +
          'update/org.codice.ddf.resourcemanagement.usage/' +
          JSON.stringify(properties),
        dataType: 'json',
        success: function() {
          that.set(properties)
        },
      })
    },
  })

  return DataUsage
})
