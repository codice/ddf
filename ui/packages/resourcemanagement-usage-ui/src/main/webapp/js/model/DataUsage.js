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
  const GB_SIZE = 1000 * 1000 * 1000

  const MB_SIZE = 1000 * 1000

  const KB_SIZE = 1000

  const CONFIGURATION_ADMIN_URL =
    '../jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/'

  const DataUsage = {}

  DataUsage.UsageModel = Backbone.Model.extend({
    initialize: function() {
      this.set({ users: [] })
      this.set({ saving: false })
      this.getUsageData()
      this.getCronTime()
    },
    getUsageData: function() {
      const url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/userMap/'
      const that = this
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
      const url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/cronTime/'
      const that = this
      $.ajax({
        url: url,
        dataType: 'json',
        success: function(data) {
          that.set({ cronTime: data.value })
        },
      })
    },
    updateCronTime: function(time) {
      const url =
        '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/updateCronTime/' +
        time
      const that = this
      $.ajax({
        url: url,
        dataType: 'json',
        success: function() {
          that.getCronTime()
        },
      })
    },
    parseDataModel: function(data) {
      const dataModel = []
      const that = this

      _.object(
        _.map(data, function(value, key) {
          const dataUsage = value[0]
          const dataLimit = value[1]

          let usagePercent = 0
          if (dataLimit >= 0) {
            usagePercent = Math.round((dataUsage / dataLimit) * 100)
            if (usagePercent > 100) {
              usagePercent = 100
            }
          }

          const usageRemaining = that.constructUsageRemainingString(
            dataLimit,
            dataUsage
          )
          const displayUsage = that.constructUsageRemainingString(dataUsage, 0)

          let displayLimit
          let displaySize

          if (dataLimit >= GB_SIZE) {
            displayLimit = (dataLimit / GB_SIZE).toFixed(1)
            displaySize = 'GB'
          } else {
            displayLimit = Math.round(dataLimit / MB_SIZE)
            displaySize = 'MB'
          }

          const object = {
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
      const that = this
      const url =
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
      const that = this
      let updatedModel = false
      this.set({ saving: true })
      ;(function poll() {
        const currentModel = that.get('users')

        if (!updatedModel) {
          setTimeout(function() {
            $.ajax({
              url:
                '../jolokia/exec/org.codice.ddf.resourcemanagement.usage.service.DataUsage:service=datausage/userMap/',
              success: function(data) {
                const receivedData = that.parseDataModel(data.value)
                const current = JSON.stringify(currentModel)
                const newData = JSON.stringify(receivedData)

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
      const model = this.get('users')
      let dataInBytes = value
      let isLimitChanged = false

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
      let usageRemaining = 'Unlimited'

      if (dataLimit >= 0) {
        const bytesRemaining = dataLimit - dataUsage

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
      const properties = { monitorLocalSources: updateMonitorLocalSources }
      const that = this
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
