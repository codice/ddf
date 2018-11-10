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
const Marionette = require('marionette')
const template = require('./search-form-interactions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const LoadingView = require('../loading/loading.view.js')
const announcement = require('../announcement/index.jsx')
const ConfirmationView = require('../confirmation/confirmation.view.js')
const lightboxInstance = require('../lightbox/lightbox.view.instance.js')
const QueryTemplateSharing = require('../query-template-sharing/query-template-sharing.view.js')
const wreqr = require('../../exports/wreqr.js')

module.exports = Marionette.ItemView.extend({
  template: template,
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
  initialize: function() {
    this.listenTo(
      user.getQuerySettings(),
      'change',
      this.checkIfDefaultSearchForm
    )
  },
  onRender: function() {
    this.checkIfSubscribed()
    this.checkIfDefaultSearchForm()
    this.checkIfResultForm()
    this.isSystemTemplate()
    this.isShareableTemplate()
  },
  checkIfSubscribed: function() {
    this.$el.toggleClass('is-subscribed', Boolean(this.model.get('subscribed')))
  },
  handleTrash: function() {
    let loginUser = user.get('user')
    if (loginUser.get('email') === this.model.get('createdBy')) {
      this.listenTo(
        ConfirmationView.generateConfirmation({
          prompt: 'This will permanently delete the template. Are you sure?',
          no: 'Cancel',
          yes: 'Delete',
        }),
        'change:choice',
        function(confirmation) {
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
              error: function(model, xhr, options) {
                announcement.announce(
                  {
                    title: 'Error!',
                    message: 'Unable to delete the forms: ' + xhr.responseText,
                    type: 'error',
                  },
                  2500
                )
                throw new Error('Error Deleting Template: ' + xhr.responseText)
              }.bind(this),
              success: function(model, xhr, options) {
                this.options.collectionWrapperModel.deleteCachedTemplateById(
                  this.model.id
                )
              }.bind(this),
            })
            loadingview.remove()
            const template = user.getQuerySettings().get('template')
            if (!template) {
              user.getQuerySettings().set('type', 'text')
              user.savePreferences()
              this.options.queryModel.resetToDefaults()
            } else if (id === template.id) {
              this.handleClearDefault()
              this.options.queryModel.resetToDefaults()
            } else {
              const defaults = {
                type: 'custom',
                title: template.name,
                filterTree: template.filterTemplate,
                src:
                  (template.querySettings && template.querySettings.src) || '',
                federation:
                  (template.querySettings &&
                    template.querySettings.federation) ||
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
        }.bind(this)
      )
    } else {
      this.messageNotifier(
        'Error!',
        'Unable to delete the form: You are not the author',
        'error'
      )
      throw new Error('Unable to delete the form: You are not the author ')
    }
    this.trigger('doneLoading')
  },
  checkIfResultForm() {
    this.$el.toggleClass(
      'is-result-form-template',
      this.model.get('type') === 'result'
    )
  },
  handleMakeDefault: function() {
    user.getQuerySettings().set({
      type: 'custom',
      template: this.model.toJSON(),
    })
    user.savePreferences()
    this.messageNotifier(
      'Success!',
      `\"${this.model.get('name')}\" Saved As Default Query Form`,
      'success'
    )
  },
  handleClearDefault: function() {
    user.getQuerySettings().set({
      template: undefined,
      type: 'text',
    })
    user.savePreferences()
    this.messageNotifier('Success!', `Default Query Form Cleared`, 'success')
  },
  checkIfDefaultSearchForm: function() {
    this.$el.toggleClass(
      'is-current-template',
      user.getQuerySettings().isTemplate(this.model)
    )
  },
  messageNotifier: function(title, message, type) {
    announcement.announce({
      title: title,
      message: message,
      type: type,
    })
  },
  handleShare: function() {
    lightboxInstance.model.updateTitle(this.options.sharingLightboxTitle)
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new QueryTemplateSharing({
        model: this.options.modelForComponent,
      })
    )
    this.handleClick()
  },
  isSystemTemplate: function() {
    this.$el.toggleClass(
      'is-system-template',
      this.model.get('createdBy') === 'system'
    )
  },
  isShareableTemplate: function() {
    const notShareable = function(that) {
      const loginUser = user.get('user')
      if (loginUser.get('email') === that.model.get('createdBy')) {
        return false
      }
      const accessAdministrators = that.model.get('accessAdministrators') || []
      return !accessAdministrators.includes(loginUser.get('email'))
    }
    this.$el.toggleClass('is-not-shareable-template', notShareable(this))
  },
  handleEdit: function() {
    if (this.model.get('type') === 'custom') {
      this.model.set({
        title: this.model.get('name'),
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
        title: this.model.get('name'),
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
  handleClick: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
