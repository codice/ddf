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
/*global define, window*/
define([
    'icanhaz',
    'underscore',
    'marionette',
    'js/models/Service',
    './ConfigurationEdit.view',
    'js/views/EmptyView',
    'js/wreqr.js',
    'js/views/Utils.js',
    'text!templates/configuration/serviceList.handlebars',
    'text!templates/configuration/serviceRow.handlebars',
    'text!templates/configuration/configurationRow.handlebars',
    'text!templates/configuration/servicePage.handlebars',
    'text!templates/configuration/configurationList.handlebars'
    ],function (ich, _, Marionette, Service, ConfigurationEdit, EmptyView, wreqr, Utils, serviceList, serviceRow, configurationRow, servicePage, configurationList) {

    var ServiceView = {};

    ich.addTemplate('serviceList', serviceList);
    ich.addTemplate('serviceRow', serviceRow);
    ich.addTemplate('configurationRow', configurationRow);
    ich.addTemplate('servicePage', servicePage);
    ich.addTemplate('configurationList', configurationList);

    ServiceView.ConfigurationRow = Marionette.Layout.extend({
        template: "configurationRow",
        tagName: "tr",
        events: {
            'click .editLink' : 'editConfiguration',
            'click .removeLink' : 'removeConfiguration'
        },
        regions: {
            editModal: '.config-modal'
        },
        editConfiguration: function() {
            wreqr.vent.trigger('poller:stop');
            this.editModal.show(new ConfigurationEdit.View({model: this.model, service: this.model.collection.parents[0]}));
            wreqr.vent.trigger('refresh');
        },
        removeConfiguration: function() {
            var question = "Are you sure you want to remove the configuration: "+this.model.get("service.pid")+"?";
            var confirmation = window.confirm(question);
            if(confirmation) {
                var configuration = this;
                configuration.model.destroy().done(function () {
                    wreqr.vent.trigger('refreshConfigurations');
                    configuration.close();
                });
            }
        }
    });

    ServiceView.ConfigurationTable = Marionette.CompositeView.extend({
        template: 'configurationList',
        itemView: ServiceView.ConfigurationRow,
        itemViewContainer: 'tbody'
    });

    ServiceView.ServiceRow = Marionette.Layout.extend({
        template: "serviceRow",
        tagName: "tr",
        events: {
            'click .newLink' : 'newConfiguration'
        },
        regions: {
            collectionRegion: '#configurationRegion',
            editModal: '.service-modal'
        },
        onRender: function() {
            this.collectionRegion.show(new ServiceView.ConfigurationTable({ collection: this.model.get("configurations") }));
        },
        /**
         * If it is a factory OR it has no existing configurations, generate a configuration from the model.
         * Otherwise, use the existing configuration.
         * Then show an editing modal with the configuration.
         */
        newConfiguration: function () {
            var configuration;
            var model = this.model;
            var hasFactory = model.get("factory");
            var existingConfigurations = model.get("configurations");

            if (hasFactory || existingConfigurations.isEmpty()) {
                wreqr.vent.trigger('poller:stop');
                configuration = (new Service.Configuration()).initializeFromModel(model);
            } else {
                configuration = existingConfigurations.at(0);
            }

            this.editModal.show(new ConfigurationEdit.View({model: configuration, service: model}));
        }
    });

    ServiceView.ServiceTable = Marionette.CompositeView.extend({
        template: 'serviceList',
        itemView: ServiceView.ServiceRow,
        itemViewContainer: 'tbody',
        emptyView: EmptyView.services,

        initialize: function(options) {
            this.showWarnings = options.showWarnings;
        },

         serializeData: function() {
             var returnValue = {
                 showWarnings: this.showWarnings
             };

             return returnValue;
        }
    });

    ServiceView.ServicePage = Marionette.Layout.extend({
        template: 'servicePage',
        regions: {
            collectionRegion: '#servicesRegion'
        },
        initialize: function(options) {
            _.bindAll(this);
            this.poller = options.poller;
            this.listenTo(this.model, 'services:refresh', this.stopRefreshSpin);
            if(this.poller){
                this.listenTo(wreqr.vent, 'poller:stop', this.stopPoller);
                this.listenTo(wreqr.vent, 'poller:start', this.startPoller);
                this.listenTo(this.model, 'sync', this.triggerSync);
            }
            this.refreshButton = Utils.refreshButton('.refreshButton', this.refreshServices, this);
            this.showWarnings = options.showWarnings;
            this.url = options.url;
        },
        onShow: function() {
            this.refreshButton.init();
        },
        onDestroy: function() {
            this.refreshButton.cleanUp();
        },
        triggerSync: function() {
            wreqr.vent.trigger('sync');
        },
        stopPoller: function() {
            this.poller.stop();
        },
        startPoller: function() {
            this.poller.start();
        },
        onRender: function() {
            this.model.get("value").comparator = function( model ) {
                return model.get('name');
            };

            var collection = this.model.get("value").sort();
            this.collectionRegion.show(new ServiceView.ServiceTable({ collection: collection, showWarnings: this.showWarnings }));
        },
        refreshServices: function() {
            wreqr.vent.trigger('refreshConfigurations');
        },
        stopRefreshSpin: function(source) {
            if (this.cid === source.view.cid) {
                this.refreshButton.done();
            }
        }
    });
    return ServiceView;

});