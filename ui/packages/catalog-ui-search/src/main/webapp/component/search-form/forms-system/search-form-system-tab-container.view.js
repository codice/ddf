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
/* global require */
import React from 'react'
import styled from '../../../react-component/styles/styled-components'
const Marionette = require('marionette')
const SearchFormCollectionView = require('../search-form.collection.view')
const SearchFormCollection = require('../search-form-collection-instance')
const LoadingCompanionView = require('../../loading-companion/loading-companion.view.js')
const Router = require('../../router/router.js')

const Root = styled.div`
  > .title {
    text-align: center;
    font-size: 20px;
    padding: 15px;
  }
  > .collection {
    margin: auto;
    max-width: 1020px;
    height: 100%;
    overflow: auto;
  }
`
module.exports = Marionette.LayoutView.extend({
  className: 'customElement',
  regions: {
    collection: '.collection',
  },
  initialize: function() {
    this.searchFormCollection = SearchFormCollection
    this.listenTo(
      this.searchFormCollection,
      'change:doneLoading',
      this.handleLoadingSpinner
    )
  },
  template() {
    return (
      <Root>
        {Router.attributes.path === 'forms(/)' ? (
          <React.Fragment>
            <div className="title">
              {' '}
              These are system search forms and <b>cannot be changed</b>{' '}
            </div>
            <div className="collection" />{' '}
          </React.Fragment>
        ) : (
          <div className="collection" />
        )}
      </Root>
    )
  },
  onRender: function() {
    this.collection.show(
      new SearchFormCollectionView({
        collection: this.searchFormCollection.getCollection(),
        model: this.model,
        hideInteractionMenu: this.options.hideInteractionMenu,
        filter: function(child) {
          return child.get('createdBy') === 'system'
        },
      })
    )
    LoadingCompanionView.beginLoading(this, this.$el)
    this.handleLoadingSpinner()
  },
  handleLoadingSpinner: function() {
    if (this.searchFormCollection.getDoneLoading()) {
      LoadingCompanionView.endLoading(this)
    }
  },
})
