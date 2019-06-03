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

import React from 'react'
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const IconHelper = require('../../js/IconHelper.js')
import MetacardInteractionsDropdown from '../../react-component/container/metacard-interactions/metacard-interactions-dropdown'

module.exports = Marionette.ItemView.extend({
  template({ title, icon }) {
    return (
      <React.Fragment>
        <div
          className="metacard-title"
          data-help="This is the title of the result."
        >
          <span className={icon} />
          <span>{title}</span>
        </div>
        <MetacardInteractionsDropdown model={this.model} />
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('metacard-title'),
  initialize() {
    if (this.model.length === 1) {
      this.listenTo(
        this.model
          .first()
          .get('metacard')
          .get('properties'),
        'change',
        this.handleModelUpdates
      )
    }
    this.checkTags()
  },
  handleModelUpdates() {
    this.render()
    this.checkTags()
  },
  serializeData() {
    let title, icon
    if (this.model.length === 1) {
      icon = IconHelper.getClass(this.model.first())
      title = this.model
        .first()
        .get('metacard')
        .get('properties')
        .get('title')
    } else {
      title = this.model.length + ' Items'
    }
    return {
      title,
      icon,
    }
  },
  checkTags() {
    const types = {}
    this.model.forEach(result => {
      if (result.isWorkspace()) {
        types.workspace = true
      } else if (result.isResource()) {
        types.resource = true
      } else if (result.isRevision()) {
        types.revision = true
      } else if (result.isDeleted()) {
        types.deleted = true
      }
      if (result.isRemote()) {
        types.remote = true
      }
    })

    this.$el.toggleClass('is-mixed', Object.keys(types).length > 1)
    this.$el.toggleClass('is-workspace', types.workspace !== undefined)
    this.$el.toggleClass('is-resource', types.resource !== undefined)
    this.$el.toggleClass('is-revision', types.revision !== undefined)
    this.$el.toggleClass('is-deleted', types.deleted !== undefined)
    this.$el.toggleClass('is-remote', types.remote !== undefined)
  },
})
