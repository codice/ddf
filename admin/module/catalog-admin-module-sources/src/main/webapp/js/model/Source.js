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
define(function (require) {

    var Backbone = require('backbone'),
        Service = require('js/model/Service.js'),
        _ = require('underscore');

    require('backbonerelational');

    var Source = {};

    Source.ConfigurationList = Backbone.Collection.extend({
        model: Service.Configuration
    });

    Source.Model = Backbone.Model.extend({
        configUrl: "/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui",

        addConfiguration: function(configuration) {
            if(this.get("configurations")) {
                this.get("configurations").add(configuration);
                this.listenTo(configuration, 'remove', this.removeConfiguration);
                this.trigger('change');
            } else {
                this.set({configurations: new Source.ConfigurationList()});
                this.get("configurations").add(configuration);
            }
        },
        addDisabledConfiguration: function(configuration) {
            if(this.get("disabledConfigurations")) {
                this.get("disabledConfigurations").add(configuration);
                this.listenTo(configuration, 'remove', this.removeConfiguration);
            } else {
                this.set({disabledConfigurations: new Source.ConfigurationList()});
                this.get("disabledConfigurations").add(configuration);
            }
        },
        removeConfiguration: function(configuration) {
            if(this.get("configurations").contains(configuration)) {
                this.stopListening(configuration);
                this.get("configurations").remove(configuration);
                if(this.get("configurations").length === 0) {
                    this.trigger('removeSource', this);
                }
            } else if(this.get("disabledConfigurations").contains(configuration)) {
                this.stopListening(configuration);
                this.get("disabledConfigurations").remove(configuration);
            }
        },
        setCurrentConfiguration: function(configuration) {
            this.set({currentConfiguration: configuration});
        },
        initializeFromMSF: function(msf) {
            this.set({"fpid":msf.get("id")});
            this.set({"name":msf.get("name")});
            this.initializeConfigurationFromMetatype(msf.get("metatype"));
            this.configuration.set({"service.factoryPid": msf.get("id")});
        },
        initializeConfigurationFromMetatype: function(metatype) {
            var src = this;
            src.configuration = new Source.Configuration();
            metatype.forEach(function(obj){
                var id = obj.id;
                var val = obj.defaultValue;
                src.configuration.set(id, (val) ? val.toString() : null);
            });
        }
    });

    Source.Collection = Backbone.Collection.extend({
        model: Source.Model,
        sourceMap: {},
        addSource: function(configuration, enabled) {
            var source;
            if(this.sourceMap[configuration.get("id")]) {
                source = this.sourceMap[configuration.get("id")];
                if(enabled) {
                    source.addConfiguration(configuration);
                } else {
                    source.addDisabledConfiguration(configuration);
                }
            } else {
                source = new Source.Model();
                this.sourceMap[configuration.get("id")] = source;
                if(enabled) {
                    source.setCurrentConfiguration(configuration);
                    source.addConfiguration(configuration);
                } else {
                    source.addDisabledConfiguration(configuration);
                }
                this.add(source);
            }
        },
        removeSource: function(source) {
            this.stopListening(source);
            this.remove(source);
            delete this.sourceMap[source.get('currentConfiguration').get('id')];
        },
        comparator: function(model){
            var id = model.get('currentConfiguration').id.replace('_disabled','');  // scrub the label of the _disable
            return id;
        }
    });

    Source.Response = Backbone.Model.extend({
        initialize: function(options) {
            if(options.model) {
                this.model = options.model;
                var collection = new Source.Collection();
                this.set({collection: collection});
                this.listenTo(this.model, 'change', this.parseServiceModel);
            }
        },
        parseServiceModel: function() {
            var resModel = this;
            if(this.model.get("value")) {
                this.model.get("value").each(function(service) {
                    if(!_.isEmpty(service.get("configurations"))) {
                        service.get("configurations").each(function(configuration) {
                            if(configuration.get('fpid') && configuration.get('id') && configuration.get('fpid').indexOf('Source') !== -1){
                                resModel.get("collection").addSource(configuration, true);
                            }
                        });
                    }
                });
            }
        },
        getSourceMetatypes: function() {
            var resModel = this;
            var metatypes = [];
            if(resModel.model.get('value')) {
                resModel.model.get('value').each(function(service) {
                var id = service.get('id');
                var name = service.get('name');
                if (!_.isUndefined(id) && id.indexOf('Source') !== -1 || !_.isUndefined(name) && name.indexOf('Source') !== -1) {
                    metatypes.push(service);
                }
                });
            }
            return metatypes;
        },
        getSourceModelWithServices: function() {
            var resModel = this;
            var serviceCollection = resModel.model.get('value');
            var retModel = new Source.Model();
            
            if(serviceCollection) {
                serviceCollection.each(function(service) {
                    var id = service.get('id');
                    var name = service.get('name');
                    if (!_.isUndefined(id) && id.indexOf('Source') !== -1 || !_.isUndefined(name) && name.indexOf('Source') !== -1) {
                        var config = new Service.Configuration();
                        config.initializeFromService(service);
                        retModel.addDisabledConfiguration(config);
                    }
                });
            }
            return retModel;
        },
        isSourceConfiguration: function(configuration) {
            return (configuration.get('fpid') && configuration.get('id') && configuration.get('fpid').indexOf('Source') !== -1);
        }
    });
    return Source;

});
