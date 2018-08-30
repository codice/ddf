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
define([
  'marionette',
  'underscore',
  'jquery',
  './workspace-menu.hbs',
  'js/CustomElements',
  'component/content-title/content-title.view',
  'component/dropdown/dropdown',
  'component/dropdown/workspace-interactions/dropdown.workspace-interactions.view',
  'component/dropdown/query/dropdown.query.view',
  'js/store',
  'component/save/workspace/workspace-save.view',
], function(
  Marionette,
  _,
  $,
  template,
  CustomElements,
  TitleView,
  DropdownModel,
  WorkspaceInteractionsView,
  QueryView,
  store,
  SaveView
) {
  return Marionette.LayoutView.extend({
    setDefaultModel: function() {
      this.model = store.get('content')
    },
    template: template,
    tagName: CustomElements.register('workspace-menu'),
    regions: {
      title: '.content-title',
      workspaceSave: '.content-save',
      workspaceQuery: '.content-adhoc',
      workspaceInteractions: '.content-interactions',
    },
    initialize: function(options) {
      if (options.model === undefined) {
        this.setDefaultModel()
      }
    },
    onFirstRender() {
      this.listenTo(this.model, 'change:currentWorkspace', this.updateSubViews)
      this.listenTo(this.model, 'change:currentWorkspace', this.handleSaved)
    },
    onBeforeShow: function() {
      this.title.show(new TitleView())
      this.showSubViews()
      this.handleSaved()
    },
    showSubViews() {
      this.showWorkspaceSave()
      this.showWorkspaceQuery()
      this.showWorkspaceInteractions()
    },
    showWorkspaceQuery() {
      if (this.model.get('currentWorkspace')) {
        this.workspaceQuery.show(
          new QueryView({
            model: new DropdownModel(),
            modelForComponent: this.model.get('currentWorkspace'),
          })
        )
      }
    },
    showWorkspaceSave() {
      if (this.model.get('currentWorkspace')) {
        this.workspaceSave.show(
          new SaveView({
            model: this.model.get('currentWorkspace'),
          })
        )
      }
    },
    showWorkspaceInteractions() {
      if (this.model.get('currentWorkspace')) {
        this.workspaceInteractions.show(
          new WorkspaceInteractionsView({
            model: new DropdownModel(),
            modelForComponent: this.model.get('currentWorkspace'),
            dropdownCompanionBehaviors: {
              navigation: {},
            },
          })
        )
      }
    },
    updateSubViews: function(workspace) {
      if (workspace && workspace.changed.currentWorkspace) {
        this.showSubViews()
      }
    },
    handleSaved: function() {
      var currentWorkspace = this.model.get('currentWorkspace')
      this.$el.toggleClass(
        'is-saved',
        currentWorkspace ? currentWorkspace.isSaved() : false
      )
    },
  })
})
