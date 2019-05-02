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
import SearchFormView, { Item, NewForm } from './search-form.view'
const CustomElements = require('../../js/CustomElements')
const wreqr = require('../../exports/wreqr.js')
import React from 'react'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('my-search-forms'),
  className: 'is-list is-inline has-list-highlighting',
  initialize(options) {
    this.model = this.options.collection
    this.filter = this.options.filter
    this.listenTo(this.model, 'add remove', this.render)
  },
  template() {
    return (
      <React.Fragment>
        {this.options.showNewForm ? (
          <NewForm
            label="New Search Form"
            onClick={this.handleNewForm.bind(this)}
          />
        ) : null}
        {this.model.filter(child => this.doFilter(child)).map(child => {
          return (
            <Item key={child.get('id')}>
              <MarionetteRegionContainer
                view={SearchFormView}
                viewOptions={{
                  model: child,
                  queryModel: this.options.queryModel,
                  collectionWrapperModel: this.options.collectionWrapperModel,
                }}
              />
            </Item>
          )
        })}
      </React.Fragment>
    )
  },
  handleNewForm() {
    this.options.queryModel.set({
      type: 'new-form',
      associatedFormModel: this.model,
    })
    this.routeToSearchFormEditor('create')
  },
  doFilter(child) {
    if (typeof this.options.filter !== 'function') {
      // Show all children if no filter is provided
      return true
    }
    return this.options.filter(child)
  },
  routeToSearchFormEditor(newSearchFormId) {
    const fragment = `forms/${newSearchFormId}`
    wreqr.vent.trigger('router:navigate', {
      fragment,
      options: {
        trigger: true,
      },
    })
  },
})
