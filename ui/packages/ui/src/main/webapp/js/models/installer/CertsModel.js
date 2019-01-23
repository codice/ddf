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
/*global define, location*/
define(['backbone.marionette', 'underscore', 'backbone', 'jquery'], function(
  Marionette,
  _,
  Backbone,
  $
) {
  var CertsModel = Backbone.Model.extend({
    defaults: {
      hostName: undefined,
      keystoreFile: undefined,
      keystoreFileName: undefined,
      keystorePass: undefined,
      keyPass: undefined,
      truststoreFile: undefined,
      truststoreFileName: undefined,
      truststorePass: undefined,
      certErrors: [],
      devMode: location.search.indexOf('dev=true') > -1 ? true : false,
    },
    validate: function(attrs) {
      var validation = []

      if (this.get('devMode')) {
        return undefined
      }

      if (!attrs.keystorePass) {
        validation.push({
          message: 'Keystore password is required',
          name: 'keystorePass',
        })
      }
      if (!attrs.keyPass) {
        validation.push({
          message: 'Key password is required',
          name: 'keyPass',
        })
      }
      if (!attrs.truststorePass) {
        validation.push({
          message: 'Truststore password is required',
          name: 'truststorePass',
        })
      }
      if (!attrs.keystoreFile) {
        validation.push({
          message: 'Keystore file is required',
          id: 'keystore-drop-zone',
        })
      }
      if (!attrs.truststoreFile) {
        validation.push({
          message: 'Truststore file is required',
          id: 'truststore-drop-zone',
        })
      }

      if (validation.length > 0) {
        return validation
      }
    },
    sync: function() {
      var model = this

      var data, jdata

      if (this.get('devMode')) {
        data = {
          type: 'EXEC',
          mbean:
            'org.codice.ddf.security.certificate.generator.CertificateGenerator:service=certgenerator',
          operation: 'configureDemoCert',
          arguments: [this.get('hostName')],
        }
      } else {
        data = {
          type: 'EXEC',
          mbean:
            'org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore',
          operation: 'replaceSystemStores',
          arguments: [
            this.get('hostName'),
            this.get('keyPass'),
            this.get('keystorePass'),
            this.get('keystoreFile'),
            this.get('keystoreFileName'),
            this.get('truststorePass'),
            this.get('truststoreFile'),
            this.get('truststoreFileName'),
          ],
        }
      }

      jdata = JSON.stringify(data)

      return $.ajax({
        type: 'POST',
        contentType: 'application/json',
        data: jdata,
        url: './jolokia/exec/' + data.mbean + '/' + data.operation,
      }).done(function(result) {
        if (!model.get('devMode')) {
          model.set('certErrors', JSON.parse(result).value)
        }
        if (model.get('certErrors').length > 0) {
          model.trigger('certErrors', this)
        }
      })
    },
  })

  return CertsModel
})
