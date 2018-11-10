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
const $ = require('jquery')
const SearchFormCollectionView = require('./search-form-sharing.collection.view')
const SearchFormSharingCollection = require('./search-form-sharing.collection')
const CustomElements = require('../../../js/CustomElements.js')
const LoadingCompanionView = require('../../loading-companion/loading-companion.view.js')

const Root = styled.div`
  height: 100%;
  width: 100%;
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
  template() {
    return (
      <Root>
        <div className="collection" />
      </Root>
    )
  },
  initialize: function() {
    this.searchFormSharingCollection = new SearchFormSharingCollection()
    this.listenTo(
      this.searchFormSharingCollection,
      'change:doneLoading',
      this.handleLoadingSpinner
    )
  },
  onRender: function() {
    this.collection.show(
      new SearchFormCollectionView({
        collection: this.searchFormSharingCollection.getCollection(),
        model: this.model,
        hideInteractionMenu: this.options.hideInteractionMenu,
      })
    )
    LoadingCompanionView.beginLoading(this, this.$el)
    this.handleLoadingSpinner()
  },
  showCollection: function() {
    if (
      this.collection.currentView.searchFormSharingCollection.getDoneLoading()
    ) {
      this.$el.find('.loading').hide()
    }
  },
  handleLoadingSpinner: function() {
    if (this.searchFormSharingCollection.getDoneLoading()) {
      LoadingCompanionView.endLoading(this)
    }
  },
})
