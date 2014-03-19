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
define(function (require) {

    var ich = require('icanhaz'),
        _ = require('underscore'),
        Marionette = require('marionette'),
        ConfigurationEdit = require('/configurations/view/ConfigurationEdit.view.js');

    var ServiceView = {};

    ich.addTemplate('serviceList', require('text!/configurations/templates/serviceList.handlebars'));
    ich.addTemplate('serviceRow', require('text!/configurations/templates/serviceRow.handlebars'));
    ich.addTemplate('configurationRow', require('text!/configurations/templates/configurationRow.handlebars'));
    ich.addTemplate('servicePage', require('text!/configurations/templates/servicePage.handlebars'));
    ich.addTemplate('configurationList', require('text!/configurations/templates/configurationList.handlebars'));

    ServiceView.ConfigurationRow = Marionette.Layout.extend({
        template: "configurationRow",
        tagName: "tr",
        events: {
            'click .editLink' : 'editService',
            'click .removeLink' : 'removeService'
        },
        regions: {
            editModal: '.config-modal-container'
        },
        onRender: function() {
            this.editModal.show(new ConfigurationEdit.View({model: this.model, id: this.model.get('id'), factory: !_.isUndefined(this.model.get("fpid"))}));
        },
        editService: function() {
            this.editModal.currentView.$el.modal();
        },
        removeService: function() {
            var question = "Are you sure you want to remove the configuration: "+this.model.get("service.pid")+"?";
            var confirmation = window.confirm(question);
            if(confirmation) {
                this.model.destroy();
                this.close();
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
            'click .newLink' : 'newService'
        },
        regions: {
            collectionRegion: '#configurationRegion',
            editModal: '.service-modal-container'
        },
        onRender: function() {
            this.collectionRegion.show(new ServiceView.ConfigurationTable({ collection: this.model.get("configurations") }));
            //TODO: need to create a new configuration for this when it is clicked
//            this.editModal.show(new ConfigurationEdit.View({model: this.model, id: this.model.get('id'), factory: this.model.get("factory")}));
        },
        newService: function() {
            this.editModal.currentView.$el.modal();
        }
    });

    ServiceView.ServiceTable = Marionette.CompositeView.extend({
        template: 'serviceList',
        itemView: ServiceView.ServiceRow,
        itemViewContainer: 'tbody'
    });

    ServiceView.ServicePage = Marionette.Layout.extend({
        template: 'servicePage',
        events: {
            'click .refreshButton' : 'refreshServices'
        },
        regions: {
            collectionRegion: '#servicesRegion'
        },
        onRender: function() {
            this.collectionRegion.show(new ServiceView.ServiceTable({ collection: this.model.get("value") }));
        },
        refreshServices: function() {
            this.model.fetch();
        }
    });

    return ServiceView;

});