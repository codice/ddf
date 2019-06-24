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

const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const _ = require('underscore')
import * as React from 'react'
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const lightboxInstance = require('../lightbox/lightbox.view.instance.js')
const QueryFeedbackView = require('../query-feedback/query-feedback.view.js')
const QueryConfirmationView = require('../confirmation/query/confirmation.query.view.js')
const LoadingView = require('../loading/loading.view.js')
const QueryAnnotationsView = require('../query-annotations/query-annotations.view.js')
const properties = require('../../js/properties.js')
import ResultsExport from '../../react-component/container/results-export'
import { getExportResults } from '../../react-component/utils/export/export'

const NOT_CLONEABLE_ATTRIBUTES = ['id', 'result', 'hasBeenSaved']

const createDuplicateQuery = attributes => {
  let clonedAttributes = JSON.parse(JSON.stringify(attributes))
  return _.omit(clonedAttributes, NOT_CLONEABLE_ATTRIBUTES)
}

module.exports = Marionette.ItemView.extend({
  template() {
    const disabledExport = store.getSelectedResults().length < 1
    const exportClass = disabledExport ? 'composed-menu' : ''
    return (
      <React.Fragment>
        <div
          className="query-interaction interaction-run"
          title="Run"
          data-help="Executes the search."
        >
          <div className="interaction-text">Run</div>
        </div>
        <div
          className="query-interaction interaction-stop"
          title="Stop"
          data-help="Stops a search in the progress of executing.  If results have already returned from some sources, you won't lose them."
        >
          <div className="interaction-text">Stop</div>
        </div>
        <div
          className="query-interaction interaction-delete"
          title="Delete"
          data-help="Deletes the search."
        >
          <div className="interaction-text">Delete</div>
        </div>
        <div
          className="query-interaction interaction-duplicate"
          title="Duplicate"
          data-help="Create a new search based off this one."
        >
          <div className="interaction-text">Duplicate</div>
        </div>
        <div className="is-divider composed-menu" />
        <div
          className="query-interaction interaction-refresh-result-count"
          title="Refresh Result Count"
          data-help="Executes the search, returning only the result count."
        >
          <div className="interaction-text">Refresh Result Count</div>
        </div>
        <div className="is-divider composed-menu" />
        <div className="interaction-versioning composed-menu">
          <div
            className="query-interaction interaction-deleted interaction-search-archived"
            title="Search Archived"
            data-help="Executes the search, but specifically looks for archived results."
          >
            <div className="interaction-text">Search Archived</div>
          </div>
          <div
            className="query-interaction interaction-historic interaction-search-historical"
            title="Search Historical"
            data-help="Executes the search, but specifically looks for historical data."
          >
            <div className="interaction-text">Search Historical</div>
          </div>
        </div>
        <div className="is-divider composed-menu" />
        <div
          className="query-interaction interaction-annotations"
          title="View Notes"
          data-help="View notes on the search."
        >
          <div className="interaction-text">View Notes</div>
        </div>
        <div
          className="query-interaction interaction-feedback"
          title="Submit Feedback"
          data-help="Brings up a form to submit comments about your current search and its results."
        >
          <div className="interaction-text">Submit Feedback</div>
        </div>
        <div className="is-divider composed-menu" />
        <div
          className={`query-interaction interaction-export ${exportClass} ${
            disabledExport ? 'is-disabled' : ''
          }`}
          title="Export Selected"
          data-help="Opens a form to export the selected search results."
        >
          <div className={`interaction-text ${exportClass}`}>
            Export Selected
          </div>
        </div>
        <div
          className={`query-interaction interaction-zipped-export ${exportClass} ${
            disabledExport ? 'is-disabled' : ''
          }`}
          title="Export Selected (Compressed)"
          data-help="Opens a form to export the selected search results in a compressed zip folder."
        >
          <div className={`interaction-text ${exportClass}`}>
            Export Selected (Compressed)
          </div>
        </div>
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('query-interactions'),
  className: 'composed-menu',
  modelEvents: {},
  events: {
    'click .interaction-run': 'handleRun',
    'click .interaction-refresh-result-count': 'handleRefreshResultCount',
    'click .interaction-stop': 'handleCancel',
    'click .interaction-delete': 'handleDelete',
    'click .interaction-duplicate': 'handleDuplicate',
    'click .interaction-deleted': 'handleDeleted',
    'click .interaction-historic': 'handleHistoric',
    'click .interaction-feedback': 'handleFeedback',
    'click .interaction-annotations': 'handleAnnotations',
    'click .interaction-export': 'handleExport',
    'click .interaction-zipped-export': 'handleZippedExport',
    click: 'handleClick',
  },
  ui: {},
  initialize() {
    if (!this.model.get('result')) {
      this.startListeningToSearch()
    }
    this.handleResult()
    this.$el.toggleClass(
      'is-archive-searchable',
      !properties.isArchiveSearchEnabled()
    )
    this.$el.toggleClass(
      'is-history-searchable',
      !properties.isHistoricalSearchEnabled()
    )
    this.$el.toggleClass(
      'is-versioning-enabled',
      !properties.isVersioningEnabled
    )
    this.listenTo(
      store.getSelectedResults(),
      'update add remove reset',
      this.render
    )
  },
  onRender() {
    this.handleLocal()
  },
  handleLocal() {
    this.$el.toggleClass('is-local', this.model.isLocal())
  },
  startListeningToSearch() {
    this.listenToOnce(this.model, 'change:result', this.startListeningForResult)
  },
  startListeningForResult() {
    this.listenToOnce(this.model.get('result'), 'sync error', this.handleResult)
  },
  handleRun() {
    this.model.startSearchFromFirstPage()
  },
  handleRefreshResultCount() {
    this.model.startSearch({ resultCountOnly: true })
  },
  handleCancel() {
    this.model.cancelCurrentSearches()
  },
  handleDelete() {
    this.model.collection.remove(this.model)
  },
  handleDeleted() {
    this.model.startSearch({
      limitToDeleted: true,
    })
  },
  handleDuplicate() {
    const copyAttributes = createDuplicateQuery(this.model.attributes)
    const newQuery = new this.model.constructor(copyAttributes)
    if (this.model.collection.canAddQuery()) {
      this.model.collection.add(newQuery)
      store.setCurrentQuery(newQuery)
    } else {
      this.listenTo(
        QueryConfirmationView.generateConfirmation({}),
        'change:choice',
        confirmation => {
          const choice = confirmation.get('choice')
          if (choice === true) {
            const loadingview = new LoadingView()
            store.get('workspaces').once('sync', (workspace, resp, options) => {
              loadingview.remove()
              wreqr.vent.trigger('router:navigate', {
                fragment: 'workspaces/' + workspace.id,
                options: {
                  trigger: true,
                },
              })
            })
            store.get('workspaces').createWorkspaceWithQuery(newQuery)
          } else if (choice !== false) {
            store.getCurrentQueries().remove(choice)
            store.getCurrentQueries().add(newQuery)
            store.setCurrentQuery(newQuery)
          }
        }
      )
    }
  },
  handleHistoric() {
    this.model.startSearch({
      limitToHistoric: true,
    })
  },
  handleFeedback() {
    lightboxInstance.model.updateTitle('Search Quality Feedback')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new QueryFeedbackView({
        model: this.model,
      })
    )
  },
  handleAnnotations() {
    lightboxInstance.model.updateTitle('Search Notes')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new QueryAnnotationsView({
        model: this.model,
      })
    )
  },
  handleExport() {
    const selectedResults = store.getSelectedResults()
    const exportResults = getExportResults(selectedResults.models)

    lightboxInstance.model.updateTitle('Export Results')
    lightboxInstance.model.open()
    lightboxInstance.showContent(<ResultsExport results={exportResults} />)
  },
  handleZippedExport() {
    const selectedResults = store.getSelectedResults()
    const exportResults = getExportResults(selectedResults.models)

    lightboxInstance.model.updateTitle('Export Results (Compressed)')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      <ResultsExport results={exportResults} isZipped={true} />
    )
  },
  handleResult() {
    this.$el.toggleClass('has-results', this.model.get('result') !== undefined)
  },
  handleClick() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
