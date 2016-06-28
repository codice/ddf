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
            /**
             * Button events, right now there's a submit button
             * I do not know where to go with the cancel button.
             */
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
                this.descriptors = FieldDescriptors.retrieveFieldDescriptors();
                this.model.refreshData();
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.generalInfo.get('segmentId'), this.updateGeneralTabError);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.organizationInfo.get('segmentId'), this.updateOrgTabError);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.contactInfo.get('segmentId'), this.updateContactTabError);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.serviceInfo.get('segmentId'), this.updateServiceTabError);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.contentInfo.get('segmentId'), this.updateContentTabError);

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

                return data;
            },
            onRender: function () {
                this.$el.attr('role', "dialog");
                this.$el.attr('aria-hidden', "true");

                this.generalInfo.show(new Segment.SegmentCollectionView({
                    model: this.model.generalInfo,
                    collection: this.model.generalInfo.get('segments'),
                    showHeader: true
                }));
                this.organizationInfo.show(new Segment.SegmentCollectionView({
                    model: this.model.organizationInfo,
                    collection: this.model.organizationInfo.get('segments'),
                    showHeader: true
                }));
                this.contactInfo.show(new Segment.SegmentCollectionView({
                    model: this.model.contactInfo,
                    collection: this.model.contactInfo.get('segments'),
                    showHeader: true
                }));
                this.serviceInfo.show(new Segment.SegmentCollectionView({
                    model: this.model.serviceInfo,
                    collection: this.model.serviceInfo.get('segments'),
                    showHeader: true
                }));
                this.contentInfo.show(new Segment.SegmentCollectionView({
                    model: this.model.contentInfo,
                    collection: this.model.contentInfo.get('segments'),
                    showHeader: true
                }));
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
                this.$el.modal("hide");
            }
        });

        return NodeModal;

    });