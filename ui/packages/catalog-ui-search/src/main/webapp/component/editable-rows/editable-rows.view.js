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
const CustomElements = require('../../js/CustomElements.js')
const EditableRowsTemplate = require('./editable-rows.hbs')
const EditableRowsView = require('../editable-row/editable-row.collection.view.js')
const JsonView = require('../json/json.view.js')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('editable-rows'),
  template: EditableRowsTemplate,
  events: { 'click .add-row': 'addRow' },
  regions: { rows: '.rows' },
  initialize() {
    this.listenTo(this.collection, 'add remove update reset', this.checkEmpty)
  },
  checkEmpty() {
    this.$el.toggleClass('is-empty', this.collection.isEmpty())
  },
  addRow() {
    this.collection.add({})
  },
  embed(model) {
    return new JsonView({ model })
  },
  onRender() {
    this.rows.show(
      new EditableRowsView({
        collection: this.collection,
        embed: this.embed,
        embedOptions: this.options,
      })
    )
    this.checkEmpty()
  },
})
