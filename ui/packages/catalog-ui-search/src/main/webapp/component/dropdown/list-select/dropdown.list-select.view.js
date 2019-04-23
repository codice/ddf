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
const _ = require('underscore')
const $ = require('jquery')
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.list-select.hbs')
const ListItemView = require('../../list-item/list-item.view.js')
const ListSelectView = require('../../list-select/list-select.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-listSelect',
  componentToShow: ListSelectView,
  regions: {
    listItem: '.list-item',
  },
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.listenTo(this.options.workspaceLists, 'remove', this.handleRemoveList)
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
  handleRemoveList: function(removedList) {
    if (removedList.id === this.model.get('value')) {
      this.model.set('value', undefined)
    }
  },
  onRender: function() {
    DropdownView.prototype.onRender.call(this)
    const listId = this.model.get('value')
    if (listId) {
      this.listItem.show(
        new ListItemView({
          model: this.options.workspaceLists.get(listId),
        })
      )
      this.$el.addClass('list-selected')
    } else {
      this.$el.removeClass('list-selected')
    }
  },
})
