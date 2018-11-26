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
const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./metacard-title.hbs')
const CustomElements = require('../../js/CustomElements.js')
const IconHelper = require('../../js/IconHelper.js')
const store = require('../../js/store.js')
const PopoutView = require('../dropdown/popout/dropdown.popout.view.js')
const MetacardInteractionsView = require('../metacard-interactions/metacard-interactions.view.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('metacard-title'),
  regions: {
    metacardInteractions: '.metacard-interactions',
  },
  onBeforeShow: function() {
    this.metacardInteractions.show(
      PopoutView.createSimpleDropdown({
        componentToShow: MetacardInteractionsView,
        dropdownCompanionBehaviors: {
          navigation: {},
        },
        modelForComponent: this.model,
        leftIcon: 'fa fa-ellipsis-v',
      })
    )
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
      var tags = result
        .get('metacard')
        .get('properties')
        .get('metacard-tags')
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
