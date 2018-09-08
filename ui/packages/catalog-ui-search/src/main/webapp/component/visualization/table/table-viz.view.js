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
/*global require*/

const { reactToMarionette } = require('component/transmute')
const ExportResults = reactToMarionette(
  require('react-component/export-results')
)

const template = require('./table-viz.hbs')
const Marionette = require('marionette')
const CustomElements = require('js/CustomElements')
const $ = require('jquery')
const TableVisibility = require('./table-visibility.view')
const TableRearrange = require('./table-rearrange.view')
const ResultsTableView = require('component/table/results/table-results.view')
const user = require('component/singletons/user-instance')
const properties = require('properties')
const announcement = require('component/announcement')

function saveFile(name, type, data) {
  if (data != null && navigator.msSaveBlob)
    return navigator.msSaveBlob(new Blob([data], { type: type }), name)
  var a = $("<a style='display: none;'/>")
  var url = window.URL.createObjectURL(new Blob([data], { type: type }))
  a.attr('href', url)
  a.attr('download', name)
  $('body').append(a)
  a[0].click()
  window.URL.revokeObjectURL(url)
  a.remove()
}

function getFilenameFromContentDisposition(header) {
  if (header == null) {
    return null
  }

  var parts = header.split('=', 2)
  if (parts.length !== 2) {
    return null
  }
  //return filename portion
  return parts[1]
}

const adaptFetchData = async response => ({
  data: await response.text(),
  status: response.status,
  getFirstResponseHeader: requestedHeader => {
    for (const header of response.headers.entries()) {
      if (!Array.isArray(header)) {
        continue
      }
      if (
        (header[0] || '').toLowerCase() ===
        (requestedHeader || '').toLowerCase()
      )
        return header[1]
    }
  },
})

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('table-viz'),
  template: template,
  events: {
    'click .options-rearrange': 'startRearrange',
    'click .options-visibility': 'startVisibility',
  },
  regions: {
    table: {
      selector: '.tables-container',
    },
    tableVisibility: {
      selector: '.table-visibility',
      replaceElement: true,
    },
    tableRearrange: {
      selector: '.table-rearrange',
      replaceElement: true,
    },
    tableExportAs: '.options-export-as',
  },
  initialize: function(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.listenTo(
      this.options.selectionInterface,
      'reset:activeSearchResults add:activeSearchResults',
      this.handleEmpty
    )
  },
  handleEmpty: function() {
    this.$el.toggleClass(
      'is-empty',
      this.options.selectionInterface.getActiveSearchResults().length === 0
    )
  },
  onRender: function() {
    this.handleEmpty()
    this.table.show(
      new ResultsTableView({
        selectionInterface: this.options.selectionInterface,
      })
    )
    this.tableExportAs.show(this.setupExportResults(), {
      replaceElement: true,
    })
  },
  startRearrange: function() {
    this.$el.toggleClass('is-rearranging')
    this.tableRearrange.show(
      new TableRearrange({
        selectionInterface: this.options.selectionInterface,
      }),
      {
        replaceElement: true,
      }
    )
  },
  startVisibility: function() {
    this.$el.toggleClass('is-visibilitying')
    this.tableVisibility.show(
      new TableVisibility({
        selectionInterface: this.options.selectionInterface,
      }),
      {
        replaceElement: true,
      }
    )
  },
  buildCqlQueryFromMetacards: function(metacards) {
    const queryParts = []
    for (const [index, metacard] of metacards.entries()) {
      queryParts.push(`(("id" ILIKE '${metacard.metacard.id}'))`)
    }
    return `(${queryParts.join(' OR ')})`
  },
  saveExport: (data, status, xhr) => {
    if (status === 200) {
      var filename = getFilenameFromContentDisposition(
        xhr.getFirstResponseHeader('Content-Disposition')
      )
      if (filename === null) {
        filename = 'export' + Date.now()
      }
      saveFile(
        filename,
        'data:' + xhr.getFirstResponseHeader('Content-Type'),
        data
      )
    } else {
      announcement.announce({
        title: 'Error!',
        message: 'Could not export results.',
        type: 'error',
      })
      console.error('Export failed with http status ' + status)
    }
  },
  setupExportResults() {
    const hiddenFieldsValue = user
      .get('user')
      .get('preferences')
      .get('columnHide')
    const hasHiddenFields = Object.keys(hiddenFieldsValue).length !== 0

    const columnOrderValue = user
      .get('user')
      .get('preferences')
      .get('columnOrder')
    const hasColumnOrder = Object.keys(columnOrderValue).length !== 0

    const visibleData = () => ({
      arguments: {
        hiddenFields: hasHiddenFields ? hiddenFieldsValue : {},
        columnOrder: hasColumnOrder ? columnOrderValue : {},
        columnAliasMap: properties.attributeAliases,
      },
      cql: this.buildCqlQueryFromMetacards(
        this.options.selectionInterface.getActiveSearchResults().toJSON()
      ),
    })

    const allData = () => ({
      arguments: {
        hiddenFields: hasHiddenFields ? hiddenFieldsValue : {},
        columnOrder: hasColumnOrder ? columnOrderValue : {},
        columnAliasMap: properties.attributeAliases,
      },
      cql: this.options.selectionInterface.getCurrentQuery().get('cql'),
    })

    const dataModel = {
      model: this.options.selectionInterface,
      props: {
        export: {
          visible: {
            url: `./internal/cql/transform/`,
            data: visibleData,
          },
          all: {
            url: `./internal/cql/transform/`,
            data: allData,
          },
        },
        defaultExportFormat: 'csv',
        contentType: 'application/json',
        onDownloadSuccess: async response => {
          const formattedResponse = await adaptFetchData(response)
          this.saveExport(
            formattedResponse.data,
            formattedResponse.status,
            formattedResponse
          )
        },
      },
    }

    return new ExportResults({ ...dataModel })
  },
})
