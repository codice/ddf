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

import React from 'react'
const Marionette = require('marionette')
const _ = require('underscore')
const CustomElements = require('../../js/CustomElements.js')
const QueryStatusView = require('../query-status/query-status.view.js')
const moment = require('moment')

function getResultsFound(total, data) {
  const hits = data.reduce(
    (hits, status) => (status.hits ? hits + status.hits : hits),
    0
  )
  const searching = _.every(data, status => _.isUndefined(status.successful))
  if (searching && data.length > 0) {
    return 'Searching...'
  } else if (total >= hits) {
    return total + ' results'
  } else {
    return hits + ' results'
  }
}

function getSomeStatusSuccess(status, value) {
  return _.some(status, s => s.successful === value)
}

function getPending(status) {
  return getSomeStatusSuccess(status, undefined)
}

function getFailed(status) {
  return getSomeStatusSuccess(status, false)
}

function getLastRan(initiated) {
  if (!_.isUndefined(initiated)) {
    return moment(initiated).fromNow()
  } else {
    return ''
  }
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('query-feed'),
  behaviors() {
    return {
      dropdown: {
        dropdowns: [
          {
            selector: '.details-view',
            view: QueryStatusView,
            viewOptions: {
              model: this.options.model,
            },
          },
        ],
      },
    }
  },
  initialize(options) {
    this.updateQuery = _.throttle(this.updateQuery, 200)
    this.listenTo(this.model, 'change', this.updateQuery)
    if (this.model.has('result')) {
      this.listenToStatus(this.model)
    } else {
      this.listenTo(this.model, 'change:result', this.resultAdded)
    }
  },
  template(data) {
    return (
      <React.Fragment>
        <div className="details-text">
          <div className="details-results" title={data.resultCount}>
            {data.pending ? (
              <i className="fa fa-circle-o-notch fa-spin is-critical-animation" />
            ) : (
              ''
            )}
            {data.failed ? <i className="fa fa-warning" /> : ''}
            {data.resultCount}
          </div>
          <div className="details-status" title="Last run {{queryStatus}}">
            {data.queryStatus}
          </div>
        </div>
        <button
          className="details-view is-button"
          title="Show the full status for the search."
          data-help="Show the full status for the search."
        >
          <span className="fa fa-heartbeat" />
        </button>
      </React.Fragment>
    )
  },
  updateQuery() {
    if (!this.isDestroyed) {
      this.render()
    }
  },
  resultAdded(model) {
    if (model.has('result') && _.isUndefined(model.previous('result'))) {
      this.listenToStatus(model)
    }
  },
  listenToStatus(model) {
    this.$el.toggleClass('has-been-run')
    this.listenTo(model.get('result>results'), 'reset', this.updateQuery)
    this.listenTo(model.get('result>results'), 'add', this.updateQuery)
    this.listenTo(model.get('result'), 'sync', this.updateQuery)
    this.listenTo(model.get('result>status'), 'change', this.updateQuery)
  },
  serializeData() {
    const query = this.model.toJSON({
      additionalProperties: ['cid', 'color'],
    })
    if (this.model.get('result')) {
      const status = _.filter(
        this.model
          .get('result')
          .get('status')
          .toJSON(),
        status => status.id !== 'cache'
      )
      return {
        resultCount: getResultsFound(
          this.model.get('result').get('results').length,
          status
        ),
        pending: getPending(status),
        failed: getFailed(status),
        queryStatus: getLastRan(this.model.get('result>initiated')),
      }
    } else {
      return {
        resultCount: 'Has not been run',
        queryStatus: '',
      }
    }
  },
})
