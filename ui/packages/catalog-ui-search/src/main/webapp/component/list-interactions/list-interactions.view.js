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
const Marionette = require('marionette')
const template = require('./list-interactions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const announcement = require('../../component/announcement')
import fetch from '../../react-component/utils/fetch'
import saveFile, {
  getFilenameFromContentDisposition,
} from '../../react-component/utils/save-file'

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
  async triggerAction(event) {
    const url = event.currentTarget.getAttribute('data-url')
    try {
      let res = await fetch(url, { Accept: 'application/json' })
      if (!res.ok) {
        throw Error(res.statusText)
      }

      if (res.headers.get('content-disposition') != null) {
        const data = await res.text()
        const contentType = res.headers.get('content-type')
        let name = getFilenameFromContentDisposition(
          res.headers.get('content-disposition')
        )

        return saveFile(name, contentType, data)
      } else {
        res = await res.json()
      }

      if (res !== undefined) {
        announcement.announce({
          title: 'Success',
          message: res.message,
          type: 'success',
        })
      }
    } catch (err) {
      announcement.announce({
        title: 'Error',
        message: err.message,
        type: 'error',
      })
    }
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
