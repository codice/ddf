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
/*global define,window*/
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
        'text!templates/regenerateSourcesModal.handlebars',
        'text!templates/subscriptionFilterModal.handlebars',
        'text!templates/clientServerModal.handlebars'
    ],
    function (ich,Backbone,Marionette,_,moment,$,Q,wreqr,Node, NodeCollection, NodeModal,registryPage, nodeList, nodeRow, deleteNodeModal, regenerateSourcesModal, subscriptionFilterModal, clientServerModal) {

        var RegistryView = {};

        ich.addTemplate('registryPage', registryPage);
        ich.addTemplate('nodeList', nodeList);
        ich.addTemplate('nodeRow', nodeRow);
        ich.addTemplate('deleteNodeModal', deleteNodeModal);
        ich.addTemplate('regenerateSourcesModal', regenerateSourcesModal);
        ich.addTemplate('subscriptionFilterModal', subscriptionFilterModal);
        ich.addTemplate('clientServerModal', clientServerModal);
        RegistryView.RegistryPage = Marionette.Layout.extend({
            template: 'registryPage',
            events: {
                'click .add-node-link': 'showAddNode',
                'click .remove-all-node-link': 'deleteAllNodes',
                'click .refresh-button': 'addDeleteNode',
                'click .regenerate-sources': 'regenerateSources',
                'click .settings-dropdown': 'settingsDropdown',
                'click .subscription-filter': 'subscriptionFilter',
                'click .client-server-mode': 'clientServerMode'
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
                this.listenTo(wreqr.vent, "refreshNodes", this.addDeleteNode);

                new RegistryView.ModalController({
                    application: this
                });
                window.onclick = function(event){
                    if (!event.target.matches('.settings-dropdown-icon')) {
                        $('.dropdown-content').removeClass('show-dropdown');
                    }
                };
            },
            regions: {
                identityRegion: '#localIdentityNodeRegion',
                additionalRegion: '#localAdditionalNodeRegion',
                remoteNodeRegion: '#remoteNodeRegion',
                modalRegion: '#registry-modal'
            },
            onRender: function() {
                this.identityRegion.show(new RegistryView.NodeTable({collection: new NodeCollection(this.model.getIdentityNode())}));
                this.additionalRegion.show(new RegistryView.NodeTable({collection: new NodeCollection(this.model.getSecondaryNodes()), multiValued: true, region: "additional", filterInverted: this.model.filterInverted}));
                this.remoteNodeRegion.show(new RegistryView.NodeTable({collection: new NodeCollection(this.model.getRemoteNodes()), multiValued:true, region: "remote", readOnly:true, filterInverted: this.model.filterInverted}));
                if(this.model.models.length <= 1){
                    $('.regenerate-sources').prop("disabled",true);
                    $('.regenerate-sources').addClass('disabled-link');
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
            deleteAllNodes: function(table) {
                var remote, nodes, modal;
                if (table.currentTarget.id === "additional") {
                    nodes = this.model.getSecondaryNodes();
                } else if (table.currentTarget.id === "remote") {
                    remote = true;
                    nodes = this.model.getRemoteNodes();
                }
                modal = new RegistryView.DeleteAllModal({ model: this.model, remote: remote, nodes: nodes });
                wreqr.vent.trigger("showModal", modal);
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
            settingsDropdown: function(){
                $('.dropdown-content').toggleClass('show-dropdown');
            },
            subscriptionFilter: function() {
                wreqr.vent.trigger("showModal",
                    new RegistryView.SubscriptFilterModal({nodes: this.model})
                );
            },
            clientServerMode: function() {
                wreqr.vent.trigger("showModal",
                    new RegistryView.ClientServerModal({clientMode: this.model.clientMode})
                );
            },
            serializeData: function () {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                    data.waitingForData = !this.model.hasData;
                    data.clientMode = this.model.clientMode;
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
                    data.filtered = (!data.filtered && this.options.filterInverted) || (data.filtered && !this.options.filterInverted);
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
                data.region = this.options.region;
                return data;
            },
            buildItemView: function (item, ItemViewType, itemViewOptions) {
                var options = _.extend({
                    model: item,
                    readOnly: this.options.readOnly,
                    filterInverted: this.options.filterInverted
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
                data.remote = !this.model.attributes.localNode;
                return data;
           },
           getNodeName: function() {
               return this.model.get('name');
           }
        });

        RegistryView.DeleteAllModal = RegistryView.DeleteModal.extend({
            deleteNode: function() {
                var registryIds = this.options.nodes.map(
                    function (n) {
                        return n.get('registryId');
                    }
                );
                this.model.deleteNodes(registryIds);
                this.close();
            },
            serializeData: function() {
                var data = {};
                if (this.model) {
                    data = this.model.toJSON();
                }
                if (this.options.nodes.length === 0) {
                    data.noNodes = true;
                }

                data.remote = this.options.remote;
                if (data.remote) {
                    data.name = "Remote";
                } else {
                    data.name = "Additional Local";
                }

                data.deleteAll = true;
                return data;
            }
        });

        RegistryView.AbstractSettingsModal = Marionette.ItemView.extend({
            className: 'modal',
            events: {
                'click .submit-button' : 'doOperation',
                'click .cancel-button' : 'cancel',
                'click .close': 'cancel'
            },
            opUrl: '',
            operation: '',
            getArguments: function(){
                return {};
            },
            doOperation: function() {

                $('.submit-button').prop("disabled",true);
                $('.cancel-button').prop("disabled",true);
                var mbean = 'org.codice.ddf.registry:type=FederationAdminMBean';
                var modal = this;
                var data = {
                    type: 'EXEC',
                    mbean: mbean,
                    operation: this.operation
                };

                data.arguments = [this.getArguments()];
                var json = JSON.stringify(data);

                $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    data: json,
                    url: modal.opUrl
                }).done(function () {
                    modal.close();
                    wreqr.vent.trigger("refreshNodes");
                });

            },
            cancel: function() {
                this.close();
            },
            close: function() {
                this.$el.off('hidden.bs.modal');
                this.$el.off('shown.bs.modal');
                this.$el.modal("hide");
            }
        });

        RegistryView.SubscriptFilterModal = RegistryView.AbstractSettingsModal.extend({
            template: 'subscriptionFilterModal',
            opUrl: '/admin/jolokia/exec/org.codice.ddf.registry:type=FederationAdminMBean/nodeFilterProperties(java.util.Map)',
            operation: 'nodeFilterProperties(java.util.Map)',
            getArguments: function() {
                var checkboxes = $('.filtered-node-check');
                var regIds = [];
                _.each(checkboxes, function (input) {
                    if (input.checked) {
                        regIds.push($(input).attr('id'));
                    }
                });
                return {
                    filtered: regIds,
                    filterInverted: $('.invert-filtered-check')[0].checked
                };
            },
            serializeData: function() {
                var data = {};
                var nodes = [];
                data.filterInverted = this.options.nodes.filterInverted;
                _.each(this.options.nodes.models, function (node) {
                    if(node.get('identityNode')){
                        return;
                    }
                    nodes.push({
                        name: node.get('name'),
                        id: node.get('registryId'),
                        filtered: node.get('filtered')
                    });
                });
                nodes = _.sortBy(nodes, function(o){
                    return o.name.toLowerCase();
                });
                data.registry = nodes;
                return data;
            }
        });
        RegistryView.ClientServerModal = RegistryView.AbstractSettingsModal.extend({
            template: 'clientServerModal',
            opUrl: '/admin/jolokia/exec/org.codice.ddf.registry:type=FederationAdminMBean/nodeFilterProperties(java.util.Map)',
            operation: 'nodeFilterProperties(java.util.Map)',
            getArguments: function () {
                return {
                    clientMode: $('.client-server-check')[0].checked
                };
            },
            serializeData: function () {
                var data = {};
                data.clientMode = this.options.clientMode;
                return data;
            }
        });

        RegistryView.RegenerateSourcesModal = RegistryView.AbstractSettingsModal.extend({
            template: 'regenerateSourcesModal',
            className: 'modal',
            events: {
                'click .select-all-group' : 'selectAllSources',
                'click .submit-button' : 'doOperation',
                'click .cancel-button' : 'cancel',
                'click .close': 'cancel'
            },
            selectAllSources: function () {
                // Select All box will set all source checkboxes to the same state as Select All
                var checkboxes = $('.regenerate-source-check');
                var selectAll = $('.regenerate-source-check-all')[0].checked;
                _.each(checkboxes, function(input) {
                    input.checked = selectAll;
                });
            },
            opUrl: '/admin/jolokia/exec/org.codice.ddf.registry:type=FederationAdminMBean/regenerateRegistrySources',
            operation: 'regenerateRegistrySources',
            getArguments: function() {
                var checkboxes = $('.regenerate-source-check');
                var regIds = [];
                _.each(checkboxes, function (input) {
                    if (input.checked) {
                        regIds.push($(input).attr('id'));
                    }
                });
                return regIds;
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
