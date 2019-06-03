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
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from '../../react-component/styles/styled-components'
import { readableColor } from 'polished'
import MetacardInteractionsDropdown from '../../react-component/container/metacard-interactions/metacard-interactions-dropdown'
const Backbone = require('backbone')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const IconHelper = require('../../js/IconHelper.js')
const store = require('../../js/store.js')
const Common = require('../../js/Common.js')
const DropdownModel = require('../dropdown/dropdown.js')
const ResultIndicatorView = require('../result-indicator/result-indicator.view.js')
const properties = require('../../js/properties.js')
const user = require('../singletons/user-instance.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')
const moment = require('moment')
const sources = require('../singletons/sources-instance.js')
const HoverPreviewDropdown = require('../dropdown/hover-preview/dropdown.hover-preview.view.js')
const ResultAddView = require('../result-add/result-add.view.js')
const PopoutView = require('../dropdown/popout/dropdown.popout.view.js')
require('../../behaviors/button.behavior.js')
require('../../behaviors/dropdown.behavior.js')
const HandleBarsHelpers = require('../../js/HandlebarsHelpers.js')
const ResultLinkView = require('../result-link/result-link.view.js')
const {
  SelectItemToggle,
} = require('../selection-checkbox/selection-checkbox.view.js')
const plugin = require('plugins/result-item')

const LIST_DISPLAY_TYPE = 'List'
const GRID_DISPLAY_TYPE = 'Grid'

const renderResultLink = (shouldRender, model) =>
  shouldRender && (
    <MarionetteRegionContainer
      className="result-link is-button is-neutral composed-button"
      data-help="Follow external links."
      view={props => PopoutView.createSimpleDropdown(props)}
      viewOptions={{
        componentToShow: ResultLinkView,
        modelForComponent: model,
        leftIcon: 'fa fa-external-link',
      }}
    />
  )

const getResultDisplayType = () =>
  (user &&
    user
      .get('user')
      .get('preferences')
      .get('resultDisplay')) ||
  LIST_DISPLAY_TYPE

const Divider = styled.div`
  height: ${props => props.theme.borderRadius};
  background: ${props => readableColor(props.theme.backgroundContent)};
  opacity: 0.1;
  margin-top: ${props => props.theme.minimumSpacing};
`

const Footer = styled.div`
  background: rgba(0, 0, 0, 0.05);
`

