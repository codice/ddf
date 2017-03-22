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
        'icanhaz',
        'marionette',
        'backbone',
        'wreqr',
        'underscore',
        'jquery',
        'js/model/Node.js',
        'js/model/FieldDescriptors.js',
        'js/view/Segment.view.js',
        'text!templates/nodeModal.handlebars'
    ],
    function (ich, Marionette, Backbone, wreqr, _, $, Node, FieldDescriptors, Segment, nodeModal) {

        ich.addTemplate('modalNode', nodeModal);

        var NodeModal = {};

        NodeModal.View = Marionette.Layout.extend({
            template: 'modalNode',
            className: 'modal',

            events: {
                "click .submit-button": "submitData",
                "click .cancel-button": "cancel",
                "click .close": "cancel"
            },
            regions: {
                generalInfo: '#generalInfo',
                organizationInfo: '#organizationInfo',
                contactInfo: '#contactInfo',
                serviceInfo: '#serviceInfo',
                contentInfo: '#contentInfo'
            },

            saveErrors: undefined,
            /**
             * Initialize  the binder with the ManagedServiceFactory model.
             * @param options
             */
            initialize: function (options) {
                this.mode = options.mode;
                this.readOnly = options.readOnly;
                this.hasData = options.modelEmpty;
                if (this.hasData) {
                    this.refreshData();
                }
            },
            refreshData: function() {
                this.model.initializeData();
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.generalInfo.get('segmentId'), this.updateGeneralTabError);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.organizationInfo.get('segmentId'), this.updateOrgTabError);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.contactInfo.get('segmentId'), this.updateContactTabError);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.serviceInfo.get('segmentId'), this.updateServiceTabError);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.contentInfo.get('segmentId'), this.updateContentTabError);

                this.model.generalInfo.get('segments').forEach(function (segment){
                    segment.set('identityNode', this.model.get('identityNode'));
                }.bind(this));

                this.generalInfoView = new Segment.SegmentCollectionView({
                    model: this.model.generalInfo,
                    collection: this.model.generalInfo.get('segments'),
                    showHeader: true,
                    readOnly: this.readOnly
                });
                this.organizationInfoView = new Segment.SegmentCollectionView({
                    model: this.model.organizationInfo,
                    collection: this.model.organizationInfo.get('segments'),
                    showHeader: true,
                    readOnly: this.readOnly
                });
                this.contactInfoView = new Segment.SegmentCollectionView({
                    model: this.model.contactInfo,
                    collection: this.model.contactInfo.get('segments'),
                    showHeader: true,
                    readOnly: this.readOnly
                });
                this.serviceInfoView = new Segment.SegmentCollectionView({
                    model: this.model.serviceInfo,
                    collection: this.model.serviceInfo.get('segments'),
                    showHeader: true,
                    readOnly: this.readOnly
                });
                this.contentInfoView = new Segment.SegmentCollectionView({
                    model: this.model.contentInfo,
                    collection: this.model.contentInfo.get('segments'),
                    showHeader: true,
                    readOnly: this.readOnly
                });
                this.hasData = true;
            },
            serializeData: function () {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                }
                data.generalErrors = this.model.generalInfo.validationError;
                data.serviceErrors = this.model.serviceInfo.validationError;
                data.organizationErrors = this.model.organizationInfo.validationError;
                data.contactErrors = this.model.contactInfo.validationError;
                data.collectionErrors = this.model.contentInfo.validationError;
                data.saveErrors = this.saveErrors;
                data.validationErrors = this.model.validationError;
                data.mode = this.mode;
                data.readOnly = this.readOnly;
                data.hasData = this.hasData;

                data.name = this.getNodeName();

                if(this.model.summary.attributes) {
                    data.registryId = this.model.summary.get('registryId');
                }

                return data;
            },
            getNodeName: function() {
                var extName;
                this.model.get("RegistryObjectList").ExtrinsicObject.forEach( function (extObj) {
                    if (extObj.objectType === "urn:registry:federation:node") {
                        extName = extObj.Name;
                    }
                });
                if(!extName && this.model.summary.attributes){
                    extName = this.model.summary.get('name');
                }
                return extName;
            },
            onRender: function () {
                this.$el.attr('role', "dialog");
                this.$el.attr('aria-hidden', "true");
                if(this.hasData) {
                    this.generalInfo.show(this.generalInfoView);
                    this.organizationInfo.show(this.organizationInfoView);
                    this.contactInfo.show(this.contactInfoView);
                    this.serviceInfo.show(this.serviceInfoView);
                    this.contentInfo.show(this.contentInfoView);
                    wreqr.vent.trigger('modalSizeChanged', this.el.firstChild.clientHeight);
                }
            },
            updateGeneralTabError: function () {
               this.updateTabError("#generalTabLink", this.model.generalInfo);
            },
            updateServiceTabError: function () {
                this.updateTabError("#serviceTabLink", this.model.serviceInfo);
            },
            updateOrgTabError: function () {
                this.updateTabError("#organizationTabLink", this.model.organizationInfo);
            },
            updateContactTabError: function () {
                this.updateTabError("#contactTabLink", this.model.contactInfo);
            },
            updateContentTabError: function () {
                this.updateTabError("#collectionTabLink", this.model.contentInfo);
            },
            updateTabError: function(id, model){
                var title = this.$(id);
                if(model.validate()) {
                    title.addClass('validation-error-text');
                } else {
                    title.removeClass('validation-error-text');
                }
            },
            /**
             * Submit to the backend.
             */
            submitData: function () {
                wreqr.vent.trigger('beforesave');
                var view = this;
                var response = view.model.save();
                if (response) {
                    response.success(function () {
                        view.closeAndUnbind();
                        if (response.addOperation) {
                            wreqr.vent.trigger('nodeAdded');
                        } else {
                            wreqr.vent.trigger('nodeUpdated');
                        }
                    });
                    response.fail(function (val) {
                        view.saveErrors = val;
                        view.render();
                    });
                } else {
                    view.render();
                }

            },
            cancel: function () {
                this.saveErrors = undefined;
                this.model.validationError = undefined;
                this.closeAndUnbind();
            },
            closeAndUnbind: function () {
                this.$el.off('hidden.bs.modal');
                this.$el.off('shown.bs.modal');
                this.$el.modal("hide");
            }
        });

        return NodeModal;

    });