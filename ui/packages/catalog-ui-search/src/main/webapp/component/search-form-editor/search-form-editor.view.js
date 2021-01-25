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
import styled from 'styled-components'
import { ChangeBackground } from '../../react-component/styles/mixins/change-background'

const Marionette = require('marionette')
const Common = require('../../js/Common.js')
const MapView = require('../visualization/maps/openlayers/openlayers.view.js')
const properties = require('../../js/properties.js')
const Router = require('../router/router.js')
const SearchFormCollection = require('../search-form/search-form-collection-instance')
const SearchFormModel = require('../search-form/search-form.js')
const SelectionInterface = require('../selection-interface/selection-interface.model.js')
const QueryAdvanced = require('../query-advanced/query-advanced.view.js')
const QueryModel = require('../../js/model/Query.js')
const QueryTitle = require('../query-title/query-title.view.js')
const wreqr = require('../../js/wreqr.js')
const user = require('../singletons/user-instance.js')
const cql = require('../../js/cql.js')
const CQLUtils = require('../../js/CQLUtils.js')
const terraformer = require('terraformer-wkt-parser')
const TurfCircle = require('@turf/circle')
const announcement = require('../announcement/index.jsx')
import { showErrorMessages } from '../../react-component/utils/validation'

const CIRCLE_PRECISION_STEPS = 64

const formTitle = properties.i18n['form.title']
  ? properties.i18n['form.title'].toLowerCase()
  : 'form'

const Root = styled.div`
  display: flex;
  height: 100%;
  width: 100%;
  background: inherit;
  ${props => {
    return ChangeBackground(props.theme.backgroundContent)
  }};
`

const Left = styled.div`
  width: 34%;
  height: 100%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  > .search-form-editor-title {
    flex-shrink: 0;
  }
  > .search-form-editor-query-input {
    height: 100%;
    overflow: auto;
  }
`
const Right = styled.div`
  width: 66%;
  background: inherit;
  > .search-form-editor-map {
    height: 100%;
    width: 100%;
    background: inherit;
  }
`

