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
  'jquery',
  'templates/installer/configuration.handlebars',
  'templates/installer/configurationItem.handlebars',
  './Certificate.view.js',
  '../../models/installer/CertsModel.js',
  'modelbinder',
], function(
  Marionette,
  _,
  Backbone,
  $,
  configurationTemplate,
  configurationItemTemplate,
  CertificateView,
  CertificateModel
) {
  /*
   * Item View
   */
  const SystemPropertyView = Marionette.ItemView.extend({
    template: configurationItemTemplate,
    className: 'property-item col-md-6',
    initialize: function() {
      this.modelBinder = new Backbone.ModelBinder()
    },
    setupPopOvers: function() {
      const tooltipOptions = {
        content: this.model.get('description'),
        placement: 'bottom',
        trigger: 'hover',
        container: 'body',
        delay: 250,
      };

      const tooltipSelector =
        '[data-toggle="' + this.model.get('key') + '-popover"]';
      this.$el.find(tooltipSelector).popover(tooltipOptions)
    },
    onRender: function() {
      const bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
      this.modelBinder.bind(this.model, this.el, bindings, {
        modelSetOptions: { validate: true },
      })
      this.setupPopOvers()
    },
    modelEvents: {
      change: 'render',
    },
  });

  /*
   * Collection View
   */
  const SystemPropertiesView = Marionette.CollectionView.extend({
    className: 'row',
    itemView: SystemPropertyView,
  });

  /*
   * Layout
   */
  const ConfigurationView = Marionette.Layout.extend({
    template: configurationTemplate,
    className: 'full-height',
    regions: {
      configurationItems: '#config-form',
      certificates: '#certificate-configuration',
    },

    initialize: function(options) {
      this.navigationModel = options.navigationModel
      this.navigationModel.set('hidePrevious', false)

      this.certificateModel = new CertificateModel()

      this.listenTo(this.navigationModel, 'next', this.next)
      this.listenTo(this.navigationModel, 'previous', this.previous)
    },
    modelEvents: {
      change: function() {
        this.navigationModel.set('modified', true)
      },
    },

    next: function() {
      const layout = this;

      // loop through models and check for hostname change, validation errors and set redirect url
      let hostChange = true;
      let hostName;
      let port;
      let hasErrors = false;

      this.model.each(function(model) {
        hasErrors = hasErrors || model.validationError

        if (model.get('title') === 'Internal Host') {
          hostName = model.get('value')
          layout.certificateModel.set('hostName', hostName)
          if (model.get('value') === model.get('defaultValue')) {
            hostChange = false
          }
        } else if (model.get('title') === 'Internal HTTPS Port') {
          port = model.get('value')
        }
      })
      layout.navigationModel.set('redirectUrl', './index.html')

      if (!hasErrors) {
        if (hostChange) {
          const certSave = layout.certificateModel.save();
          if (certSave) {
            certSave.done(function() {
              if (!_.isEmpty(layout.certificateModel.get('certErrors'))) {
                layout.navigationModel.nextStep(
                  'Unable to save certificates. Check error messages.',
                  0
                )
              } else {
                layout.saveProperties()
              }
            })

            certSave.fail(function() {
              layout.navigationModel.nextStep(
                'Unable to save certificates. Check logs.',
                0
              )
            })
          } else {
            layout.navigationModel.nextStep(
              'Certificate validation failed. Check inputs.',
              0
            )
          }
        } else {
          layout.saveProperties()
        }
      } else {
        layout.navigationModel.nextStep(
          'System property validation failed. Check inputs.',
          0
        )
      }
    },
    previous: function() {
      //this is your hook to perform any teardown that must be done before going to the previous step
      this.navigationModel.previousStep()
    },
    onRender: function() {
      const view = this;

      const sysPropsView = new SystemPropertiesView({ collection: this.model });
      const certificateView = new CertificateView({
        model: this.certificateModel,
      });

      this.configurationItems.show(sysPropsView)
      this.certificates.show(certificateView)
    },
    saveProperties: function() {
      const layout = this;
      const propertySave = this.model.save();
      if (propertySave) {
        propertySave.done(function() {
          layout.navigationModel.nextStep('', 100)
        })

        propertySave.fail(function() {
          layout.navigationModel.nextStep(
            'Unable to Save Configuration. Check logs.',
            0
          )
        })
      } else {
        layout.navigationModel.nextStep(
          'System property validation failed. Check inputs.',
          0
        )
      }
    },
  });

  return ConfigurationView
})
