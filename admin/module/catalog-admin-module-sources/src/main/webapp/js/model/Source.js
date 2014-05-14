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
        Service = require('/sources/js/model/Service.js');

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
            if(this.sourceMap[configuration.get("sourceId")]) {
                source = this.sourceMap[configuration.get("sourceId")];
                if(enabled) {
                    source.addConfiguration(configuration);
                } else {
                    source.addDisabledConfiguration(configuration);
                }
            } else {
                source = new Source.Model();
//                this.listenTo(source, 'removeSource', this.removeSource);
                this.sourceMap[configuration.get("sourceId")] = source;
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
            delete this.sourceMap[source.get('currentConfiguration').get('sourceId')];
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
                    if(service.get("configurations") && service.get("configurations").length > 0) {
                        service.get("configurations").each(function(configuration) {
                            if(configuration.get("sourceId")) {
                                resModel.get("collection").addSource(configuration, true);
                            }
                        });
                    }
                    if(service.get("disabledConfigurations") && service.get("disabledConfigurations").length > 0) {
                        service.get("disabledConfigurations").each(function(configuration) {
                            if(configuration.get("sourceId")) {
                                resModel.get("collection").addSource(configuration, false);
                            }
                        });
                    }
                });
            }
        }
    });

    return Source;

});
