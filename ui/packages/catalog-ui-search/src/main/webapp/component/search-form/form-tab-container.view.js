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
const LoadingCompanionView = require('../loading-companion/loading-companion.view.js')

module.exports = ({ collection, childView, childViewOptions }) =>
  Marionette.LayoutView.extend({
    template() {
      return (
        <React.Fragment>
          <div className="collection" />
        </React.Fragment>
      )
    },
    regions: {
      collectionView: '.collection',
    },
    initialize() {
      this.searchFormCollection = collection
      this.listenTo(
        this.searchFormCollection,
        'change:doneLoading',
        this.handleLoadingSpinner
      )
    },
    onRender() {
      this.collectionView.show(
        new childView({
          collection: this.searchFormCollection.getCollection(),
          collectionWrapperModel: this.searchFormCollection,
          model: this.model,
          ...childViewOptions,
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
