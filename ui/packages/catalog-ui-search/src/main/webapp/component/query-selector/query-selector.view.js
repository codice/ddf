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

const Marionette = require('marionette')
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const Query = require('../../js/model/Query.js')
const QueryItemCollectionView = require('../query-item/query-item.collection.view.js')
import React from 'react'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'
import styled from 'styled-components'

const SearchButton = styled(Button)`
  line-height: ${props => props.theme.minimumButtonSize};
  padding: 0px ${props => props.theme.largeSpacing};
`

const SearchIcon = styled.div`
  display: block;
  opacity: ${props => props.theme.minimumOpacity};
`

const Span = styled.span`
  opacity: ${props => props.theme.minimumOpacity};
`
const namespace = CustomElements.getNamespace()

const QuerySelector = Marionette.LayoutView.extend({
  setDefaultModel() {
    this.model = store.getCurrentQueries()
  },
  template() {
    return (
      <React.Fragment>
        <div className="querySelector-list" />
        <div align="center" className="if-empty is-header">
          <Span>New searches will appear here</Span>
          <div>
            <SearchIcon className="fa fa-search fa-5x" />
            <SearchButton
              className="quick-add"
              buttonType={buttonTypeEnum.primary}
            >
              Create a Search
            </SearchButton>
          </div>
        </div>
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('query-selector'),
  modelEvents: {},
  events() {
    let eventObj = {
      'click .querySelector-add': 'addQuery',
      'click > .if-empty .quick-add': 'triggerQuery',
    }
    eventObj[
      'click .querySelector-list ' +
        CustomElements.getNamespace() +
        'query-item'
    ] = 'selectQuery'
    return eventObj
  },
  ui: {},
  regions: {
    queryCollection: '.querySelector-list',
  },
  onBeforeShow() {
    this.queryCollection.show(new QueryItemCollectionView())
    this.queryCollection.currentView.$el
      .addClass('is-list')
      .addClass('has-list-highlighting')
  },
  initialize(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.handleUpdate()
    this.listenTo(this.model, 'add', this.handleUpdate)
    this.listenTo(this.model, 'remove', this.handleUpdate)
    this.listenTo(this.model, 'update', this.handleUpdate)
    this.listenTo(store.get('content'), 'change:query', this.handleQuerySelect)
  },
  addQuery() {
    if (this.model.canAddQuery()) {
      const newQuery = new Query.Model()
      store.setQueryByReference(newQuery)
    }
  },
  selectQuery(event) {
    const queryId = event.currentTarget.getAttribute('data-queryId')
    store.setQueryById(queryId)
  },
  handleQuerySelect() {
    const query = store.getQuery()
    this.$el.find(namespace + 'query-item').removeClass('is-selected')
    if (query) {
      this.$el
        .find(namespace + 'query-item[data-queryid="' + query.id + '"]')
        .addClass('is-selected')
    }
  },
  handleUpdate() {
    this.handleMaxQueries()
    this.handleEmptyQueries()
  },
  handleMaxQueries() {
    this.$el.toggleClass('can-addQuery', this.model.canAddQuery())
  },
  handleEmptyQueries() {
    this.$el.toggleClass('is-empty', this.model.isEmpty())
  },
  triggerQuery() {
    $('.content-adhoc')
      .mousedown()
      .click()
  },
})

module.exports = QuerySelector