module.exports = Marionette.LayoutView.extend({
  className: 'customElement',
  template() {
    return (
      <Root>
        <Left>
          <div className="search-form-editor-title" />
          <div className="search-form-editor-query-input" />
        </Left>
        <Right>
          <div className="search-form-editor-map" />
        </Right>
      </Root>
    )
  },
  regions: {
    queryTitle: '.search-form-editor-title',
    editor: '.search-form-editor-query-input',
    map: '.search-form-editor-map',
  },
  initialize() {
    this.listenTo(Router, 'change', this.render)
  },
  onRender() {
    const [id] = Router.get('args')
    if (!id) {
      return
    }

    const collection = SearchFormCollection.getCollection()

    if (id === 'create') {
      this.model = new QueryModel.Model()
    } else {
      this.model = collection.get(id)
      if (this.model) {
        this.model.set(this.model.transformToQueryStructure())
      } else {
        return
      }
    }
    this.originalTitle = this.model.get('title')
    this.map.show(
      new MapView({
        showResultFilter: false,
        selectionInterface: new SelectionInterface(),
        onMapLoaded: olMap => {
          const filterTemplate = this.model.get('filterTemplate')
          this.showQueryForm(collection, id)
          if (!filterTemplate) {
            return
          }
          const geoFilters = (
            filterTemplate.filters || [filterTemplate]
          ).filter(filter => CQLUtils.isGeoFilter(filter.type))
          const coords = this.wktToCoords(geoFilters)
          Common.queueExecution(() => {
            this.map.currentView.map.zoomToExtent(coords)
          })
        },
      })
    )
  },
  wktToCoords(wktList) {
    const parsedGeoCoords = wktList.map(wkt => {
      if (wkt.value.startsWith('POINT') && parseFloat(wkt.distance) !== 0) {
        const center = terraformer.parse(wkt.value).coordinates
        return new TurfCircle(
          center,
          parseFloat(wkt.distance),
          CIRCLE_PRECISION_STEPS,
          'meters'
        ).geometry.coordinates[0]
      }
      return terraformer.parse(wkt.value).coordinates
    })
    return this.createCoordinatePairs(this.flatten(parsedGeoCoords.flat()))
  },
  flatten(arr) {
    return arr.reduce((flat, toFlatten) => {
      return flat.concat(
        Array.isArray(toFlatten) ? this.flatten(toFlatten) : toFlatten
      )
    }, [])
  },
  createCoordinatePairs(arr) {
    return arr.reduce((result, value, index, array) => {
      if (index % 2 === 0) {
        result.push(array.slice(index, index + 2))
      }
      return result
    }, [])
  },
  showQueryForm(collection, id) {
    wreqr.vent.trigger('resetSearch')
    this.editor.show(
      new QueryAdvanced({
        model: this.model,
        isFormBuilder: true,
        isSearchFormEditor: true,
        onSave: () => {
          if (this.model.get('title').trim() !== '') {
            const errorMessages = this.editor.currentView.getErrorMessages()
            if (errorMessages.length !== 0) {
              showErrorMessages(errorMessages)
            } else {
              this.updateQuerySettings(collection, id)
              this.saveTemplateToBackend(collection, id)
              this.navigateToForms()
            }
          } else {
            announcement.announce(
              {
                title: 'Some fields need your attention',
                message: `Search ${formTitle} title cannot be blank.`,
                type: 'error',
              },
              2500
            )
          }
        },
        onCancel: () => {
          this.resetTitle()
          this.navigateToForms()
        },
      })
    )
    this.queryTitle.show(
      new QueryTitle({
        model: this.model,
        isSearchFormEditor: true,
      })
    )
  },
  updateQuerySettings(collection, id) {
    const currentQuerySettings = user.getQuerySettings()
    const templateId = currentQuerySettings.get('template')
      ? currentQuerySettings.get('template').id
      : undefined
    if (templateId === id) {
      const template = this.getQueryAsQueryTemplate(collection, id)
      currentQuerySettings.set({ type: 'custom', template })
      user.savePreferences()
    }
  },
  getQueryAsQueryTemplate(collection, id) {
    const formModel = collection.get(id) || new SearchFormModel()
    const formParameters = this.editor.currentView.serializeTemplateParameters()
    let filterTree = cql.simplify(formParameters.filterTree || {})
    let filterSettings = formParameters.filterSettings || {}
    if (filterTree.filters && filterTree.filters.length === 1) {
      filterTree = filterTree.filters[0]
    }
    filterSettings.sorts = filterSettings.sorts
      .filter(sort => sort.attribute && sort.direction)
      .map(sort => sort.attribute + ',' + sort.direction)
    return {
      filterTemplate: filterTree,
      accessIndividuals: formModel.get('accessIndividuals'),
      accessGroups: formModel.get('accessGroups'),
      accessAdministrators: formModel.get('accessAdministrators'),
      creator: formModel.get('createdBy'),
      id: formModel.get('id'),
      title: this.model.get('title'),
      description: formModel.get('description'),
      created: formModel.get('createdOn'),
      default: formModel.get('default'),
      owner: formModel.get('owner'),
      querySettings: filterSettings,
    }
  },
  saveTemplateToBackend(collection, id) {
    const json = this.getQueryAsQueryTemplate(collection, id)
    const options = {
      success: () => {
        this.successMessage()
      },
      error: () => {
        this.errorMessage()
      },
      wait: true,
    }
    this.model.set(json)
    json.id ? this.model.save({}, options) : collection.create(json, options)
  },
  resetTitle() {
    this.model.set({ title: this.originalTitle })
  },
  navigateToForms() {
    const fragment = `forms`
    wreqr.vent.trigger('router:navigate', {
      fragment,
      options: {
        trigger: true,
      },
    })
  },
  successMessage() {
    announcement.announce({
      title: 'Success',
      message: `Search ${formTitle} successfully saved`,
      type: 'success',
    })
  },
  errorMessage() {
    announcement.announce({
      title: 'Error',
      message: `Search ${formTitle} failed to save`,
      type: 'error',
    })
  },
})
