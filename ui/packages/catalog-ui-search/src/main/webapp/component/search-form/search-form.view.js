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
/*global require*/
const Marionette = require('marionette')
const template = require('./search-form.hbs')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance')
const DropdownModel = require('../dropdown/dropdown')
const SearchFormInteractionsDropdownView = require('../dropdown/search-form-interactions/dropdown.search-form-interactions.view')
const wreqr = require('../../exports/wreqr.js')
const Router = require('../router/router.js')
const announcement = require('../announcement')
const Common = require('../../js/Common.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('search-form'),
  className() {
    return this.model.get('createdBy') === 'system' &&
      Router.attributes.path === 'forms(/)'
      ? 'systemSearchForm'
      : 'is-button'
  },
  events: {
    click: 'changeView',
  },
  modelEvents: {
    change: 'render',
  },
  regions: {
    searchFormActions: '.choice-actions',
  },
  initialize: function() {
    this.listenTo(this.model, 'change:type', this.changeView)
    this.handleDefault()
    this.listenTo(
      user.getQuerySettings(),
      'change:template',
      this.handleDefault
    )
  },
  serializeData: function() {
    const { createdOn, ...json } = this.model.toJSON()
    return { createdOn: Common.getMomentDate(createdOn), ...json }
  },
  onRender: function() {
    if (
      this.model.get('type') === 'new-form' ||
      this.model.get('type') === 'new-result'
    ) {
      this.$el.addClass('is-static')
    } else {
      if (!this.options.hideInteractionMenu) {
        this.searchFormActions.show(
          new SearchFormInteractionsDropdownView({
            model: new DropdownModel(),
            modelForComponent: this.model,
            collectionWrapperModel: this.options.collectionWrapperModel,
            queryModel: this.options.queryModel,
            dropdownCompanionBehaviors: {
              navigation: {},
            },
          })
        )
      }
    }
  },
  changeView: function() {
    let oldType = this.options.queryModel.get('type')
    switch (this.model.get('type')) {
      case 'new-form':
        this.options.queryModel.set({
          type: 'new-form',
          associatedFormModel: this.model,
          title: this.model.get('title'),
          filterTree: this.model.get('filterTemplate'),
        })
        if (oldType === 'new-form') {
          this.options.queryModel.trigger('change:type')
        }
        this.routeToSearchFormEditor('create')
        break
      case 'custom':
        const sharedAttributes = this.model.transformToQueryStructure()
        if (
          Router.attributes.path === 'forms(/)' &&
          this.model.get('createdBy') !== 'system'
        ) {
          this.openEditor(sharedAttributes)
        } else {
          this.options.queryModel.set({
            type: 'custom',
            ...sharedAttributes,
          })
          if (oldType === 'custom') {
            this.options.queryModel.trigger('change:type')
          }
          user.getQuerySettings().set('type', 'custom')
        }
    }
    user.savePreferences()
    this.triggerCloseDropdown()
  },
  handleDefault: function() {
    this.$el.toggleClass(
      'is-default',
      user.getQuerySettings().isTemplate(this.model)
    )
  },
  triggerCloseDropdown: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  openEditor: function(sharedAttributes) {
    if (user.canWrite(this.model)) {
      this.model.set({
        ...sharedAttributes,
        id: this.model.get('id'),
        accessGroups: this.model.get('accessGroups'),
        accessIndividuals: this.model.get('accessIndividuals'),
        accessAdministrators: this.model.get('accessAdministrators'),
      })
      this.routeToSearchFormEditor(this.model.get('id'))
    } else {
      announcement.announce(
        {
          title: 'Error',
          message: `You have read-only permission on search form ${this.model.get(
            'title'
          )}.`,
          type: 'error',
        },
        3000
      )
    }
  },
  routeToSearchFormEditor: function(newSearchFormId) {
    const fragment = `forms/${newSearchFormId}`
    wreqr.vent.trigger('router:navigate', {
      fragment,
      options: {
        trigger: true,
      },
    })
  },
})
