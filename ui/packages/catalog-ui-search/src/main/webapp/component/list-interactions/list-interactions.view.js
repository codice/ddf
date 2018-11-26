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
/*global define*/
var Marionette = require('marionette')
var template = require('./list-interactions.hbs')
var CustomElements = require('../../js/CustomElements.js')

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('list-interactions'),
  className: 'composed-menu',
  events: {
    'click .interaction-run': 'triggerRun',
    'click .interaction-stop': 'triggerCancel',
    'click .interaction-delete': 'triggerDelete',
    'click .interaction-duplicate': 'triggerDuplicate',
    'click .interaction-action': 'triggerAction',
    click: 'triggerClick',
  },
  modelEvents: {
    'change:actions': 'render',
  },
  onRender() {
    this.handleResult()
  },
  initialize() {
    if (!this.model.get('query').get('result')) {
      this.startListeningToSearch()
    }
    this.handleResult()
  },
  startListeningToSearch() {
    this.listenToOnce(
      this.model.get('query'),
      'change:result',
      this.startListeningForResult
    )
  },
  startListeningForResult() {
    this.listenToOnce(
      this.model.get('query').get('result'),
      'sync error',
      this.handleResult
    )
  },
  triggerRun() {
    const ids = this.model.get('list.bookmarks')
    this.model.get('query').startTieredSearch(ids)
  },
  triggerCancel() {
    this.model.get('query').cancelCurrentSearches()
  },
  triggerDelete() {
    this.model.collection.remove(this.model)
  },
  triggerDuplicate() {
    const copyAttributes = JSON.parse(JSON.stringify(this.model.attributes))
    delete copyAttributes.id
    delete copyAttributes.query
    const newList = new this.model.constructor(copyAttributes)
    this.model.collection.add(newList)
  },
  triggerAction(event) {
    const url = event.currentTarget.getAttribute('data-url')
    window.open(url)
  },
  handleResult() {
    this.$el.toggleClass(
      'has-results',
      this.model.get('query').get('result') !== undefined
    )
  },
  triggerClick() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
