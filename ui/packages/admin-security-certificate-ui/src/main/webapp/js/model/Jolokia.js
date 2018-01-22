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
        'backbone',
        'backboneassociations'
    ],
    function (Backbone) {

        return Backbone.AssociatedModel.extend({
            sync: function (method, model, options) {
                if (method === 'delete') {
                    method = 'read';
                    options.url = model.deleteUrl + model.get('alias');
                } else if (method === 'update') {
                    method = 'create';
                    options.url = model.postUrl;
                    options.data = JSON.stringify(model.post());
                }
                return Backbone.sync.call(this, method, model, options);
            },
            post: function () {
                var file = this.get('file');
                if (file !== undefined) {
                    return {
                        type: 'EXEC',
                        mbean: 'org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore',
                        operation: this.postOperation,
                        arguments: [
                            this.get('alias'),
                            this.get('keypass'),
                            this.get('storepass'),
                            file.data,
                            file.type,
                            file.name
                        ]
                    };
                }
            }
        });

    });
