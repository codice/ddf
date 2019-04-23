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
  const UrlCertificate = {}

  UrlCertificate.Model = Jolokia.extend({
    idAttribute: 'subjectDn',
  })

  UrlCertificate.Response = Backbone.AssociatedModel.extend({
    url:
      '../jolokia/exec/org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore/certificateDetails',
    saveUrl:
      '../jolokia/exec/org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore/addTrustedCertificateFromUrl',
    relations: [
      {
        type: Backbone.One,
        key: 'value',
        relatedModel: UrlCertificate.Model,
      },
    ],
  })
  return UrlCertificate
})
