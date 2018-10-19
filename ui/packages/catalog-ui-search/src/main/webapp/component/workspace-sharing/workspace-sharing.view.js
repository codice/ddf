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
/*global define, window*/
const Backbone = require('backbone')
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./workspace-sharing.hbs')
const itemTemplate = require('./workspace-sharing.item.hbs')
const CustomElements = require('js/CustomElements')
const user = require('component/singletons/user-instance')
const EditableRows = require('component/editable-rows/editable-rows.view')
const DropdownView = require('component/dropdown/dropdown.view')
const Loading = require('component/loading-companion/loading-companion.view')

var Input = Marionette.ItemView.extend({
  template: '<input class="form-control" type="text"/>',
  modelEvents: { change: 'updateValue' },
  events: {
    'change .form-control': 'onChange',
  },
  onRender: function() {
    this.updateValue()
  },
  valueKey: function() {
    return this.options.valueKey || 'value'
  },
  onChange: function(e) {
    this.model.set(this.valueKey(), e.target.value)
  },
  updateValue: function() {
    this.$('input').val(this.model.get(this.valueKey()))
  },
})

var IconView = Marionette.ItemView.extend({
  className: 'icon-view',
  template:
    '<i class="fa {{icon}}"></i> <span class="icon-label">{{label}}</span>',
})

var SharingByEmailView = Marionette.LayoutView.extend({
  template: '<div class="email"></div>' + '<div class="action"></div>',
  regions: {
    email: '.email',
    action: '.action',
  },
  onRender: function() {
    this.email.show(new Input({ model: this.model }))
  },
})

var EmailSharingEditor = EditableRows.extend({
  embed: function(model) {
    return new SharingByEmailView({ model: model })
  },
})

const SharingByAdminView = Marionette.LayoutView.extend({
  template: '<div class="admin"></div>' + '<div class="action"></div>',
  regions: {
    admin: '.admin',
    action: '.action',
  },
  onRender: function() {
    this.admin.show(new Input({ model: this.model }))
  },
})

const AdminSharingEditor = EditableRows.extend({
  embed: function(model) {
    return new SharingByAdminView({ model: model })
  },
})

var SharingByRoleView = Marionette.LayoutView.extend({
  className: 'row',
  template: '<div class="role">{{value}}</div>' + '<div class="action"></div>',
  regions: {
    role: '.role',
    action: '.action',
  },
  updateAction: function() {
    this.model.set('action', this.action.currentView.model.get('value')[0])
  },
  onRender: function() {
    this.action.show(
      DropdownView.createSimpleDropdown({
        list: [
          { icon: 'fa-ban', label: 'No Access', value: 'none' },
          { icon: 'fa-pencil', label: 'Can Access', value: 'access' },
        ],
        defaultSelection: [this.model.get('action')],
        customChildView: IconView,
      })
    )

    this.listenTo(
      this.action.currentView.model,
      'change:value',
      this.updateAction
    )
  },
})

var RoleSharingEditor = Marionette.CollectionView.extend({
  childView: SharingByRoleView,
})

var WorkspaceSharing = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('workspace-sharing'),
  modelEvents: {
    reset: 'render',
    sync: 'cleanup',
  },
  regions: {
    byEmail: '.workspace-sharing-by-email',
    byRole: '.workspace-sharing-by-role',
    byAdmin: '.workspace-sharing-by-admin',
  },
  events: {
    'click .save': 'save',
    'click .reset': 'render', // resetting can be done by re-rendering and flushing the state
  },
  serializeData: function() {
    return {
      link: window.location.href + '/' + this.model.get('id'),
    }
  },
  getSharingByEmail: function() {
    return (this.model.get('security.access-individuals') || []).map(function(
      email
    ) {
      return { value: email }
    })
  },
  getSharingByAdmin: function() {
    return (this.model.get('security.access-administrators') || []).map(
      function(admin) {
        return { value: admin }
      }
    )
  },
  getSharingByRole: function() {
    var roles = this.model.get('security.access-groups') || []
    var view = this

    return user
      .get('user')
      .get('roles')
      .map(function(role) {
        return {
          attribute: 'role',
          action: roles.indexOf(role) === -1 ? 'none' : 'access',
          value: role,
        }
      })
  },
  onRender: function() {
    this.collection = new Backbone.Collection(this.getSharingByRole())
    this.emailCollection = new Backbone.Collection(this.getSharingByEmail())
    this.adminCollection = new Backbone.Collection(this.getSharingByAdmin())

    this.byEmail.show(
      new EmailSharingEditor({
        collection: this.emailCollection,
      })
    )

    this.byRole.show(
      new RoleSharingEditor({
        collection: this.collection,
      })
    )

    this.byAdmin.show(
      new AdminSharingEditor({
        collection: this.adminCollection,
      })
    )
  },
  save: function() {
    var view = this

    Loading.beginLoading(view)

    var roles = this.collection
      .chain()
      .filter(function(role) {
        return role.get('action') !== 'none'
      })
      .map(function(role) {
        return role.get('value')
      })
      .value()

    var emails = this.emailCollection
      .map(function(email) {
        return (email.get('value') || '').trim()
      })
      .filter(function(email) {
        return email !== ''
      })

    const admins = this.adminCollection
      .map(function(admin) {
        return (admin.get('value') || '').trim()
      })
      .filter(function(admin) {
        return admin !== ''
      })

    this.model.set('security.access-groups', roles)
    this.model.set('security.access-individuals', emails)
    this.model.set('security.access-administrators', admins)

    this.model.save()
  },
  cleanup: function() {
    this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox')
    Loading.endLoading(this)
  },
})

module.exports = WorkspaceSharing
