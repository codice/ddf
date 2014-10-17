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
define(['backbone', 'jquery','backboneassociations'],function (Backbone, $) {


    Backbone.Associations.SEPARATOR = '~';

    var Service = {};

    Service.Metatype = Backbone.AssociatedModel.extend({

    });

    Service.Properties = Backbone.AssociatedModel.extend({

    });

    Service.Configuration = Backbone.AssociatedModel.extend({
        configUrl: "/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0",

        defaults: {
            properties: new Service.Properties()
        },

        relations: [
            {
                type: Backbone.One,
                key: 'properties',
                relatedModel: Service.Properties,
                includeInJSON: true
            }
        ],

        initialize: function(options) {
            if(options && options.properties && options.properties['service.pid']) {
                this.set({"uuid": options.properties['service.pid'].replace(/\./g, '')});
            }
        },

        /**
         * Collect all the data to save.
         * @param pid The pid id.
         * @returns {{type: string, mbean: string, operation: string}}
         */
        collectedData: function (pid) {
            var model = this;
            var data = {
                type: 'EXEC',
                mbean: 'org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
                operation: 'update'
            };
            data.arguments = [pid];
            data.arguments.push(model.get('properties').toJSON());
            return data;
        },

        /**
         * Get the serviceFactoryPid PID
         * @param model, this is really this model.
         * @returns an ajax promise
         */
        makeConfigCall: function (model) {
            if (!model) {
                return;
            }
            var configUrl = [model.configUrl, "createFactoryConfiguration", model.get("fpid")].join("/");
            return $.ajax({type: 'GET',
                url: configUrl
            });
        },

        /**
         * When a model calls save the sync is called in Backbone.  I override it because this isn't a typical backbone
         * object
         * @return Return a deferred which is a handler with the success and failure callback.
         */
        sync: function () {
            var deferred = $.Deferred(),
                model = this,
                addUrl = [model.configUrl, "add"].join("/");
            //if it has a pid we are editing an existing record
            if(model.get('properties') && model.get('properties').get("service.pid") || (model.parents.length > 0 && model.parents[0].get('id') && !model.get('fpid'))) {
                var collect = model.collectedData(model.get('properties').get("service.pid") || model.parents[0].get('id'));
                var jData = JSON.stringify(collect);

                return $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    data: jData,
                    url: addUrl
                }).done(function (result) {
                        deferred.resolve(result);
                    }).fail(function (error) {
                        deferred.fail(error);
                    });
            } else {
                //no pid means this is a new record
                model.makeConfigCall(model).done(function (data) {
                    var collect = model.collectedData(JSON.parse(data).value);
                    var jData = JSON.stringify(collect);

                    return $.ajax({
                        type: 'POST',
                        contentType: 'application/json',
                        data: jData,
                        url: addUrl
                    }).done(function (result) {
                            deferred.resolve(result);
                        }).fail(function (error) {
                            deferred.fail(error);
                        });
                }).fail(function (error) {
                        deferred.fail(error);
                    });
            }
            return deferred;
        },
        destroy: function() {
            var deferred = $.Deferred(),
                model = this;
            var deleteUrl = [model.configUrl, "delete", model.get('properties').get("service.pid")].join("/");

            return $.ajax({
                type: 'GET',
                url: deleteUrl
            }).done(function (result) {
                  deferred.resolve(result);
                }).fail(function (error) {
                    deferred.fail(error);
                });
        },
        initializeFromMSF: function(msf) {
            this.set({"fpid":msf.get("id")});
            this.set({"name":msf.get("name")});
            this.get('properties').set({"service.factoryPid": msf.get("id")});
            this.initializeFromService(msf);
        },
        initializeFromService: function(service) {
            this.initializeFromMetatype(service.get("metatype"));
        },
        initializeFromMetatype: function(metatype) {
            var model = this;
            metatype.forEach(function(obj){
                var id = obj.get('id');
                var val = obj.get('defaultValue');
                model.get('properties').set(id, (val) ? val.toString() : null);
            });
        }
    });

    Service.ConfigurationList = Backbone.Collection.extend({
        model: Service.Configuration
    });

    Service.Model = Backbone.AssociatedModel.extend({
        configUrl: "/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0",

        defaults: {
            configurations: new Service.ConfigurationList()
        },


        relations: [
            {
                type: Backbone.Many,
                key: 'configurations',
                relatedModel: Service.Configuration,
                collectionType: Service.ConfigurationList,
                includeInJSON: true
            },
            {
                type: Backbone.Many,
                key: 'metatype',
                relatedModel: Service.Metatype,
                includeInJSON: false
            }
        ],

        initialize: function(options) {
            if(options && options.id) {
                this.set({"uuid": options.id.replace(/\./g, '')});
            }
        },

        hasConfiguration: function() {
            if(this.get('configurations')) {
                return true;
            }
            return false;
        }
    });


    Service.Response = Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.Many,
                key: 'value',
                relatedModel: Service.Model,
                includeInJSON: false
            }
        ],

        initialize: function(options) {
            if (options && options.url) {
                this.url = options.url;
            } else {
                this.url = "/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listServices";
            }
        }
    });

    return Service;

});