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
/*global define,confirm*/
define([
        'jquery',
        'underscore',
        'icanhaz',
        'marionette',
        'text!templates/storePage.handlebars',
        'text!templates/storeRow.handlebars',
        'text!templates/storeTable.handlebars',
        'js/view/UploadModal.view',
        'fileupload'
    ],
    function ($, _, ich, Marionette, storePage, storeRow, storeTable, UploadModal) {
        var StoreView = {};

        ich.addTemplate('storePage', storePage);
        ich.addTemplate('storeRow', storeRow);
        ich.addTemplate('storeTable', storeTable);

        StoreView.StoreItem = Marionette.ItemView.extend({
            template: 'storeRow',
            tagName: 'tr',
            events: {
                'click .delete': 'removeItem'
            },
            removeItem: function () {
                if (confirm('Are you sure you want to remove ' + this.model.get('alias') + '? \nThis operation cannot be undone.')) {
                    this.model.destroy();
                }
            }
        });

        StoreView.StoreCollection = Marionette.CompositeView.extend({
            childView: StoreView.StoreItem,
            template: 'storeTable',
            childViewContainer: 'tbody'
        });

        StoreView.CertificatePage = Marionette.LayoutView.extend({
            template: 'storePage',
            regions: {
                trustregion: '#trustregion',
                keyregion: '#keyregion',
                modalregion: '#modalregion'
            },
            events: {
                'click .add-key': 'addKey',
                'click .add-trust': 'addTrust'
            },
            initialize: function (options) {
                this.certificate = options.certificate;
                this.privateKey = options.privateKey;
                this.certificate.on('change', this.onRender, this);
                this.privateKey.on('change', this.onRender, this);
            },
            onRender: function () {
                this.trustregion.show(new StoreView.StoreCollection({collection: this.certificate.get('value')}));
                this.keyregion.show(new StoreView.StoreCollection({collection: this.privateKey.get('value')}));
            },
            addKey: function () {
                var modal = new UploadModal.UploadModal({
                    url: '/jolokia/exec/org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore/addPrivateKey/',
                    type: 'Private Key',
                    collection: this.privateKey.get('value')
                });
                this.modalregion.show(modal);
                modal.show();
            },
            addTrust: function () {
                var modal = new UploadModal.UploadModal({
                    url: '/jolokia/exec/org.codice.ddf.security.certificate.keystore.editor.KeystoreEditor:service=keystore/addTrustedCertificate/',
                    type: 'Certificate',
                    collection: this.certificate.get('value')
                });
                this.modalregion.show(modal);
                modal.show();
            }
        });
        return StoreView;

    });
