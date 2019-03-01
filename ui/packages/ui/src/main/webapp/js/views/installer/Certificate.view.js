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
/** Main view page for add. */
define([
  'backbone.marionette',
  'underscore',
  'backbone',
  'jquery',
  '../../models/installer/CertsModel',
  '../../models/installer/FileHelper',
  'templates/installer/certificate.handlebars',
  'modelbinder',
  'blueimp-file-upload/js/jquery.fileupload',
], function(
  Marionette,
  _,
  Backbone,
  $,
  CertsModel,
  FileHelper,
  certificateTemplate
) {
  var CertificateView = Marionette.Layout.extend({
    template: certificateTemplate,
    model: new CertsModel(),
    initialize: function() {
      this.modelBinder = new Backbone.ModelBinder()
    },

    modelEvents: {
      invalid: function(model, errors) {
        var view = this
        view.$('[name=keystorePass]').removeClass('error-border')
        view.$('[name=keyPass]').removeClass('error-border')
        view.$('[name=truststorePass]').removeClass('error-border')
        errors.forEach(function(errorItem) {
          if (errorItem.name) {
            view.$('[name=' + errorItem.name + ']').addClass('error-border')
          } else if (errorItem.id) {
            view.$('[id=' + errorItem.id + ']').addClass('error-border')
          }
        })
      },
      certErrors: 'render',
    },

    onRender: function() {
      this.modelBinder.bind(this.model, this.el)
      this.addFileListener('keystore')
      this.addFileListener('truststore')
      this.setupPopOvers()
    },

    addFileListener: function(name) {
      var view = this
      var el = this.$('.' + name + '-fileupload')
      el.fileupload({
        url: '../services/content',
        paramName: 'file',
        dataType: 'json',
        maxFileSize: 5000000,
        maxNumberOfFiles: 1,
        dropZone: el,
        add: function(e, data) {
          view.model.set(name + 'FileName', data.files[0].name)
          var fileHelper = new FileHelper()
          fileHelper.setData(data)
          fileHelper.load(function() {
            view.model.set(name + 'File', fileHelper.get('data'))
          })
          view.render()
        },
      })
    },

    setupPopOvers: function() {
      var view = this

      var keystoreOptions, truststoreOptions
      keystoreOptions = {
        title: 'Keystore Info',
        content:
          "This is a keystore file in jks or pkcs12 format that contains the system's private key and associated CA. The CN of the private key should match that of the configured hostname/FQDN.",
        trigger: 'hover',
      }
      truststoreOptions = {
        title: 'Truststore Info',
        content:
          "This is a keystore file in jks or pkcs12 format that contains the trusted CA certificates. At a minimum must contain one CA associated with the system's private key.",
        trigger: 'hover',
      }
      view.$('.keystore-info').popover(keystoreOptions)
      view.$('.truststore-info').popover(truststoreOptions)
    },
  })

  return CertificateView
})
