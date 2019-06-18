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
import { TabMessage } from '../search-form-presentation'
const Marionette = require('marionette')
const SearchFormCollectionView = require('../search-form.collection.view')
const SearchFormCollection = require('../search-form-collection-instance')
const LoadingCompanionView = require('../../loading-companion/loading-companion.view.js')
const Router = require('../../router/router.js')

const Root = styled.div`
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
  initialize() {
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
            {this.searchFormCollection.attributes.searchForms.length !== 0 && (
              <TabMessage>
                These are system search forms and <b>cannot be changed</b>{' '}
              </TabMessage>
            )}
            <div className="collection" />
          </React.Fragment>
        ) : (
          <div className="collection" />
        )}
      </Root>
    )
  },
  onRender() {
    this.collection.show(
      new SearchFormCollectionView({
        collection: this.searchFormCollection.getCollection(),
        model: this.model,
        type: 'System',
        hideInteractionMenu: this.options.hideInteractionMenu,
        filter: child => child.get('createdBy') === 'system',
      })
    )
    LoadingCompanionView.beginLoading(this)
    this.handleLoadingSpinner()
  },
  handleLoadingSpinner() {
    if (this.searchFormCollection.getDoneLoading()) {
      LoadingCompanionView.endLoading(this)
    }
  },
})
