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
  'jquery',
  'backbone',
  'underscore',
  'backbone.marionette',
  'handlebars',
  'icanhaz',
  'text!templates/dataUsagePage.handlebars',
  'text!templates/dataUsageTable.handlebars',
  'text!templates/dataUsageControl.handlebars',
], function(
  $,
  Backbone,
  _,
  Marionette,
  Handlebars,
  ich,
  userDataPage,
  userDataTable,
  userPageControl
) {
  var DataUsageView = {}

  ich.addTemplate('userDataPage', userDataPage)
  ich.addTemplate('userDataTable', userDataTable)
  ich.addTemplate('userPageControl', userPageControl)

  DataUsageView.UsagePage = Marionette.LayoutView.extend({
    template: 'userDataPage',
    regions: {
      usageTable: '.user-data-table',
      control: '.page-control',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
    },
    onRender: function() {
      this.usageTable.show(new DataUsageView.UsageTable({ model: this.model }))
      this.control.show(new DataUsageView.PageControl({ model: this.model }))
    },
  })

  DataUsageView.UsageTable = Marionette.CompositeView.extend({
    template: 'userDataTable',
    tagName: 'table',
    className: 'table table-striped table-bordered table-hover table-condensed',
    events: {
      'change .data-limit-td': 'contentChanged',
    },
    contentChanged: function(e) {
      var dataSize = $(e.target)
        .parent()
        .find('select')
        .find(':selected')
        .text()
      var inputValue = $(e.target)
        .parent()
        .find('input')
        .val()
      var user = $(e.target)
        .parent()
        .find('input')
        .attr('name')

      if (this.model.isLimitChanged(user, inputValue, dataSize)) {
        $(e.target).addClass('notify')
      } else {
        $(e.target).removeClass('notify')
      }
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(this.model, 'change:users', this.render)
    },
  })

  DataUsageView.PageControl = Marionette.LayoutView.extend({
    template: 'userPageControl',
    events: {
      'click .save': 'updateUsers',
      'click .refresh': 'refreshUsers',
      'change .data-limit-all': 'notifyAllData',
      'change .input-time': 'notifyTimeChange',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(this.model, 'change:saving', this.render)
      this.listenTo(this.model, 'change:cronTime', this.render)
      this.listenTo(this.model, 'change:monitorLocalSources', this.render)
    },
    onRender: function() {
      this.setupPopOver(
        '[data-toggle="update-all-popover"]',
        'Updates the data limit for all users in the table. This value overrides all individual user limits. ' +
          '[-1] indicates unlimited data usage. [0] indicates data usage is prohibited.'
      )
      this.setupPopOver(
        '[data-toggle="cron-time-popover"]',
        'Sets the time for the Data Usage for each user to reset. The system must be restarted for this new time to take effect.'
      )
      this.setupPopOver(
        '[data-toggle="monitor-local-sources"]',
        'When checked, the Data Usage Plugin will also consider data usage from local sources.'
      )
    },
    updateUsers: function() {
      var userData = this.model.get('users')
      var data = {}
      var updateAllUsers = $('.data-limit-all').val()
      var allDataSize = $('.data-size-all')
        .find(':selected')
        .text()
      var dataAllUsersByteLimit

      if (allDataSize === 'GB') {
        dataAllUsersByteLimit = this.getToBytes(
          parseFloat(updateAllUsers),
          allDataSize
        )
      } else {
        dataAllUsersByteLimit = this.getToBytes(
          parseInt(updateAllUsers),
          allDataSize
        )
      }

      var that = this

      $('.usertabledata tr').each(function(i, row) {
        var $row = $(row)
        var user = $row.find('td[name*="user"]').html()

        var dataSize = $row.find(':selected').text()
        var usageLimit

        if (dataSize === 'GB') {
          usageLimit = parseFloat(
            $row.find('input[name*="' + user + '"]').val()
          )
        } else {
          usageLimit = parseInt($row.find('input[name*="' + user + '"]').val())
        }

        var dataByteLimit = that.getToBytes(usageLimit, dataSize)

        if (
          updateAllUsers !== '' &&
          updateAllUsers >= -1 &&
          dataAllUsersByteLimit !== dataByteLimit &&
          dataAllUsersByteLimit !== userData[i].usageLimit
        ) {
          // global limit precedes user limits
          data[user] = dataAllUsersByteLimit
        } else if (usageLimit === -1 && usageLimit !== userData[i].usageLimit) {
          // unlimited data usage
          data[user] = -1
        } else if (
          dataByteLimit >= 0 &&
          dataByteLimit !== userData[i].usageLimit
        ) {
          data[user] = dataByteLimit
        }
      })

      if (!_.isEmpty(data)) {
        this.model.submitUsageData(data)
      }

      var updateTime = $('.input-time').val()
      if (updateTime !== this.model.get('cronTime')) {
        this.model.updateCronTime(updateTime)
      }
      var updateMonitorLocalSources = $('.monitor-checkbox').prop('checked')
      if (updateMonitorLocalSources !== this.model.get('monitorLocalSources')) {
        this.model.updateMonitorLocalSources(updateMonitorLocalSources)
      }
    },
    refreshUsers: function() {
      this.model.getUsageData()
      this.model.trigger('change:users', this.model)
      this.model.trigger('change:monitorLocalSources', this.model)
    },
    notifyAllData: function(e) {
      var value = $(e.target).val()
      var select = $(e.target)
        .parent()
        .find('select')

      if (value !== '') {
        $(e.target).addClass('notify')
        select.addClass('notify')
      } else {
        $(e.target).removeClass('notify')
        select.removeClass('notify')
      }
    },
    notifyTimeChange: function(e) {
      var timeInput = $(e.target).val()
      if (timeInput !== this.model.get('cronTime')) {
        $(e.target).addClass('notify')
      } else {
        $(e.target).removeClass('notify')
      }
    },
    getToBytes: function(dataLimit, dataSize) {
      var toBytes

      if (dataLimit === -1) {
        return dataLimit
      }

      if (dataSize === 'GB') {
        toBytes = 1000 * 1000 * 1000
      } else {
        toBytes = 1000 * 1000
      }

      return dataLimit * toBytes
    },
    setupPopOver: function(selector, content) {
      var options = {
        trigger: 'hover',
        content: content,
      }
      this.$el.find(selector).popover(options)
    },
  })

  return DataUsageView
})
