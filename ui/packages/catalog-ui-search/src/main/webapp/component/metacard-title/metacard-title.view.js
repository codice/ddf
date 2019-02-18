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
import * as React from 'react'
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const IconHelper = require('../../js/IconHelper.js')
const PopoutView = require('../dropdown/popout/dropdown.popout.view.js')
const MetacardInteractionsView = require('../metacard-interactions/metacard-interactions.view.js')
const Backbone = require('backbone')
require('../../behaviors/dropdown.behavior.js')

module.exports = Marionette.ItemView.extend({
  template({ title, icon }) {
    return (
      <React.Fragment>
        <div
          className="metacard-title"
          data-help="This is the title of the result."
        >
          <span className={icon} />
          {title}
        </div>
        <button
          className="metacard-interactions is-button"
          title="Provides a list of actions to take on the result."
          data-help="Provides a list
                        of actions to take on the result."
        >
          <span className="fa fa-ellipsis-v" />
        </button>
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('metacard-title'),
  behaviors() {
    return {
      dropdown: {
        dropdowns: [
          {
            selector: '.metacard-interactions',
            view: MetacardInteractionsView.extend({
              behaviors: {
                navigation: {},
              },
            }),
            viewOptions: {
              model: this.options.model,
            },
          },
        ],
      },
    }
  },
  initialize: function() {
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
  handleModelUpdates: function() {
    this.render()
    this.onBeforeShow()
    this.checkTags()
  },
  serializeData: function() {
    var title, icon
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
      title: title,
      icon: icon,
    }
  },
  checkTags: function() {
    var types = {}
    this.model.forEach(function(result) {
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
