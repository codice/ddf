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

define([
  'backbone.marionette',
  'underscore',
  'jquery',
  'moment',
  './system-information.hbs',
  'js/util/TimeUtil',
  'js/util/UnitsUtil',
  'js/CustomElements',
  'q',
  'js/models/module/SystemInformation',
  'js/models/module/OperatingSystem',
], function(
  Marionette,
  _,
  $,
  moment,
  template,
  TimeUtil,
  UnitsUtil,
  CustomElements,
  Q,
  SystemInformation,
  OperatingSystem
) {
  'use strict'

  var FeaturesView = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('system-information'),
    initialize: function() {
      this.systemInformation = new SystemInformation.Model()
      this.operatingSystem = new OperatingSystem.Model()
      this.systemInformation.fetch()
      this.operatingSystem.fetch()
      this.listenTo(this.systemInformation, 'change:fetched', this.render)
      this.listenTo(this.operatingSystem, 'change:fetched', this.render)
    },
    serializeData: function() {
      var returnValue = {
        fetched: false,
      }
      if (
        this.systemInformation.get('fetched') &&
        this.operatingSystem.get('fetched')
      ) {
        var systemData = this.systemInformation.toJSON()
        var operatingSystemData = this.operatingSystem.toJSON()
        var uptime = TimeUtil.convertUptimeToString(systemData.Uptime)
        var usedMemory = UnitsUtil.convertBytesToDisplay(
          operatingSystemData.TotalPhysicalMemorySize -
            operatingSystemData.FreePhysicalMemorySize
        )
        var totalMemory = UnitsUtil.convertBytesToDisplay(
          operatingSystemData.TotalPhysicalMemorySize
        )
        var freeMemory = UnitsUtil.convertBytesToDisplay(
          operatingSystemData.FreePhysicalMemorySize
        )
        var startTime = moment(systemData.StartTime).toDate()

        returnValue = {
          systemInformation: systemData,
          operatingSystem: operatingSystemData,
          startTime: startTime,
          uptime: uptime,
          usedMemory: usedMemory,
          totalMemory: totalMemory,
          freeMemory: freeMemory,
          runtime: systemData.SystemProperties['java.runtime.name'],
          runtimeVersion: systemData.SystemProperties['java.runtime.version'],
          fetched: true,
        }
      }

      return returnValue
    },
  })

  return FeaturesView
})
