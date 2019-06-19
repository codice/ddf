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
const Marionette = require('marionette')
const ResultFormView = require('./result-forms.view.js')
const CustomElements = require('../../js/CustomElements.js')
const lightboxResultInstance = require('../lightbox/result/lightbox.result.view.js')
const lightboxInstance = lightboxResultInstance.generateNewLightbox()
const QueryResult = require('./result-form.view.js')
const SearchFormModel = require('../search-form/search-form.js')
import React from 'react'
import { Item, NewForm } from '../search-form/search-form.view'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'

module.exports = Marionette.ItemView.extend({
  initialize(options) {
    this.listenTo(this.options.collection, 'add remove', this.render)
  },
  template() {
    return (
      <React.Fragment>
        <NewForm
          onClick={() => this.handleNewResultForm()}
          label="New Result Form"
        />
        {this.options.collection.map(child => {
          return (
            <Item key={child.cid}>
              <MarionetteRegionContainer
                view={ResultFormView}
                viewOptions={{
                  model: child,
                  queryModel: this.options.model,
                  collectionWrapperModel: this.options.collectionWrapperModel,
                }}
              />
            </Item>
          )
        })}
      </React.Fragment>
    )
  },
  handleNewResultForm() {
    lightboxInstance.model.updateTitle('Create a new result form')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new QueryResult({
        model: new SearchFormModel({ name: '' }),
      })
    )
  },
  className: 'is-list is-inline has-list-highlighting',
  tagName: CustomElements.register('result-forms'),
})
