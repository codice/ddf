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
  'text!templates/queryMonitorPage.handlebars',
  'text!templates/queryMonitorTable.handlebars',
], function(
  $,
  Backbone,
  _,
  Marionette,
  Handlebars,
  ich,
  queryMonitorPage,
  queryMonitorTable
) {
  var QueryMonitorView = {}

  ich.addTemplate('queryMonitorPage', queryMonitorPage)
  ich.addTemplate('queryMonitorTable', queryMonitorTable)

  QueryMonitorView.QueryMonitorPage = Marionette.LayoutView.extend({
    template: 'queryMonitorPage',
    regions: {
      usageTable: '.query-data-table',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
    },
    onRender: function() {
      this.usageTable.show(
        new QueryMonitorView.QueryMonitorTable({ model: this.model })
      )
    },
  })

  QueryMonitorView.QueryMonitorTable = Marionette.CompositeView.extend({
    template: 'queryMonitorTable',
    tagName: 'table',
    className: 'table table-striped table-bordered table-hover table-condensed',
    events: {
      'click .glyphicon': 'stopSearch',
    },
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(this.model, 'change:users', this.render)
    },
    onRender: function() {
      this.setupPopOver(
        '[data-toggle="stop-popover"]',
        'Cancels the current search in progress.'
      )
    },
    stopSearch: function(data) {
      var user = $(data.target)
      var uuid = user[0].name
      this.model.stopSearch(uuid)
    },
    setupPopOver: function(selector, content) {
      var options = {
        trigger: 'hover',
        content: content,
        placement: 'left',
      }
      this.$el.find(selector).popover(options)
    },
  })

  return QueryMonitorView
})
