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

const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const QueryItemCollectionView = require('../query-item/query-item.collection.view.js')

const eventsHash = {
  click: 'handleClick',
}

const namespace = CustomElements.getNamespace()
const queryItemClickEvent = 'click ' + namespace + 'query-item'
eventsHash[queryItemClickEvent] = 'handleQueryItemClick'

module.exports = QueryItemCollectionView.extend({
  className: 'is-query-select composed-menu',
  events: eventsHash,
  onBeforeShow: function() {
    this.handleValue()
  },
  handleQueryItemClick: function(event) {
    this.model.set('value', $(event.currentTarget).attr('data-queryid'))
    this.handleValue()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  handleValue: function() {
    const queryId = this.model.get('value')
    this.$el.find(namespace + 'query-item').removeClass('is-selected')
    if (queryId) {
      this.$el
        .find(namespace + 'query-item[data-queryid="' + queryId + '"]')
        .addClass('is-selected')
    }
  },
})
