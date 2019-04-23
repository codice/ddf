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

/** Main view page for add. */
define([
  'icanhaz',
  'marionette',
  'backbone',
  'js/model/Organization.js',
  'js/view/ConfigurationEdit.view.js',
  'js/view/Organization.view.js',
  'js/model/Service.js',
  'js/view/Utils.js',
  'wreqr',
  'underscore',
  'jquery',
  'templates/sourceModal.handlebars',
  'templates/optionListType.handlebars',
  'templates/textType.handlebars',
  'templates/sourceOrganization.hbs',
], function(
  ich,
  Marionette,
  Backbone,
  Organization,
  ConfigurationEdit,
  OrganizationView,
  Service,
  Utils,
  wreqr,
  _,
  $,
  sourceModal,
  optionListType,
  textType,
  sourceOrganization
) {
  if (!ich.sourceOrganization) {
    ich.addTemplate('sourceOrganization', sourceOrganization)
  }
  if (!ich.sourceModal) {
    ich.addTemplate('sourceModal', sourceModal)
  }
  if (!ich.optionListType) {
    ich.addTemplate('optionListType', optionListType)
  }
  if (!ich.textType) {
    ich.addTemplate('textType', textType)
  }

  const ModalSource = {}

  ModalSource.View = Marionette.Layout.extend({
    template: 'sourceModal',
    className: 'modal',
    events: {
      'change .activeBindingSelect': 'handleTypeChange',
      'click .submit-button': 'submitData',
      'click .cancel-button': 'cancel',
      'click .operation-action': 'handleAction',
      'change .sourceName': 'sourceNameChanged',
    },
    regions: {
      organizationInfo: '.modal-organization',
      details: '.modal-details',
      buttons: '.source-buttons',
    },
    behaviors: [
      {
        behaviorClass: Utils.modalDismissalBehavior,
      },
    ],
    serializeData: function() {
      let data = {}

      if (this.model) {
        data = this.model.toJSON()
      }
      data.mode = this.mode
      data.reportActions = this.model.getActions('report_actions')
      data.operationActions = this.model.getActions('operation_actions')
      return data
    },
    /**
     * Initialize  the binder with the ManagedServiceFactory model.
     * @param options
     */
    initialize: function(options) {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.source = options.source
      this.modelBinder = new Backbone.ModelBinder()
      this.mode = options.mode
    },
    onRender: function() {
      const config =
        this.model.get('currentConfiguration') ||
        this.model.get('disabledConfigurations').at(0)
      const properties = config.get('properties')

      this.$el.attr('role', 'dialog')
      this.$el.attr('aria-hidden', 'true')
      this.renderNameField()
      this.renderTypeDropdown()
      if (!_.isNull(this.model)) {
        this.rebind(properties)
      }
    },
    /**
     * Renders editable name field.
     */
    renderNameField: function() {
      const model = this.model
      const $sourceName = this.$('.sourceName')
      const initialName = model.get('name')
      const data = {
        id: model.id,
        name: 'Source Name',
        defaultValue: [initialName],
        description:
          'Unique identifier for all source configurations of this type.',
      }
      $sourceName.append(ich.textType(data))
      $sourceName.val(data.defaultValue)
      Utils.setupPopOvers($sourceName, data.id, data.name, data.description)
    },
    /**
     * Renders the type dropdown box
     */
    renderTypeDropdown: function() {
      const $sourceTypeSelect = this.$('.activeBindingSelect')
      const configs = this.model.getAllConfigServices()
      $sourceTypeSelect.append(
        ich.optionListType({
          list: configs.toJSON(),
        })
      )
      $sourceTypeSelect.val(configs.at(0).get('id')).change()
    },
    handleAction: function(event) {
      const link = this.$(event.currentTarget)
      const id = link
        .attr('id')
        .split('.')
        .join('\\.')
      const failed = $(this.$('#' + id + '-failed')[0])
      const success = $(this.$('#' + id + '-success')[0])
      const spinner = $(this.$('#' + id + '-spinner')[0])
      link.addClass('inactive-link')
      spinner.css('display', 'inline-block')
      failed.hide()
      success.hide()

      this.model
        .performAction(link.attr('action-id'), link.attr('action'))
        .done(function() {
          spinner.hide()
          success.show()
          link.removeClass('inactive-link')
        })
        .fail(function() {
          spinner.hide()
          failed.show()
          link.removeClass('inactive-link')
        })
    },
    getCurrentConfiguration: function() {
      const selectedSource = this.$('.selected-source option:selected')
        .text()
        .trim()
      return this.model.getAllConfigsWithServices().filter(function(config) {
        return config.get('service').get('name') === selectedSource
      })[0]
    },
    /**
     * Submit to the backend. This is called when 'Add' or 'Save' are clicked in the Modal.
     */
    submitData: function() {
      wreqr.vent.trigger('beforesave')
      const view = this
      const service = view.getCurrentConfiguration()
      if (_.isUndefined(service.get('properties').id)) {
        const name = view
          .$('.sourceName')
          .find('input')
          .val()
          .trim()
        view.setConfigName(service, name)
      }

      service
        .save()
        .then(
          function() {
            // Since saving was successful, make publish call
            // This avoids publishing if any error occurs in service.save()

            wreqr.vent.trigger('refreshSources')
            view.closeAndUnbind()
          },

          function() {
            wreqr.vent.trigger('refreshSources')
          }
        )
        .always(function() {
          view.closeAndUnbind()
        })
    },
    sourceNameChanged: function(evt) {
      const newName = this.$(evt.currentTarget)
        .find('input')
        .val()
        .trim()
      this.checkName(newName)
    },
    checkName: function(newName) {
      const view = this
      const model = view.model
      const config = model.get('currentConfiguration')
      const disConfigs = model.get('disabledConfigurations')

      if (newName === '') {
        view.showError('A source must have a name.')
      } else if (newName !== model.get('name')) {
        if (view.nameIsValid(newName, model.get('editConfig').get('fpid'))) {
          this.setConfigName(config, newName)
          if (!_.isUndefined(disConfigs)) {
            disConfigs.each(function(cfg) {
              view.setConfigName(cfg, newName)
            })
          }
          view.clearError()
        } else {
          view.showError(
            'A configuration with the name "' +
              newName +
              '" already exists. Please choose another name.'
          )
        }
      } else {
        // model name was reverted back to original value
        view.clearError()
      }
    },
    showError: function(msg) {
      const view = this
      const $group = view.$el.find('.sourceName>.control-group')

      $group
        .find('.error-text')
        .text(msg)
        .show()
      view.$el.find('.submit-button').attr('disabled', 'disabled')
      $group.addClass('has-error')
    },
    clearError: function() {
      const view = this
      const $group = view.$el.find('.sourceName>.control-group')
      const $error = $group.find('.error-text')

      view.$el.find('.submit-button').removeAttr('disabled')
      $group.removeClass('has-error')
      $error.hide()
    },
    setConfigName: function(config, name) {
      if (!_.isUndefined(config)) {
        const properties = config.get('properties')
        properties.set({
          shortname: name,
          id: name,
        })
      }
    },
    /**
     * Returns true if any of the existing source configurations have a name matching the name parameter and false otherwise.
     */
    nameExists: function(name) {
      const configs = this.parentModel.get('collection')
      const match = configs.find(function(sourceConfig) {
        return sourceConfig.get('name') === name
      })
      return !_.isUndefined(match)
    },
    nameIsValid: function(name, fpid) {
      let valid = false
      const configs = this.source.get('collection')
      const match = configs.find(function(sourceConfig) {
        return sourceConfig.get('name') === name
      })
      if (_.isUndefined(match)) {
        valid = true
      } else {
        valid = !this.fpidExists(match, fpid)
      }
      return valid
    },
    fpidExists: function(model, fpid) {
      const modelConfig = model.get('currentConfiguration')
      const disabledConfigs = model.get('disabledConfigurations')
      let matchFound = false

      if (
        !_.isUndefined(modelConfig) &&
        (modelConfig.get('fpid') === fpid ||
          modelConfig.get('fpid') + '_disabled' === fpid)
      ) {
        matchFound = true
      } else if (!_.isUndefined(disabledConfigs)) {
        matchFound = !_.isUndefined(
          disabledConfigs.find(function(modelDisableConfig) {
            // check the ID property to ensure that the config we're checking exists server side
            // otherwise assume it's a template/placeholder for filling in the default modal form data
            if (_.isUndefined(modelDisableConfig.id)) {
              return false
            } else {
              return (
                modelDisableConfig.get('fpid') === fpid ||
                modelDisableConfig.get('fpid') + '_disabled' === fpid
              )
            }
          })
        )
      }
      return matchFound
    },
    // should be able to remove this method when the 'shortname' is removed from existing source metatypes
    getId: function(config) {
      const properties = config.get('properties')
      return properties.get('shortname') || properties.get('id')
    },
    closeAndUnbind: function() {
      wreqr.vent.trigger('refreshSources')
      this.modelBinder.unbind()
      this.$el.modal('hide')
    },
    /**
     * Unbind the model and dom during close.
     */
    onClose: function() {
      wreqr.vent.trigger('refreshSources')
      this.modelBinder.unbind()
      this.$el.off('hidden.bs.modal')
      this.$el.off('shown.bs.modal')
    },
    cancel: function() {
      this.closeAndUnbind()
    },
    /**
     *  Called when the activebinding dropdown is changed and also when the source
     *  modal is first created.
     */
    handleTypeChange: function(evt) {
      const view = this
      const $select = this.$(evt.currentTarget)
      if ($select.hasClass('activeBindingSelect')) {
        this.modelBinder.unbind()
        const config = view.model.findConfigFromId($select.val())
        view.model.set('editConfig', config)

        const properties = config.get('properties')
        view.checkName(
          view
            .$('.sourceName')
            .find('input')
            .val()
            .trim()
        )
        view.renderDetails(config)
        view.rebind(properties)
        view.$el
          .find('.control-group>.source-info')
          .text(config.get('service').attributes.description)
      }
      view.$el.trigger('shown.bs.modal')
    },
    rebind: function(properties) {
      const $boundData = this.$el.find('.bound-controls')
      const bindings = Backbone.ModelBinder.createDefaultBindings(
        $boundData,
        'name'
      )
      //this is done so that model binder wont watch these values. We need to handle this ourselves.
      delete bindings.value
      this.modelBinder.bind(properties, $boundData, bindings)
    },
    renderDetails: function(configuration) {
      const service = configuration.get('service')
      if (!_.isUndefined(service)) {
        const toDisplay = service.get('metatype').filter(function(mt) {
          return !_.contains(['shortname', 'id'], mt.get('id'))
        })
        this.details.show(
          new ConfigurationEdit.ConfigurationCollection({
            collection: new Service.MetatypeList(toDisplay),
            service: service,
            configuration: configuration,
          })
        )
      } else {
        this.$(this.organizationInfo.el).html('')
        this.$(this.details.el).html('')
        this.$(this.buttons.el).html('')
      }
    },
  })
  return ModalSource
})