const ResultItemView = Marionette.LayoutView.extend({
  template(data) {
    const model = this.model
    const displayAsGrid = getResultDisplayType() === GRID_DISPLAY_TYPE
    const renderThumbnail = displayAsGrid && data.metacard.properties.thumbnail
    const renderResultLinkDropdown = model
      .get('metacard')
      .get('properties')
      .get('associations.external')

    return (
      <React.Fragment>
        <div
          className="result-container"
          data-metacard-id={data.id}
          data-query-id={data.metacard.queryId}
        >
          <MarionetteRegionContainer
            className="container-indicator"
            view={ResultIndicatorView}
            viewOptions={{ model }}
          />
          <div className="container-content">
            <div className="content-header">
              <span
                className="header-icon fa fa-history"
                title="Type: Revision"
                data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"
              />
              <span
                className="header-icon fa fa-book"
                title="Type: Workspace"
                data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"
              />
              <span
                className="header-icon fa fa-file"
                title="Type: Resource"
                data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"
              />
              <span
                className="header-icon fa fa-trash"
                title="Type: Deleted"
                data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"
              />

              <span
                className={`header-icon result-icon ${data.icon}`}
                title="Type: Resource"
                data-help="Indicates the type of result
                        (workspace, resource, history, deleted)"
              />
              <span
                className="header-title"
                data-help={HandleBarsHelpers.getAlias('title')}
                title={`${HandleBarsHelpers.getAlias('title')}: ${
                  data.metacard.properties.title
                }`}
              >
                {data.metacard.properties.title}
              </span>
            </div>
            <div className="content-body">
              {renderThumbnail && (
                <MarionetteRegionContainer
                  className="detail-thumbnail details-property"
                  data-help={HandleBarsHelpers.getAlias('thumbnail')}
                  view={HoverPreviewDropdown}
                  viewOptions={{
                    model: new DropdownModel(),
                    modelForComponent: model,
                  }}
                />
              )}
              {data.customDetail.map(detail => {
                return (
                  <div
                    key={detail.label}
                    className="detail-custom details-property"
                    data-help={HandleBarsHelpers.getAlias(detail.label)}
                    title={`${HandleBarsHelpers.getAlias(detail.label)}: ${
                      detail.value
                    }`}
                  >
                    <span>{detail.value}</span>
                  </div>
                )
              })}
              {data.showRelevanceScore ? (
                <div
                  className="detail-custom details-property"
                  data-help={`Relevance: ${data.relevance}`}
                  title={`Relevance: ${data.relevance}`}
                >
                  <span>{data.roundedRelevance}</span>
                </div>
              ) : (
                ''
              )}
              {data.showSource ? (
                <div
                  className="detail-source details-property"
                  title={`${HandleBarsHelpers.getAlias('source-id')}: ${
                    data.metacard.properties['source-id']
                  }`}
                  data-help={HandleBarsHelpers.getAlias('source-id')}
                >
                  {data.local ? (
                    <React.Fragment>
                      <span className="fa source-icon fa-home" />
                      <span>local</span>
                    </React.Fragment>
                  ) : (
                    <React.Fragment>
                      <span className="fa source-icon fa-cloud" />
                      <span>{data.metacard.properties['source-id']}</span>
                    </React.Fragment>
                  )}
                </div>
              ) : (
                ''
              )}
            </div>
            {this.getExtensions()}
            <Divider />
            <Footer className="content-footer">
              <div className="checkbox-container" />
              <div className="result-validation">
                {data.hasError ? (
                  <span
                    className="fa fa-exclamation-triangle resultError"
                    title="Has validation errors."
                    data-help="Indicates the given result has a validation error.
                                See the 'Quality' tab of the result for more details."
                  />
                ) : (
                  ''
                )}
                {!data.hasError && data.hasWarning ? (
                  <span
                    className="fa fa-exclamation-triangle resultWarning"
                    title="Has validation warnings."
                    data-help="Indicates the given result has a validation warning.
                                See the 'Quality' tab of the result for more details."
                  />
                ) : (
                  ''
                )}
              </div>
              <div className="result-extension">
                {this.getButtonExtensions()}
              </div>
              <button
                className="result-download fa fa-download is-button is-neutral"
                title="Downloads the associated resource directly to your machine."
                data-help="Downloads
                        the results associated product directly to your machine."
              />
              {renderResultLink(renderResultLinkDropdown, model)}
              <button
                className="result-add is-button is-neutral composed-button"
                title="Add or remove the result from a list, or make a new list with this result."
                data-help="Add or remove the result from a list, or make a new list with this result."
              >
                <span className="fa fa-plus" />
              </button>
              <MetacardInteractionsDropdown
                model={new Backbone.Collection([this.options.model])}
              />
            </Footer>
          </div>
        </div>
      </React.Fragment>
    )
  },
  onRender() {
    this.checkbox.show(
      new SelectItemToggle({
        model: this.model,
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  getExtensions() {
    return null
  },
  getButtonExtensions() {
    return null
  },
  attributes() {
    return {
      'data-resultid': this.model.id,
    }
  },
  tagName: CustomElements.register('result-item'),
  modelEvents: {},
  events: {
    'click .result-download': 'triggerDownload',
  },
  regions: {
    resultAdd: '.result-add',
    checkbox: '.checkbox-container',
  },
  behaviors() {
    return {
      button: {},
      dropdown: {
        dropdowns: [
          {
            selector: '.result-add',
            view: ResultAddView,
            viewOptions: {
              model: new Backbone.Collection([this.options.model]),
            },
          },
        ],
      },
    }
  },
  initialize(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.checkDisplayType()
    this.checkTags()
    this.checkIsInWorkspace()
    this.checkIfDownloadable()
    this.checkIfLinks()
    this.checkIfBlacklisted()
    this.listenTo(
      this.model,
      'change:metacard>properties change:metacard',
      this.handleMetacardUpdate
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:resultDisplay',
      this.checkDisplayType
    )
    this.listenTo(
      user
        .get('user')
        .get('preferences')
        .get('resultBlacklist'),
      'add remove update reset',
      this.checkIfBlacklisted
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.handleSelectionChange
    )
    this.handleSelectionChange()
  },
  handleSelectionChange() {
    const selectedResults = this.options.selectionInterface.getSelectedResults()
    const isSelected = selectedResults.get(this.model.id)
    this.$el.toggleClass('is-selected', Boolean(isSelected))
  },
  handleMetacardUpdate() {
    this.$el.attr(this.attributes())
    this.render()
    this.checkDisplayType()
    this.checkTags()
    this.checkIsInWorkspace()
    this.checkIfBlacklisted()
    this.checkIfDownloadable()
    this.checkIfLinks()
  },
  addConfiguredResultProperties(result) {
    result.showSource = false
    result.customDetail = []
    if (properties.resultShow) {
      properties.resultShow.forEach(additionProperty => {
        if (additionProperty === 'source-id') {
          result.showSource = true
          return
        }
        let value = result.metacard.properties[additionProperty]
        if (value && metacardDefinitions.metacardTypes[additionProperty]) {
          switch (metacardDefinitions.metacardTypes[additionProperty].type) {
            case 'DATE':
              if (value.constructor === Array) {
                value = value.map(val => Common.getMomentDate(val))
              } else {
                value = Common.getMomentDate(value)
              }
              break
          }
          result.customDetail.push({
            label: additionProperty,
            value,
          })
        }
      })
    }
    return result
  },
  massageResult(result) {
    //make a nice date
    result.local = Boolean(
      result.metacard.properties['source-id'] === sources.localCatalog
    )
    const dateModified = moment(result.metacard.properties.modified)
    result.niceDiff = Common.getMomentDate(dateModified)

    //icon
    result.icon = IconHelper.getClass(this.model)

    //check validation errors
    const validationErrors = result.metacard.properties['validation-errors']
    const validationWarnings = result.metacard.properties['validation-warnings']
    if (validationErrors) {
      result.hasError = true
      result.error = validationErrors
    }
    if (validationWarnings) {
      result.hasWarning = true
      result.warning = validationWarnings
    }

    //relevance score
    result.showRelevanceScore =
      properties.showRelevanceScores && result.relevance !== null
    if (result.showRelevanceScore === true) {
      result.roundedRelevance = parseFloat(result.relevance).toPrecision(
        properties.relevancePrecision
      )
    }

    return result
  },
  serializeData() {
    return this.addConfiguredResultProperties(
      this.massageResult(this.model.toJSON())
    )
  },
  checkIfBlacklisted() {
    const pref = user.get('user').get('preferences')
    const blacklist = pref.get('resultBlacklist')
    const id = this.model
      .get('metacard')
      .get('properties')
      .get('id')
    const isBlacklisted = blacklist.get(id) !== undefined
    this.$el.toggleClass('is-blacklisted', isBlacklisted)
  },
  checkIsInWorkspace() {
    const currentWorkspace = store.getCurrentWorkspace()
    this.$el.toggleClass('in-workspace', Boolean(currentWorkspace))
  },
  checkIfDownloadable() {
    this.$el.toggleClass(
      'is-downloadable',
      this.model
        .get('metacard')
        .get('properties')
        .get('resource-download-url') !== undefined
    )
  },
  checkIfLinks() {
    this.$el.toggleClass(
      'is-link',
      this.model
        .get('metacard')
        .get('properties')
        .get('associations.external') !== undefined
    )
  },
  checkDisplayType() {
    switch (getResultDisplayType()) {
      case LIST_DISPLAY_TYPE:
        this.$el.removeClass('is-grid').addClass('is-list')
        break
      case GRID_DISPLAY_TYPE:
        this.$el.addClass('is-grid').removeClass('is-list')
        this.render()
        break
    }
  },
  checkTags() {
    this.$el.toggleClass('is-workspace', this.model.isWorkspace())
    this.$el.toggleClass('is-resource', this.model.isResource())
    this.$el.toggleClass('is-revision', this.model.isRevision())
    this.$el.toggleClass('is-deleted', this.model.isDeleted())
    this.$el.toggleClass('is-remote', this.model.isRemote())
  },
  triggerDownload(e) {
    e.stopPropagation()
    window.open(
      this.model
        .get('metacard')
        .get('properties')
        .get('resource-download-url')
    )
  },
})

module.exports = plugin(ResultItemView)
