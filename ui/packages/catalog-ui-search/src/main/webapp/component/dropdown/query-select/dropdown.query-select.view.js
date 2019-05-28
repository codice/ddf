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

const DropdownView = require('../dropdown.view')
const template = require('./dropdown.query-select.hbs')
const QueryItemView = require('../../query-item/query-item.view.js')
const SearchSelectView = require('../../search-select/search-select.view.js')
const store = require('../../../js/store.js')

module.exports = DropdownView.extend({
  template,
  className: 'is-querySelect',
  componentToShow: SearchSelectView,
  regions: {
    queryItem: '.querySelect-item',
  },
  initialize() {
    DropdownView.prototype.initialize.call(this)
    this.listenTo(store.getCurrentQueries(), 'remove', this.handleRemoveQuery)
    this.handleHideActions()
  },
  handleHideActions() {
    this.$el.toggleClass('hide-actions', this.options.hideActions === true)
  },
  initializeComponentModel() {
    //override if you need more functionality
    this.modelForComponent = this.options.model
  },
  listenToComponent() {
    //override if you need more functionality
  },
  isCentered: true,
  getCenteringElement() {
    return this.el
  },
  hasTail: true,
  handleRemoveQuery(removedQuery) {
    if (removedQuery.id === this.model.get('value')) {
      this.model.set('value', undefined)
    }
  },
  onRender() {
    DropdownView.prototype.onRender.call(this)
    const queryId = this.model.get('value')
    if (queryId) {
      this.queryItem.show(
        new QueryItemView({
          model: store.getCurrentQueries().get(queryId),
        })
      )
      this.$el.addClass('query-selected')
    } else {
      this.$el.removeClass('query-selected')
    }
  },
})
