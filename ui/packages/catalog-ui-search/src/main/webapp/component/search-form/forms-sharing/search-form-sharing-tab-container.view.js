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
const template = require('../search-form.collection.hbs')
const SearchFormCollectionView = require('../search-form.collection.view')
const SearchFormCollection = require('../search-form-collection-instance')
const CustomElements = require('../../../js/CustomElements.js')
const LoadingCompanionView = require('../../loading-companion/loading-companion.view.js')
const user = require('../../singletons/user-instance')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('shared-search-form-collection'),
  regions: {
    collectionView: '.collection',
  },
  initialize() {
    this.searchFormCollection = SearchFormCollection
    this.listenTo(
      this.searchFormCollection,
      'change:doneLoading',
      this.handleLoadingSpinner
    )
  },
  onRender() {
    this.collectionView.show(
      new SearchFormCollectionView({
        collection: this.searchFormCollection.getCollection(),
        model: this.model,
        type: 'Shared',
        filter: child =>
          child.get('createdBy') !== user.getEmail() &&
          user.canRead(child) &&
          child.get('createdBy') !== 'system',
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
