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
import styled from '../../react-component/styles/styled-components'
import { ChangeBackground } from '../../react-component/styles/mixins/change-background'

const Marionette = require('marionette')
const MapView = require('../visualization/maps/openlayers/openlayers.view.js')
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
const announcement = require('../announcement/index.jsx')

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
    height: 40px;
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
    const [id] = Router.get('args')
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
    this.map.show(
      new MapView({
        selectionInterface: new SelectionInterface(),
        onMapLoaded: olMap => {
          this.showQueryForm(collection, id)
        },
      })
    )
  },
  showQueryForm(collection, id) {
    this.editor.show(
      new QueryAdvanced({
        model: this.model,
        isForm: true,
        isFormBuilder: true,
        isSearchFormEditor: true,
        onSave: () => {
          if (this.model.get('title').trim() !== '') {
            this.updateQuerySettings(collection, id)
            this.saveTemplateToBackend(collection, id)
            this.navigateToForms()
          } else {
            announcement.announce(
              {
                title: 'Some fields need your attention',
                message: 'Search form title cannot be blank.',
                type: 'error',
              },
              2500
            )
          }
        },
        onCancel: () => {
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
    }
    this.model.set(json)
    json.id ? this.model.save({}, options) : collection.create(json, options)
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
      message: 'Search form successfully saved',
      type: 'success',
    })
  },
  errorMessage() {
    announcement.announce({
      title: 'Error',
      message: 'Search form failed to save',
      type: 'error',
    })
  },
})
