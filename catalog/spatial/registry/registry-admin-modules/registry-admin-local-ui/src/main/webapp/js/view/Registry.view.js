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
/*jshint -W024*/
define([
        'icanhaz',
        'backbone',
        'marionette',
        'underscore',
        'moment',
        'jquery',
        'q',
        'wreqr',
        'js/model/Node.js',
        'js/model/Node.collection.js',
        'js/view/NodeModal.view.js',
        'text!templates/registryPage.handlebars',
        'text!templates/nodeList.handlebars',
        'text!templates/nodeRow.handlebars',
        'text!templates/deleteNodeModal.handlebars',
        'text!templates/regenerateSourcesModal.handlebars'
    ],
    function (ich,Backbone,Marionette,_,moment,$,Q,wreqr,Node, NodeCollection, NodeModal,registryPage, nodeList, nodeRow, deleteNodeModal, regenerateSourcesModal) {

        var RegistryView = {};

        ich.addTemplate('registryPage', registryPage);
        ich.addTemplate('nodeList', nodeList);
        ich.addTemplate('nodeRow', nodeRow);
        ich.addTemplate('deleteNodeModal', deleteNodeModal);
        ich.addTemplate('regenerateSourcesModal', regenerateSourcesModal);
        RegistryView.RegistryPage = Marionette.Layout.extend({
            template: 'registryPage',
            events: {
                'click .add-node-link': 'showAddNode',
                'click .refresh-button': 'addDeleteNode',
                'click .regenerate-sources': 'regenerateSources'
            },
            modelEvents: {
                "add": "render",
                "remove": "render"
            },
            initialize: function () {
                this.listenTo(wreqr.vent, 'editNode', this.showEditNode);
                this.listenTo(wreqr.vent, 'readOnlyNode', this.showReadOnlyNode);
                this.listenTo(wreqr.vent, "deleteNodes", this.deleteNodes);
                this.listenTo(wreqr.vent, "nodeUpdated", this.onRender);
                this.listenTo(wreqr.vent, "nodeAdded", this.addDeleteNode);
                this.listenTo(wreqr.vent, "nodeDeleted", this.addDeleteNode);

                new RegistryView.ModalController({
                    application: this
                });
            },
            regions: {
                identityRegion: '#localIdentityNodeRegion',
                additionalRegion: '#localAdditionalNodeRegion',
                remoteNodeRegion: '#remoteNodeRegion',
                modalRegion: '#registry-modal'
            },
            onRender: function() {
                this.identityRegion.show(new RegistryView.NodeTable({collection: new NodeCollection(this.model.getIdentityNode())}));
                this.additionalRegion.show(new RegistryView.NodeTable({collection: new NodeCollection(this.model.getSecondaryNodes()), multiValued: true}));
                this.remoteNodeRegion.show(new RegistryView.NodeTable({collection: new NodeCollection(this.model.getRemoteNodes()), multiValued:true, readOnly:true}));
            },
            showEditNode: function (node) {
                wreqr.vent.trigger("showModal",
                    new NodeModal.View({
                        model: node,
                        mode: 'edit'
                    })
                );
            },
            showAddNode: function () {
                if (this.model) {
                    wreqr.vent.trigger("showModal",
                        new NodeModal.View({
                            model: new Node.Model(),
                            mode: 'add'
                        })
                    );
                }
            },
            showReadOnlyNode: function (node) {
                if (this.model) {
                    wreqr.vent.trigger("showModal",
                        new NodeModal.View({
                            model: node,
                            mode: 'readOnly',
                            readOnly: true
                        })
                    );
                }
            },
            addDeleteNode: function () {
                var view = this;
                var button = view.$('.refresh-button');
                if (!button.hasClass('fa-spin')) {
                    button.addClass('fa-spin');
                }
                this.model.fetch({
                    reset: true,
                    success: function () {
                        view.fetchComplete(view);
                    }
                });
            },
            fetchComplete: function (view) {
                var button = view.$('.refresh-button');
                button.removeClass('fa-spin');
                view.render();
            },
            deleteNodes: function(model) {
                if (model) {
                    wreqr.vent.trigger("showModal",
                        new RegistryView.DeleteModal({
                            model: model
                        })
                    );
                }
            },
            regenerateSources: function() {
                    wreqr.vent.trigger("showModal",
                        new RegistryView.RegenerateSourcesModal({nodes: this.model.models})
                    );

            }
        });
        RegistryView.ModalController = Marionette.Controller.extend({
            initialize: function (options) {
                this.application = options.application;
                this.listenTo(wreqr.vent, "showModal", this.showModal);
            },
            showModal: function (modalView) {

                var region = this.application.getRegion('modalRegion');
                var iFrameModalDOM = $('#IframeModalDOM');
                modalView.$el.on('hidden.bs.modal', function () {
                    iFrameModalDOM.hide();
                });
                modalView.$el.on('shown.bs.modal', function () {
                    var extraHeight = modalView.el.firstChild.clientHeight - $('#nodeTables').height();
                    if (extraHeight > 0) {
                        iFrameModalDOM.height(extraHeight);
                        iFrameModalDOM.show();
                    }
                });
                region.show(modalView);
                region.currentView.$el.modal({
                    backdrop: 'static',
                    keyboard: false
                });
            }
        });


        RegistryView.NodeRow = Marionette.ItemView.extend({
            template: "nodeRow",
            tagName: "tr",
            className: "highlight-on-hover",
            events: {
                'click td': 'editNode',
                'click .remove-node-link': 'removeNode'
            },
            editNode: function (evt) {
                evt.stopPropagation();
                var node = this.model;
                if (this.options.readOnly) {
                    wreqr.vent.trigger('readOnlyNode', node);
                } else {
                    wreqr.vent.trigger('editNode', node);
                }
            },
            removeNode: function (evt) {
                evt.stopPropagation();
                wreqr.vent.trigger('deleteNodes', this.model);
            },
            serializeData: function () {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                }

                var extrinsicData = this.model.getObjectOfType('urn:registry:federation:node');
                if (extrinsicData.length === 1) {
                    data.name = extrinsicData[0].Name;
                    data.slots = extrinsicData[0].Slot;
                    data.slots.forEach(function (slotValue) {
                        if (slotValue.slotType === "xs:dateTime") {
                            var date = moment.parseZone(slotValue.value[0]).utc().format('MMM DD, YYYY HH:mm') + 'Z';
                            if (slotValue.name === "lastUpdated") {
                                data.lastUpdated = date;
                            } else if (slotValue.name === "liveDate") {
                                data.liveDate = date;
                            }
                        }
                    });
                }
                return data;
            }
        });

        RegistryView.NodeTable = Marionette.CompositeView.extend({
            template: 'nodeList',
            itemView: RegistryView.NodeRow,
            itemViewContainer: 'tbody',
            serializeData: function () {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                }
                data.multiValued = this.options.multiValued;
                data.readOnly = this.options.readOnly;

                return data;
            },
            buildItemView: function (item, ItemViewType, itemViewOptions) {
                var options = _.extend({
                    model: item,
                    readOnly: this.options.readOnly
                }, itemViewOptions);
                return new ItemViewType(options);
            }
        });

        RegistryView.DeleteModal = Marionette.ItemView.extend({
            template: 'deleteNodeModal',
            className: 'modal',
            events: {
                'click .submit-button' : 'deleteNode',
                'click .cancel-button' : 'cancel',
                'click .close': 'cancel'
            },
            deleteNode: function() {
                 this.model.collection.deleteNodes([this.model.get('id')]);
                 this.close();
            },
            cancel: function() {
                this.close();
            },
            close: function() {
                this.$el.off('hidden.bs.modal');
                this.$el.off('shown.bs.modal');
                this.$el.modal("hide");
            },
            serializeData: function() {
                var data = {};
                if (this.model) {
                    data = this.model.toJSON();
                }
                data.name = this.getNodeName();
                return data;
           },
           getNodeName: function() {
               return this.model.getObjectOfType('urn:registry:federation:node')[0].Name;
           }
        });

        RegistryView.RegenerateSourcesModal = Marionette.ItemView.extend({
            template: 'regenerateSourcesModal',
            className: 'modal',
            events: {
                'click .submit-button' : 'regenerateSources',
                'click .cancel-button' : 'cancel',
                'click .close': 'cancel'
            },
            regenerateUrl: '/admin/jolokia/exec/org.codice.ddf.registry:type=FederationAdminMBean/regenerateRegistrySources',
            regenerateSources: function() {
                var checkboxes = $('.regenerate-source-check');
                var regIds = [];
                _.each(checkboxes, function(input){
                    if(input.checked) {
                        regIds.push($(input).attr('id'));
                    }
                });
                if (regIds.length === 0) {
                    return;
                }
                $('.submit-button').prop("disabled",true);
                $('.cancel-button').prop("disabled",true);
                var mbean = 'org.codice.ddf.registry:type=FederationAdminMBean';
                var operation = 'regenerateRegistrySources';
                var modal = this;
                var data = {
                    type: 'EXEC',
                    mbean: mbean,
                    operation: operation
                };

                data.arguments = [regIds];
                data = JSON.stringify(data);

                $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    data: data,
                    url: this.regenerateUrl
                }).done(function () {
                    modal.close();
                });

            },
            cancel: function() {
                this.close();
            },
            close: function() {
                this.$el.off('hidden.bs.modal');
                this.$el.off('shown.bs.modal');
                this.$el.modal("hide");
            },
            serializeData: function() {
                var data = {};
                if (this.model) {
                    data = this.model.toJSON();
                }
                var array = [];
                _.each(this.options.nodes, function (node) {
                    array.push({
                        name: node.getObjectOfType('urn:registry:federation:node')[0].Name,
                        id: node.get('id')
                    });
                });
                data.registry = array;
                return data;
            }
        });

        return RegistryView;
    });
