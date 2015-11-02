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
define([
    'marionette',
    'underscore',
    'backbone',
    'jquery'
], function (Marionette, _, Backbone, $) {

    var CertsModel = Backbone.Model.extend({
        defaults: {
            keystoreFile: undefined,
            keystoreFileName: undefined,
            keystorePass: undefined,
            keyPass: undefined,
            truststoreFile: undefined,
            truststoreFileName: undefined,
            truststorePass: undefined,
            certErrors: [],
            devMode: Backbone.history.location.search.indexOf('dev=true') > -1 ? true : false
        },
        validate: function (attrs) {
            var validation = [];

            if (this.get('devMode')) {
                return undefined;
            }

            if (!attrs.keystorePass) {
                validation.push({
                    message: 'Keystore password is required',
                    name: 'keystorePass',
                });
            }
            if (!attrs.keyPass) {
                validation.push({
                    message: 'Key password is required',
                    name: 'keyPass',
                });
            }
            if (!attrs.truststorePass) {
                validation.push({
                    message: 'Truststore password is required',
                    name: 'truststorePass',
                });
            }
            if (!attrs.keystoreFile) {
                validation.push({
                    message: 'Keystore file is required',
                    id: 'keystore-drop-zone',
                });
            }
            if (!attrs.truststoreFile) {
                validation.push({
                    message: 'Truststore file is required',
                    id: 'truststore-drop-zone',
                });
            }


            return validation.length > 0 ? validation : undefined;
        },
        sync: function () {
            var url = '/jolokia/exec/org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean/setSystemCerts';
            var deferred = $.Deferred(),
                model = this;

            var mbean = 'org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean';
            var operation = 'setSystemCerts';

            var data = {
                type: 'EXEC',
                mbean: mbean,
                operation: operation,
                arguments: [model]
            };

            data = JSON.stringify(data);

            return $.ajax({
                type: 'POST',
                contentType: 'application/json',
                data: data,
                url: url
            }).done(function (result) {
                model.set('certErrors', JSON.parse(result).value);
                if (model.get('certErrors').length === 0) {
                    deferred.resolve(result);
                }else{
                    deferred.fail(result);
                }
            }).fail(function (error) {
                deferred.fail(error);
            });
        }
    });

    return CertsModel;
});