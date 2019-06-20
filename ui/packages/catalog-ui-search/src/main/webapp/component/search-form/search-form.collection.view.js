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

import SearchFormView, { Item, NewForm } from './search-form.view'
import React from 'react'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import { TabMessage } from './search-form-presentation'
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements')
const wreqr = require('../../exports/wreqr.js')

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('my-search-forms'),
  className: 'is-list is-inline has-list-highlighting',
  initialize() {
    this.model = this.options.collection
    this.filter = this.options.filter
    this.listenTo(this.model, 'add remove', this.render)
    this.handleNewForm = this.handleNewForm.bind(this)
  },
  template() {
    const forms = this.model.filter(child => this.doFilter(child))
    return (
      <React.Fragment>
        {this.getMessage(forms)}
        {this.getButtons()}
        {forms.map(child => {
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
  getMessage(forms) {
    if (this.options.showNewForm) {
      return null
    }

    if (forms.length === 0) {
      return <TabMessage>No {this.options.type} Search Forms Found</TabMessage>
    } else {
      return <TabMessage>{this.options.message}</TabMessage>
    }
  },
  getButtons() {
    return this.options.showNewForm ? (
      <NewForm label="New Search Form" onClick={this.handleNewForm} />
    ) : null
  },
  handleNewForm() {
    this.options.model.set({
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
