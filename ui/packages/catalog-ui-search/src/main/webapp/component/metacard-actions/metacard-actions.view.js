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
/*global define, setTimeout*/
const Marionette = require('marionette')
const _ = require('lodash')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
import React from 'react'
import MapActions from '../../react-component/container/map-actions'

module.exports = Marionette.LayoutView.extend({
  setDefaultModel: function() {
    this.model = this.selectionInterface.getSelectedResults().first()
  },
  template(props) {
    return (
      <React.Fragment>
        <div className="is-divider" />
        <div className="is-header">Export as:</div>
        <div className="is-divider" />
        <div className="actions">
          {props.exportActions.map(exportAction => {
            return (
              <a href={exportAction.url} target="_blank" key={exportAction.url}>
                {exportAction.title}
              </a>
            )
          })}
        </div>
        <div className="is-divider" />
        <div className="map-actions">
          <MapActions model={this.model} />
        </div>

        {props.otherActions.length !== 0 ? (
          <React.Fragment>
            <div className="is-header">Various:</div>
            <div className="is-divider" />
            <div className="actions">
              {props.otherActions.map(otherAction => {
                return (
                  <a
                    href={otherAction.url}
                    target="_blank"
                    key={otherAction.url}
                  >
                    {otherAction.title}
                  </a>
                )
              })}
            </div>
            <div className="is-divider" />
          </React.Fragment>
        ) : (
          ''
        )}
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('metacard-actions'),
  events: {},
  ui: {},
  selectionInterface: store,
  initialize: function(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    if (!options.model) {
      this.setDefaultModel()
    }
  },
  serializeData: function() {
    return {
      exportActions: _.sortBy(
        this.model.getExportActions().map(action => ({
          url: action.get('url'),
          title: action.getExportType(),
        })),
        action => action.title.toLowerCase()
      ),
      otherActions: _.sortBy(
        this.model.getOtherActions().map(action => ({
          url: action.get('url'),
          title: action.get('title'),
        })),
        action => action.title.toLowerCase()
      ),
    }
  },
})
