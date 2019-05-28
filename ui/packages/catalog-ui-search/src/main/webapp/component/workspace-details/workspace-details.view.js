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
const template = require('./workspace-details.hbs')
const CustomElements = require('../../js/CustomElements.js')
const moment = require('moment')
const user = require('../singletons/user-instance.js')
const UnsavedIndicatorView = require('../unsaved-indicator/workspace/workspace-unsaved-indicator.view.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('workspace-details'),
  regions: {
    unsavedIndicator: '.title-indicator',
  },
  initialize(options) {
    this.listenTo(
      user.get('user').get('preferences'),
      'change:homeDisplay',
      this.handleDisplayPref
    )
  },
  onRender() {
    this.handleDisplayPref()
  },
  onBeforeShow() {
    this.unsavedIndicator.show(
      new UnsavedIndicatorView({
        model: this.model,
      })
    )
  },
  handleDisplayPref() {
    this.$el.toggleClass(
      'as-list',
      user
        .get('user')
        .get('preferences')
        .get('homeDisplay') === 'List'
    )
  },
  serializeData() {
    const workspacesJSON = this.model.toJSON()
    workspacesJSON.niceDate = moment(
      workspacesJSON['metacard.modified']
    ).fromNow()
    workspacesJSON.owner = workspacesJSON['metacard.owner'] || 'Guest'
    return workspacesJSON
  },
})
