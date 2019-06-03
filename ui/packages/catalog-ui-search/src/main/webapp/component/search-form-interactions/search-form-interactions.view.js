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
import { Sharing } from '../../react-component/container/sharing'

const Marionette = require('marionette')
const template = require('./search-form-interactions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const LoadingView = require('../loading/loading.view.js')
const announcement = require('../announcement/index.jsx')
const ConfirmationView = require('../confirmation/confirmation.view.js')
const lightboxInstance = require('../lightbox/lightbox.view.instance.js')
const wreqr = require('../../exports/wreqr.js')

module.exports = Marionette.ItemView.extend({
  template,
  tagName: CustomElements.register('search-form-interactions'),
  className: 'composed-menu',
  modelEvents: {
    change: 'render',
  },
  events: {
    'click .interaction-default': 'handleMakeDefault',
    'click .interaction-clear': 'handleClearDefault',
    'click .interaction-trash': 'handleTrash',
    'click .interaction-share': 'handleShare',
    'click .interaction-edit': 'handleEdit',
    click: 'handleClick',
  },
  ui: {},
  initialize() {
    this.listenTo(user.getQuerySettings(), 'change', function() {
      this.$el.toggleClass(
        'is-current-template',
        user.getQuerySettings().isTemplate(this.model)
      )
    })
  },
  onRender() {
    this.$el.toggleClass('is-subscribed', Boolean(this.model.get('subscribed')))
    this.$el.toggleClass(
      'is-current-template',
      user.getQuerySettings().isTemplate(this.model)
    )
    this.$el.toggleClass(
      'is-result-form-template',
      this.model.get('type') === 'result'
    )
    this.$el.toggleClass(
      'is-system-template',
      this.model.get('createdBy') === 'system'
    )
    this.$el.toggleClass(
      'is-not-shareable-template',
      !user.canShare(this.model)
    )
    this.$el.toggleClass('is-not-editable-template', !user.canWrite(this.model))
  },
  handleTrash() {
    this.listenTo(
      ConfirmationView.generateConfirmation({
        prompt: 'This will permanently delete the search form. Are you sure?',
        no: 'Cancel',
        yes: 'Delete',
      }),
      'change:choice',
      confirmation => {
        if (confirmation.get('choice')) {
          let loadingview = new LoadingView()
          this.model.url = './internal/forms/' + this.model.get('id')
          const id = this.model.get('id')
          this.model.destroy({
            data: JSON.stringify({
              'metacard.owner': this.model.get('createdBy'),
            }),
            contentType: 'application/json',
            wait: true,
            error: (model, xhr, options) => {
              announcement.announce(
                {
                  title: 'Error',
                  message: 'Unable to delete the forms: ' + xhr.responseText,
                  type: 'error',
                },
                2500
              )
              throw new Error('Error Deleting Template: ' + xhr.responseText)
            },
            success: (model, xhr, options) => {
              this.options.collectionWrapperModel.deleteCachedTemplateById(
                this.model.id
              )
            },
          })
          loadingview.remove()
          const template = user.getQuerySettings().get('template')
          if (!template) {
            user.getQuerySettings().set('type', 'text')
            user.savePreferences()
            if (this.options.queryModel) {
              this.options.queryModel.resetToDefaults()
            }
          } else if (id === template.id) {
            this.handleClearDefault()
            this.options.queryModel.resetToDefaults()
          } else {
            const defaults = {
              type: 'custom',
              title: template.title,
              filterTree: template.filterTemplate,
              src: (template.querySettings && template.querySettings.src) || '',
              federation:
                (template.querySettings && template.querySettings.federation) ||
                'enterprise',
              sorts: template.sorts,
              'detail-level':
                (template.querySettings &&
                  template.querySettings['detail-level']) ||
                'allFields',
            }
            this.options.queryModel.resetToDefaults(defaults)
          }
        }
      }
    )
    this.trigger('doneLoading')
  },
  handleMakeDefault() {
    user.getQuerySettings().set({
      type: 'custom',
      template: this.model.toJSON(),
    })
    user.savePreferences()
    this.messageNotifier(
      'Success',
      `\"${this.model.get('title')}\" Saved As Default Query Form`,
      'success'
    )
  },
  handleClearDefault() {
    user.getQuerySettings().set({
      template: undefined,
      type: 'text',
    })
    user.savePreferences()
    this.messageNotifier('Success', `Default Query Form Cleared`, 'success')
  },
  messageNotifier(title, message, type) {
    announcement.announce({
      title,
      message,
      type,
    })
  },
  handleShare() {
    lightboxInstance.model.updateTitle(this.options.sharingLightboxTitle)
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      <Sharing
        key={this.model.id}
        id={this.model.id}
        lightbox={lightboxInstance}
      />
    )
    this.handleClick()
  },
  handleEdit() {
    if (this.model.get('type') === 'custom') {
      this.model.set({
        title: this.model.get('title'),
        filterTree: this.model.get('filterTemplate'),
        id: this.model.get('id'),
        accessGroups: this.model.get('accessGroups'),
        accessIndividuals: this.model.get('accessIndividuals'),
        accessAdministrators: this.model.get('accessAdministrators'),
      })
      const fragment = `forms/${this.model.get('id')}`
      wreqr.vent.trigger('router:navigate', {
        fragment,
        options: {
          trigger: true,
        },
      })
    } else if (this.model.get('type') === 'result') {
      this.model.set({
        type: 'result',
        title: this.model.get('title'),
        formId: this.model.get('id'),
        accessGroups: this.model.get('accessGroups'),
        accessIndividuals: this.model.get('accessIndividuals'),
        accessAdministrators: this.model.get('accessAdministrators'),
        descriptors: this.model.get('descriptors'),
        description: this.model.get('description'),
      })
      this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
      this.model.trigger('change:type')
    }
  },
  handleClick() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
