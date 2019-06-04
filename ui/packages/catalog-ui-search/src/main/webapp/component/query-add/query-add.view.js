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

import * as React from 'react'
const Marionette = require('marionette')
const template = require('./query-add.hbs')
const CustomElements = require('../../js/CustomElements.js')
const QueryAdvanced = require('../query-advanced/query-advanced.view.js')
const QueryTitle = require('../query-title/query-title.view.js')
const Query = require('../../js/model/Query.js')
const store = require('../../js/store.js')
const QueryConfirmationView = require('../confirmation/query/confirmation.query.view.js')
const SearchForm = require('../search-form/search-form')
const LoadingView = require('../loading/loading.view.js')
const wreqr = require('../../js/wreqr.js')
const announcement = require('../announcement/index.jsx')
const user = require('../singletons/user-instance.js')
import { InvalidSearchFormMessage } from 'component/announcement/CommonMessages'
import ExtensionPoints from '../../extension-points'

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('query-add'),
  regions: {
    queryContent: '> form > .editor-content > .content-form',
    queryTitle: '> form > .editor-content > .content-title',
    queryFooter: '> form > .editor-content > .content-footer',
  },
  events: {
    'click .editor-edit': 'edit',
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
    'click .editor-saveRun': 'saveRun',
  },
  initialize() {
    this.listenTo(user.getQuerySettings(), 'change:template', querySettings =>
      this.updateCurrentQuery(querySettings)
    )
    this.model = new Query.Model(this.getDefaultQuery())
    this.listenTo(this.model, 'resetToDefaults change:type', this.reshow)
    this.listenTo(this.model, 'change:filterTree', this.reshow)
    this.listenTo(this.model, 'closeDropdown', this.closeDropdown)
    this.listenForSave()
  },
  updateCurrentQuery(currentQuerySettings) {
    if (currentQuerySettings.get('type') === 'custom') {
      const searchForm = new SearchForm(currentQuerySettings.get('template'))
      const sharedAttributes = searchForm.transformToQueryStructure()
      this.model.set({
        type: 'custom',
        ...sharedAttributes,
      })
    }
  },
  reshow() {
    this.queryView = undefined
    const formType = this.model.get('type')
    this.$el.toggleClass('is-form-builder', formType === 'new-form')
    switch (formType) {
      case 'new-form':
        this.showFormBuilder()
        break
      case 'custom':
        this.showCustom()
        break
      case 'text':
      case 'basic':
      case 'advanced':
        this.showForm(
          ExtensionPoints.queryForms.find(form => form.id === formType)
        )
        break
      default:
        const queryForm = ExtensionPoints.queryForms.find(
          form => form.id === formType
        )
        if (queryForm) {
          this.showQueryForm(queryForm)
        }
    }
  },
  onBeforeShow() {
    this.reshow()
    this.showTitle()
  },
  getDefaultQuery() {
    let userDefaultTemplate = user.getQuerySettings().get('template')
    if (!userDefaultTemplate) {
      return {}
    }
    let sorts =
      userDefaultTemplate['querySettings'] &&
      userDefaultTemplate['querySettings'].sorts
    if (sorts) {
      sorts = sorts.map(sort => ({
        attribute: sort.split(',')[0],
        direction: sort.split(',')[1],
      }))
    }
    return {
      type: 'custom',
      title: userDefaultTemplate['title'],
      filterTree: userDefaultTemplate['filterTemplate'],
      src:
        (userDefaultTemplate['querySettings'] &&
          userDefaultTemplate['querySettings'].src) ||
        '',
      federation:
        (userDefaultTemplate['querySettings'] &&
          userDefaultTemplate['querySettings'].federation) ||
        'enterprise',
      sorts: sorts || [],
      'detail-level':
        (userDefaultTemplate['querySettings'] &&
          userDefaultTemplate['querySettings']['detail-level']) ||
        'allFields',
    }
  },
  showTitle() {
    this.queryTitle.show(
      new QueryTitle({
        model: this.model,
      })
    )
  },
  showFormBuilder() {
    this.queryContent.show(
      new QueryAdvanced({
        model: this.model,
        isForm: true,
        isFormBuilder: true,
      })
    )
  },
  showForm(form) {
    const options = form.options || {}
    this.queryContent.show(
      new form.view({
        model: this.model,
        ...options,
      })
    )
  },
  showQueryForm(form) {
    const options = form.options || {}
    const queryFormView = Marionette.LayoutView.extend({
      template: () => (
        <form.view
          model={this.model}
          options={options}
          onRef={ref => (this.queryView = ref)}
        />
      ),
    })
    this.queryContent.show(new queryFormView({}))
  },
  handleEditOnShow() {
    if (this.$el.hasClass('is-editing')) {
      this.edit()
    }
  },
  showCustom() {
    this.queryContent.show(
      new QueryAdvanced({
        model: this.model,
        isForm: true,
        isFormBuilder: false,
      })
    )
  },
  focus() {
    this.queryView
      ? this.queryView.focus()
      : this.queryContent.currentView.focus()
  },
  edit() {
    this.$el.addClass('is-editing')
    this.queryView
      ? this.queryView.edit()
      : this.queryContent.currentView.edit()
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
  },
  save() {
    this.queryView
      ? this.queryView.save()
      : this.queryContent.currentView.save()
    this.queryTitle.currentView.save()
    if (this.$el.hasClass('is-form-builder')) {
      this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
      return
    }
    if (store.getCurrentQueries().get(this.model) === undefined) {
      store.getCurrentQueries().add(this.model)
    }
    this.cancel()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  setDefaultTitle() {
    this.queryView
      ? this.queryView.setDefaultTitle()
      : this.queryContent.currentView.setDefaultTitle()
  },
  saveRun() {
    const queryContentView = this.queryView
      ? this.queryView
      : this.queryContent.currentView
    if (!queryContentView.isValid()) {
      announcement.announce(InvalidSearchFormMessage)
      return
    }
    queryContentView.save()
    this.queryTitle.currentView.save()
    if (this.model.get('title') === '') {
      this.setDefaultTitle()
    }
    if (store.getCurrentQueries().canAddQuery()) {
      store.getCurrentQueries().add(this.model)
      this.endSave()
    } else {
      this.listenTo(
        QueryConfirmationView.generateConfirmation({}),
        'change:choice',
        confirmation => {
          const choice = confirmation.get('choice')
          if (choice === true) {
            const loadingview = new LoadingView()
            store.get('workspaces').once('sync', (workspace, resp, options) => {
              loadingview.remove()
              wreqr.vent.trigger('router:navigate', {
                fragment: 'workspaces/' + workspace.id,
                options: {
                  trigger: true,
                },
              })
            })
            store.get('workspaces').createWorkspaceWithQuery(this.model)
          } else if (choice !== false) {
            store.getCurrentQueries().remove(choice)
            store.getCurrentQueries().add(this.model)
            this.endSave()
          }
        }
      )
    }
  },
  endSave() {
    this.model.startSearch()
    store.setCurrentQuery(this.model)
    this.initialize()
    this.cancel()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  listenForSave() {
    this.$el
      .off('saveQuery.' + CustomElements.getNamespace())
      .on('saveQuery.' + CustomElements.getNamespace(), e => {
        this.saveRun()
      })
  },
  closeDropdown() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
