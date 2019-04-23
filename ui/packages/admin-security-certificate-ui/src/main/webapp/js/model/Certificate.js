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

define(['backbone', 'js/model/Jolokia', 'backboneassociations'], function(
  Backbone,
  Jolokia
) {
  const Certificate = {}

  Certificate.Model = Jolokia.extend({
    relations: [
      {
        type: Backbone.One,
        key: 'issuerDn',
        relatedModel: Backbone.AssociatedModel,
      },
      {
        type: Backbone.One,
        key: 'subjectDn',
        relatedModel: Backbone.AssociatedModel,
      },
    ],
    idAttribute: 'alias',
    defaults: {
      isKey: false,
    },
    deleteUrl:
      '../jolokia/exec/org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore/deleteTrustedCertificate/',
    postUrl:
      '../jolokia/exec/org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore',
    postOperation: 'addTrustedCertificate',
    validate: function() {
      const alias = this.get('alias')
      if (alias === undefined || alias === '') {
        return 'A certificate must have an alias.'
      }
    },
  })

  Certificate.Response = Backbone.AssociatedModel.extend({
    url:
      '../jolokia/read/org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore/Truststore',
    relations: [
      {
        type: Backbone.Many,
        key: 'value',
        relatedModel: Certificate.Model,
      },
    ],
  })
  return Certificate
})
