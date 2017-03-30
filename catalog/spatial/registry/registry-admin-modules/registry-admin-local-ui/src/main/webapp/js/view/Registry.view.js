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
                if(this.model.models.length <= 1){
                    $('.regenerate-sources').prop("disabled",true);
                }
            },
            showEditNode: function (node) {
                this.showNode(node,'edit', false);
            },
            showAddNode: function () {
                this.showNode({},'add', false);
            },
            showReadOnlyNode: function (node) {
                this.showNode(node,'readOnly', true);
            },
            showNode: function(node, mode, readOnly) {
                if (this.model) {
                    var nodeModel = new Node.Model({summary: node});
                    var modal = new NodeModal.View({
                        model: nodeModel,
                        mode: mode,
                        readOnly: readOnly,
                        modelEmpty: mode === 'add'
                    });
                    if(mode !== 'add') {
                        nodeModel.fetch({
                            success: function () {
                                modal.refreshData();
                                modal.render();
                            }
                        });
                    }
                    wreqr.vent.trigger("showModal", modal);
                }
            },
            addDeleteNode: function () {
                var view = this;
                this.model.hasData = false;
                this.render();
                $('.refresh-button').prop("disabled",true);

                this.model.fetch({
                    reset: true,
                    success: function () {
                        view.fetchComplete(view);
                    }
                });
            },
            fetchComplete: function (view) {
                $('.refresh-button').prop("disabled",false);
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
            },
            serializeData: function () {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                    data.waitingForData = !this.model.hasData;
                }

                return data;
            }
        });
        RegistryView.ModalController = Marionette.Controller.extend({
            initialize: function (options) {
                this.application = options.application;
                this.listenTo(wreqr.vent, "showModal", this.showModal);
                this.listenTo(wreqr.vent, "modalSizeChanged", this.adjustHeightForModal);
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
            },
            adjustHeightForModal: function(modalHeight){
                var iFrameModalDOM = $('#IframeModalDOM');
                var extraHeight = modalHeight - $('#nodeTables').height();
                if (extraHeight > 0) {
                    iFrameModalDOM.height(extraHeight);
                }
            }
        });


        RegistryView.NodeRow = Marionette.ItemView.extend({
            template: "nodeRow",
            tagName: "tr",
            className: "highlight-on-hover",
            events: {
                'click td': 'editNode',
                'click .remove-node-link': 'removeNode',
                'click .report-node-link': 'reportLink'
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
            reportLink: function(evt) {
                evt.stopPropagation();
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
                data.lastUpdated = moment.parseZone(this.model.get('modified')).utc().format('MMM DD, YYYY HH:mm') + 'Z';
                data.liveDate = moment.parseZone(this.model.get('created')).utc().format('MMM DD, YYYY HH:mm') + 'Z';

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
                 this.model.collection.deleteNodes([this.model.get('registryId')]);
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
               return this.model.get('name');
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
                var nodes = [];
                _.each(this.options.nodes, function (node) {
                    if(node.get('identityNode')){
                        return;
                    }
                    nodes.push({
                        name: node.get('name'),
                        id: node.get('registryId')
                    });
                });
                nodes = _.sortBy(nodes, function(o){
                   return o.name.toLowerCase();
                });
                data.registry = nodes;
                return data;
            }
        });

        return RegistryView;
    });
