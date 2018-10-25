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
/*global window*/
import React from 'react'
import Sharing from 'component/sharing/sharing.view'

const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const template = require('./workspace-interactions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const router = require('../router/router.js')
const user = require('../singletons/user-instance.js')
const LoadingView = require('../loading/loading.view.js')
const lightboxInstance = require('../lightbox/lightbox.view.instance.js')

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('workspace-interactions'),
  className: 'composed-menu',
  modelEvents: {
    change: 'render',
  },
  events: {
    'click .interaction-save': 'handleSave',
    'click .interaction-run': 'handleRun',
    'click .interaction-stop': 'handleStop',
    'click .interaction-subscribe': 'handleSubscribe',
    'click .interaction-unsubscribe': 'handleUnsubscribe',
    'click .interaction-new-tab': 'handleNewTab',
    'click .interaction-share': 'handleShare',
    'click .interaction-duplicate': 'handleDuplicate',
    'click .interaction-trash': 'handleTrash',
    'click .interaction-details': 'handleDetails',
    'click .workspace-interaction': 'handleClick',
  },
  ui: {},
  initialize: function() {},
  onRender: function() {
    this.$el.toggleClass('is-subscribed', Boolean(this.model.get('subscribed')))
    this.$el.toggleClass('is-local', this.model.isLocal())
    this.$el.toggleClass(
      'is-not-shareable',
      !user.canShare({
        owner: this.model.get('metacard.owner'),
        accessAdministrators:
          this.model.get('security.access-administrators') || [],
      })
    )
    this.$el.toggleClass(
      'is-not-editable',
      !user.canWrite({
        owner: this.model.get('metacard.owner'),
        accessIndividuals: this.model.get('security.access-individuals') || [],
        accessGroups: this.model.get('security.access-groups') || [],
        accessAdministrators:
          this.model.get('security.access-administrators') || [],
      })
    )
  },
  handleSave: function() {
    this.model.save()
  },
  handleRun: function() {
    store.clearOtherWorkspaces(this.model.id)
    this.model.get('queries').forEach(function(query) {
      query.startSearch()
    })
  },
  handleStop: function() {
    this.model.get('queries').forEach(function(query) {
      query.cancelCurrentSearches()
    })
  },
  handleSubscribe: function() {
    this.model.subscribe()
  },
  handleUnsubscribe: function() {
    this.model.unsubscribe()
  },
  handleNewTab: function() {
    window.open('./#workspaces/' + this.model.id)
  },
  handleShare: function() {
    lightboxInstance.model.updateTitle(
      'Workspace Sharing: ' + this.model.get('title')
    )
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      <Sharing
        key={this.model.id}
        id={this.model.id}
        lightbox={lightboxInstance}
      />
    )
  },
  handleDetails: function() {
    wreqr.vent.trigger('router:navigate', {
      fragment: 'metacards/' + this.model.id,
      options: {
        trigger: true,
      },
    })
  },
  handleDuplicate: function() {
    var loadingview = new LoadingView()
    store.get('workspaces').once('sync', function(workspace, resp, options) {
      loadingview.remove()
      wreqr.vent.trigger('router:navigate', {
        fragment: 'workspaces/' + workspace.id,
        options: {
          trigger: true,
        },
      })
    })
    store.get('workspaces').duplicateWorkspace(this.model)
  },
  handleTrash: function() {
    var loadingview = new LoadingView()
    store.getWorkspaceById(this.model.id).off(null, null, 'handleTrash')
    store.getWorkspaceById(this.model.id).once(
      'sync',
      function() {
        wreqr.vent.trigger('router:navigate', {
          fragment: 'workspaces',
          options: {
            trigger: true,
          },
        })
        loadingview.remove()
      },
      'handleTrash'
    )
    store.getWorkspaceById(this.model.id).once(
      'error',
      function() {
        loadingview.remove()
      },
      'handleTrash'
    )
    store.getWorkspaceById(this.model.id).destroy({
      wait: true,
    })
  },
  handleClick: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
