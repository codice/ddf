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
  'backbone.marionette',
  'underscore',
  'backbone',
  'js/wreqr.js',
  'spin',
  'spinnerConfig',
  'jquery',
  'js/models/Alerts.js',
  'js/views/Alerts.view',
  './configuration-edit.hbs',
  'components/configuration-field/configuration-field.collection.view',
  'js/CustomElements',
], function(
  Marionette,
  _,
  Backbone,
  wreqr,
  Spinner,
  spinnerConfig,
  $,
  AlertsModel,
  AlertsView,
  template,
  ConfigurationFieldCollectionView,
  CustomElements
) {
  const ConfigurationEditView = {}
  ConfigurationEditView.spinnerConfig = _.clone(spinnerConfig)
  ConfigurationEditView.spinnerConfig.color = '#000000'
  const passwordType = 12

  ConfigurationEditView.View = Marionette.Layout.extend({
    template: template,
    tagName: CustomElements.register('configuration-edit'),
    className: 'modal-dialog',
    regions: {
      configurationItems: '#config-div',
      jolokiaError: '.alerts',
    },
    /**
     * Button events, right now there's a submit button
     * I do not know where to go with the cancel button.
     */
    events: {
      'click .submit-button': 'submitData',
      'click .cancel-button': 'cancel',
      'click .enable-checkbox': 'toggleEnable',
      'change .sourceTypesSelect': 'render',
    },

    /**
     * Initialize  the binder with the ManagedServiceFactory model.
     * @param options
     */
    initialize: function(options) {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.modelBinder = new Backbone.ModelBinder()
      this.service = options.service
      this.listenTo(wreqr.vent, 'sync', this.bind)
    },

    serializeData: function() {
      const data = this.model.toJSON()
      data.service = this.service.toJSON()
      return data
    },

    onRender: function() {
      const view = this
      this.service.get('metatype').each(function(value) {
        if (value.get('type') === passwordType) {
          const password = view.model.get('properties').get(value.get('id'))
          if (password === null) {
            view.model.get('properties').set(value.get('id'), '')
          }
        }
      })
      this.configurationItems.show(
        new ConfigurationFieldCollectionView({
          collection: this.service.get('metatype'),
          configuration: this.model,
        })
      )
      this.bind()
      this.setupPopOvers()
    },

    bind: function() {
      const view = this
      const bindings = Backbone.ModelBinder.createDefaultBindings(
        this.el,
        'name'
      )
      //this is done so that model binder wont watch these values. We need to handle this ourselves.
      delete bindings.value
      const bindObjs = _.values(bindings)
      _.each(bindObjs, function(value) {
        if (view.$(value.selector).attr('type') === 'checkbox') {
          value.converter = function(direction, bindValue) {
            bindValue = bindValue || false
            switch (direction) {
              case 'ViewToModel':
                return bindValue.toString()
              case 'ModelToView':
                return JSON.parse(String(bindValue).toLowerCase())
            }
          }
        }
      })
      this.modelBinder.bind(this.model.get('properties'), this.$el, bindings)
    },
    /**
     * Submit to the backend.
     */
    submitData: function() {
      const spinner = new Spinner(ConfigurationEditView.spinnerConfig)
      wreqr.vent.trigger('beforesave')
      const view = this
      spinner.spin(view.el)

      if (this.service && !this.model.get('properties').has('service.pid')) {
        this.model.get('properties').set('service.pid', this.service.get('id'))
      }

      this.model
        .save()
        .always(function(dataOrjqXHR, textStatus, jqXHROrerrorThrown) {
          spinner.stop()
          view.jolokiaError.show(
            new AlertsView.View({
              model: AlertsModel.Jolokia(jqXHROrerrorThrown),
            })
          )
        })
        .done(function() {
          view.options.stopEditing()
          wreqr.vent.trigger('refreshConfigurations')
        })
      wreqr.vent.trigger('sync')
      wreqr.vent.trigger('poller:start')
    },
    /**
     * unbind the model and dom during close.
     */
    onClose: function() {
      this.modelBinder.unbind()
    },
    cancel: function() {
      wreqr.vent.trigger('poller:start')
      const view = this
      _.defer(function() {
        view.options.stopEditing()
      })
    },
    /**
     * Set up the popovers based on if the selector has a description.
     */
    setupPopOvers: function() {
      const view = this
      this.service.get('metatype').forEach(function(each) {
        if (!_.isUndefined(each.get('description'))) {
          let options,
            selector = ".description[data-title='" + each.id + "']"
          options = {
            title: each.get('name'),
            content: each.get('description'),
            trigger: 'hover',
          }
          view.$(selector).popover(options)
        }
      })
    },
    /**
     * This will set the defaults and set values for properties with cardinality of nonzero
     */

    refresh: function() {
      wreqr.vent.trigger('refreshConfigurations')
    },
  })

  return ConfigurationEditView
})
