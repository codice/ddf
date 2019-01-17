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
/*global require*/

const CustomElements = require('../../js/CustomElements.js')
const Marionette = require('marionette')
const React = require('react')
const Router = require('../router/router.js')
const user = require('../singletons/user-instance')

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('search-form-list'),
  className: 'composed-menu',
  template(props) {
    return <React.Fragment>
      {props.map(form => <div key={form.id} data-id={form.id}>{form.title}</div>)} 
      </React.Fragment>
  },
  events: {
    'click > div': 'openSearchForm',
  },
  serializeData: function() {
    return this.model.get('searchForms').toJSON()
  },
  openSearchForm: function(e) {
    const searchForms = this.model.get('searchForms')
    const selectedModel = searchForms.get(e.target.getAttribute('data-id'))
    this.changeView(selectedModel, this.model.get('currentQuery'))
  },
  changeView: function(selectedModel, queryModel) {
    let oldType = queryModel.get('type')
    switch (selectedModel.get('type')) {
      case 'basic':
        debugger
        queryModel.set('type', 'basic')
        if (oldType === 'new-form' || oldType === 'custom') {
          queryModel.set('title', 'Search Name')
        }
        user.getQuerySettings().set('type', 'basic')
        break
      case 'text':
        debugger
        queryModel.set('type', 'text')
        if (oldType === 'new-form' || oldType === 'custom') {
          queryModel.set('title', 'Search Name')
        }
        user.getQuerySettings().set('type', 'text')
        break
      case 'custom':
        debugger
        const sharedAttributes = selectedModel.transformToQueryStructure()
        queryModel.set({
          type: 'custom',
          ...sharedAttributes,
        })
        if (oldType === 'custom') {
          queryModel.trigger('change:type')
        }
        user.getQuerySettings().set('type', 'custom')
    }
    user.savePreferences()
    this.triggerCloseDropdown()
  },
  triggerCloseDropdown: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
