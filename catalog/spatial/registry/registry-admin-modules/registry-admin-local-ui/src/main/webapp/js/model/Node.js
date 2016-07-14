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
    'backbone',
    'underscore',
    'js/model/FieldDescriptors.js',
    'js/model/Segment.js',
    'js/model/Association.js',
    'jquery',
    'wreqr',
    'backboneassociation'


], function (Backbone, _, FieldDescriptors, Segment, Association, $, wreqr) {

    var Node = {};

    Node.Model = Backbone.Model.extend({
        createUrl: '/admin/jolokia/exec/org.codice.ddf.registry:type=FederationAdminMBean/createLocalEntry',
        updateUrl: '/admin/jolokia/exec/org.codice.ddf.registry:type=FederationAdminMBean/updateLocalEntry',

        initialize: function () {
            this.descriptors = FieldDescriptors.retrieveFieldDescriptors();
            var model = this;
            if (!model.get('id')) {
                model.set('id', 'temp-id'); //this id will be replaced on the server with a real uuid
            }

            if (!model.get('objectType')) {
                model.set('objectType', 'urn:registry:federation:node');
            }

            if (!model.get('RegistryObjectList')) {
                model.set('RegistryObjectList', {});
            }

            if (!model.get('RegistryObjectList').ExtrinsicObject) {
                model.get('RegistryObjectList').ExtrinsicObject = [];
            }

            var nodeDef = this.getObjectOfType('urn:registry:federation:node');
            if (!nodeDef || nodeDef.length === 0) {
                model.get('RegistryObjectList').ExtrinsicObject.push({
                    id: 'urn:registry:node',
                    objectType: 'urn:registry:federation:node',
                    Name: ''
                });
            }

            if (!model.get('RegistryObjectList').Service) {
                model.get('RegistryObjectList').Service = [];
            }


            if (!model.get('RegistryObjectList').Organization) {
                model.get('RegistryObjectList').Organization = [];
            }

            if (!model.get('RegistryObjectList').Person) {
                model.get('RegistryObjectList').Person = [];
            }

            if (!model.get('RegistryObjectList').Association) {
                model.get('RegistryObjectList').Association = [];
            }


            this.refreshData();

        },
        refreshData: function () {
            this.associationModel = new Association.AssociationModel({
                associations: new Association.Associations(),
                associationSegments: new Association.SegmentIds(),
                topSegment: new Segment.Segment()
            });
            
            this.generalInfo = new Segment.Segment({
                segmentName: 'General Information',
                multiValued: false,
                segmentType: 'General',
                associationModel: this.associationModel
            });
            this.generalInfo.populateFromModel(this.getObjectOfType('urn:registry:federation:node'), this.descriptors);

            this.serviceInfo = new Segment.Segment({
                segmentName: 'Services',
                multiValued: true,
                segmentType: 'Service',
                associationModel: this.associationModel
            });
            this.serviceInfo.constructTitle = FieldDescriptors.constructNameTitle;
            this.serviceInfo.populateFromModel(this.get('RegistryObjectList').Service, this.descriptors);

            this.organizationInfo = new Segment.Segment({
                segmentName: 'Organizations',
                multiValued: true,
                segmentType: 'Organization',
                associationModel: this.associationModel
            });
            this.organizationInfo.constructTitle = FieldDescriptors.constructNameTitle;
            this.organizationInfo.populateFromModel(this.get('RegistryObjectList').Organization, this.descriptors);

            this.contactInfo = new Segment.Segment({
                segmentName: 'Contacts',
                multiValued: true,
                segmentType: 'Person',
                associationModel: this.associationModel
            });
            this.contactInfo.constructTitle = FieldDescriptors.constructPersonNameTitle;
            this.contactInfo.populateFromModel(this.get('RegistryObjectList').Person, this.descriptors);

            this.contentInfo = new Segment.Segment({
                segmentName: 'Content Collections',
                multiValued: true,
                segmentType: 'Content',
                associationModel: this.associationModel
            });
            this.contentInfo.constructTitle = FieldDescriptors.constructNameTitle;
            var extrinsics = this.get('RegistryObjectList').ExtrinsicObject;
            var contentOnly = _.without(extrinsics, _.findWhere(extrinsics, {objectType: 'urn:registry:federation:node'}));
            this.contentInfo.populateFromModel(contentOnly, this.descriptors);

            this.associationModel.get('topSegment').set('segments', new Backbone.Collection([this.generalInfo, this.serviceInfo, this.organizationInfo, this.contactInfo, this.contentInfo]));
            this.associationModel.populateFromModel(this.get('RegistryObjectList').Association);
        },
        saveData: function () {
            this.generalInfo.saveData();
            this.serviceInfo.saveData();
            this.organizationInfo.saveData();
            this.contactInfo.saveData();
            this.contentInfo.saveData();
            var model = this;
            model.get('RegistryObjectList').ExtrinsicObject = [this.generalInfo.get('backingData')[0]];
            _.each(this.contentInfo.get('backingData'), function (content) {
                model.get('RegistryObjectList').ExtrinsicObject.push(content);
            });
            this.associationModel.saveData();
        },
        validate: function(){
            var errors = [];
            this.appendErrors(errors, this.generalInfo.validate());
            this.appendErrors(errors, this.serviceInfo.validate());
            this.appendErrors(errors, this.organizationInfo.validate());
            this.appendErrors(errors, this.contactInfo.validate());
            this.appendErrors(errors, this.contentInfo.validate());
            if(errors.length > 0){
                return errors;
            }
            //no errors save data to backingData
            this.saveData();
        },
        appendErrors: function(errorArray,errors){
          if(errors){
              _.each(errors, function(error){
                 errorArray.push(error);
              });
          }
        },
        sync: function () {

            var model = this;
            var mbean = 'org.codice.ddf.registry:type=FederationAdminMBean';
            var operation = 'updateLocalEntry(java.util.Map)';
            var url = this.updateUrl;
            var curId = model.get('id');
            var addOperation = false;
            if (curId === 'temp-id') {
                addOperation = true;
                operation = 'createLocalEntry(java.util.Map)';
                url = this.createUrl;
                model.unset("id", {silent: true});
            }

            var data = {
                type: 'EXEC',
                mbean: mbean,
                operation: operation
            };

            data.arguments = [model];
            data = JSON.stringify(data);
            var response = $.ajax({
                type: 'POST',
                contentType: 'application/json',
                data: data,
                url: url
            });
            response.addOperation = addOperation;
            return response;
        },
        getObjectOfType: function (type) {
            var foundObjects = [];
            var prop;
            var registryList = this.get('RegistryObjectList');
            for (prop in registryList) {
                if (registryList.hasOwnProperty(prop)) {
                    var objArray = registryList[prop];
                    for (var i = 0; i < objArray.length; i++) {
                        if (objArray[i].objectType === type) {
                            foundObjects.push(objArray[i]);
                        }
                    }
                }
            }
            return foundObjects.length > 0 ? foundObjects : [];
        }
    });

    Node.Models = Backbone.Collection.extend({
        model: Node.Model,
        url: '/admin/jolokia/exec/org.codice.ddf.registry:type=FederationAdminMBean/allRegistryMetacards',
        deleteUrl: '/admin/jolokia/exec/org.codice.ddf.registry:type=FederationAdminMBean/deleteLocalEntry',

        parse: function (raw) {
            FieldDescriptors.customFields = raw.value.customSlots;
            FieldDescriptors.autoPopulateValues = raw.value.autoPopulateValues;
            return raw.value.nodes;
        },
        getSecondaryNodes: function() {
            return this.models.filter(function(model){
                var transValues = model.get('TransientValues');
                return transValues && !transValues['registry-identity-node'] && transValues['registry-local-node'];
            });
        },
        getIdentityNode: function(){
            var array = this.models.filter(function(model){
                return model.get('TransientValues') && model.get('TransientValues')['registry-identity-node'];
            });
            if(array.length === 1){
                array[0].set('identityNode',true);
                return array[0];
            }
            return undefined;
        },
        getRemoteNodes: function(){
            return this.models.filter(function(model){
                var transValues = model.get('TransientValues');
                return !transValues || !transValues['registry-local-node'];
            });
        },
        deleteNodes: function (nodes) {
            var mbean = 'org.codice.ddf.registry:type=FederationAdminMBean';
            var operation = 'deleteLocalEntry';

            var data = {
                type: 'EXEC',
                mbean: mbean,
                operation: operation
            };

            data.arguments = [nodes];
            data = JSON.stringify(data);

            return $.ajax({
                type: 'POST',
                contentType: 'application/json',
                data: data,
                url: this.deleteUrl
            }).success(function () {
                wreqr.vent.trigger('nodeDeleted');
            });
        }

    });

    return Node;
});
