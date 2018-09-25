/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import React from 'react'
import styled from '../../react-component/styles/styled-components'
import { ChangeBackground } from '../../react-component/styles/mixins/change-background'
const Marionette = require('marionette')
const MapView = require('component/visualization/maps/openlayers/openlayers.view')
const Router = require('component/router/router')
const SearchFormsCollection = require('component/search-form/search-form-collection-instance')
const SearchFormModel = require('component/search-form/search-form.js')
const SelectionInterface = require('component/selection-interface/selection-interface.model')
const QueryAdd = require('component/query-add/query-add.view')
const QueryAdvanced = require('component/query-advanced/query-advanced.view')
const QueryModel = require('js/model/Query')
const QueryTitle = require('component/query-title/query-title.view')
const LoadingView = require('component/loading/loading.view')
const wreqr = require('wreqr')
const user = require('component/singletons/user-instance')
const cql = require('js/cql')
const announcement = require('component/announcement')
const $ = require('jquery')

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
    const queryModel =
      id === 'create'
        ? new QueryModel.Model()
        : SearchFormsCollection.getCollection().get(id)
    if (queryModel) {
      this.model = queryModel
      const collection = SearchFormsCollection.getCollection()
      this.map.show(
        new MapView({
          selectionInterface: new SelectionInterface(),
        })
      ),
        this.editor.show(
          new QueryAdvanced({
            model: this.model,
            isForm: true,
            isFormBuilder: true,
            onSave: () => {
              this.saveTemplateToBackend(collection, id)
              this.navigateToForms()
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
    }
  },
  getQueryAsQueryTemplate: function(collection, id) {
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
  saveTemplateToBackend: function(collection, id) {
    let loadingView = new LoadingView()
    let _this = this
    let _user = user
    $.ajax({
      url: './internal/forms/query',
      data: JSON.stringify(this.getQueryAsQueryTemplate(collection, id)),
      method: 'PUT',
      contentType: 'application/json',
      customErrorHandling: true,
    })
      .done((data, textStatus, jqxhr) => {
        _this.model.set({
          type: 'custom',
        })
        const preferences = _user.getQuerySettings()
        if (preferences.get('template')) {
          preferences.set('type', 'custom')
        } else {
          preferences.set('type', 'text')
        }
        _user.savePreferences()
        announcement.announce(
          {
            title: 'Success!',
            message: 'Search form successfully saved',
            type: 'success',
          },
          1500
        )
      })
      .fail((jqxhr, textStatus, errorThrown) => {
        announcement.announce(
          {
            title: 'Search Form Failed to be Saved',
            message: jqxhr.responseJSON.message,
            type: 'error',
          },
          2500
        )
      })
      .always(() => {
        loadingView.remove()
      })
  },
  navigateToForms: function() {
    const fragment = `forms`
    wreqr.vent.trigger('router:navigate', {
      fragment,
      options: {
        trigger: true,
      },
    })
  },
})
