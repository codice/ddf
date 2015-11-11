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
/** Main view page for add. */
define([
        'icanhaz',
        'marionette',
        'backbone',
        'js/view/ConfigurationEdit.view.js',
        'js/model/Service.js',
        'js/view/Utils.js',
        'wreqr',
        'underscore',
        'jquery',
        'text!templates/sourceModal.handlebars',
        'text!templates/optionListType.handlebars',
        'text!templates/textType.handlebars'
],
function (ich,Marionette,Backbone,ConfigurationEdit,Service,Utils,wreqr,_,$,modalSource,optionListType,textType) {

    ich.addTemplate('modalSource', modalSource);
    if (!ich.optionListType) {
        ich.addTemplate('optionListType', optionListType);
    }
    if (!ich.textType) {
        ich.addTemplate('textType', textType);
    }

    var ModalSource = {};

    ModalSource.View = Marionette.Layout.extend({
        template: 'modalSource',
        className: 'modal',
        /**
         * Button events, right now there's a submit button
         * I do not know where to go with the cancel button.
         */
        events: {
            "change .sourceTypesSelect" : "handleTypeChange",
            "click .submit-button": "submitData",
            "click .cancel-button": "cancel",
            "change .sourceName": "sourceNameChanged"
        },
        regions: {
            details: '.modal-details',
            buttons: '.source-buttons'
        },
        serializeData: function(){
            var data = {};

            if(this.model) {
                data = this.model.toJSON();
            }
            data.mode = this.mode;

            return data;
        },
        /**
         * Initialize  the binder with the ManagedServiceFactory model.
         * @param options
         */
        initialize: function(options) {
            _.bindAll(this);
            this.source = options.source;
            this.modelBinder = new Backbone.ModelBinder();
            this.mode = options.mode;
        },
        onRender: function() {
            var config = this.model.get('currentConfiguration') || this.model.get('disabledConfigurations').at(0);
            var properties = config.get('properties');

            this.$el.attr('role', "dialog");
            this.$el.attr('aria-hidden', "true");
            this.renderNameField();
            this.renderTypeDropdown();
            this.initRadioButtonUI(properties);
            if (!_.isNull(this.model)) {
                this.rebind(properties);
            }
        },
        initRadioButtonUI: function(boundModel) {
            var $radios = this.$el.find('input[type=radio]');
            var view = this;

            _.each($radios, function(radio) {
                var $radio = view.$(radio);
                var $label = $radio.closest('label.btn');
                
                if (boundModel.get($radio.attr('name')) === $radio.attr('value')) {
                    $label.addClass('active');
                } else {
                    $label.removeClass('active');
                }
            });
        },
        /**
         * Renders editable name field.
         */
        renderNameField: function() {
            var model = this.model;
            var $sourceName = this.$(".sourceName");
            var initialName = model.get('name');
            var data = {
                id: model.id,
                name: 'Source Name',
                defaultValue: [initialName],
                description: 'Unique identifier for all source configurations of this type.'
            };
            $sourceName.append(ich.textType(data));
            $sourceName.val(data.defaultValue);
            Utils.setupPopOvers($sourceName, data.id, data.name, data.description);
        },
        /**
         * Renders the type dropdown box
         */
        renderTypeDropdown: function() {
            var $sourceTypeSelect = this.$(".sourceTypesSelect");
            var configs = this.getAllConfigs();
            $sourceTypeSelect.append(ich.optionListType({"list": configs.toJSON()}));
            $sourceTypeSelect.val(configs.at(0).get('id')).change();
        },
        getAllConfigs: function() {
            var configs = new Backbone.Collection();
            var disabledConfigs = this.model.get('disabledConfigurations');
            var currentConfig = this.model.get('currentConfiguration');
            if (!_.isUndefined(currentConfig)) {
                var currentService = currentConfig.get('service');
                configs.add(currentService);
            }
            if (!_.isUndefined(disabledConfigs)) {
                disabledConfigs.each(function(config) {
                    configs.add(config.get('service'));
                });
            }
            return configs;
        },
        /**
         * Submit to the backend.
         */
        submitData: function() {
            wreqr.vent.trigger('beforesave');
            var view = this;
            var service = view.model.get('editConfig');
            if (service) {
            var fpid = service.get("fpid");
            var idx = fpid.indexOf("_disabled");
            if (idx > 0) {
               service.set("fpid", fpid.substring(0, idx));
            }
            service.save().then(function(response) {
                var needsRefresh = true;
                // check to see if the service corresponds to an existing source
                // if it does, return the source
                var existingSource = view.source.get('collection').find(function(item) {
                    var config = item.get('currentConfiguration');
                   return (config && config.get('properties').id === service.get('properties').id);
                });
                var jsonResult;
                // Logic to check if the service we are adding is going to be a member of an already existing source;
                // if it is, we need to disable the new service as soon at is created.
                if (existingSource && view.mode === 'add') {
                    try {
                        jsonResult = JSON.parse(response.toString().trim());
                    } catch (e) {
                        // https://codice.atlassian.net/browse/DDF-1642
                        // this works around an issue in json-simple where the .toString() of an array
                        // is returned in the arguments field of configs with array attributes,
                        // causing the JSON string from jolokia to be unparseable, so we remove it,
                        // since we don't care about the arguments for our parsing needs
                        response = response.replace(/\[L[\w\.;@]*/g, '""');
                        jsonResult = JSON.parse(response.toString().trim());
                    }
                    var toDisable = existingSource.get('currentConfiguration');
                    if (toDisable && service !== toDisable) {
                        // Since the source we are editing has a currentConfig already, we don't want to disable it.
                        // Using the response from the creation of the new service, disable the newly created service
                        // with a call to makeDisableCallByPid.
                        $.when(Service.Configuration.prototype.makeDisableCallByPid(jsonResult.request['arguments'][0])).then(function() {
                            wreqr.vent.trigger('refreshSources');
                            view.closeAndUnbind();
                        });
                        needsRefresh = false;
                    }
                }
                if (needsRefresh) {
                   wreqr.vent.trigger('refreshSources');
                   view.closeAndUnbind();
                }
            },
                function() {
                    wreqr.vent.trigger('refreshSources');
                }).always(function() {
                    view.closeAndUnbind();
                });
            }
        },
        sourceNameChanged: function(evt) {
            var newName = this.$(evt.currentTarget).find('input').val().trim();
            this.checkName(newName);
        },
        checkName: function(newName) {
            var view = this;
            var model = view.model;
            var config = model.get('currentConfiguration');
            var disConfigs = model.get('disabledConfigurations');

            if (newName === '') {
                view.showError('A source must have a name.');
            } else if (newName !== model.get('name')) {
                if (view.nameIsValid(newName, model.get('editConfig').get('fpid'))) {
                    this.setConfigName(config, newName);
                    if (!_.isUndefined(disConfigs)) {
                        disConfigs.each(function(cfg) {
                            view.setConfigName(cfg, newName);
                        });
                    }
                    view.clearError();
                } else {
                    view.showError('A configuration with the name "' + newName + '" already exists. Please choose another name.');
                }
            } else {
                //model name was reverted back to original value
                view.clearError();
            }
        },
        showError: function(msg) {
            var view = this;
            var $group = view.$el.find('.sourceName>.control-group');

            $group.find('.error-text').text(msg).show();
            view.$el.find('.submit-button').attr('disabled','disabled');
            $group.addClass('has-error');
        },
        clearError: function() {
            var view = this;
            var $group = view.$el.find('.sourceName>.control-group');
            var $error = $group.find('.error-text');

            view.$el.find('.submit-button').removeAttr('disabled');
            $group.removeClass('has-error');
            $error.hide();
        },
        setConfigName: function(config, name) {
            if (!_.isUndefined(config)) {
                var properties =  config.get('properties');
                properties.set({
                       'shortname': name,
                       'id': name
                });

            }
        },
        /**
         * Returns true if any of the existing source configurations have a name matching the one provided and false otherwise.
         */
        nameExists: function(name) {
            var configs = this.parentModel.get('collection');
            var match = configs.find(function(sourceConfig) {
                    return sourceConfig.get('name') === name;
                }); 
            return !_.isUndefined(match);
        },
        nameIsValid: function(name, fpid) {
            var valid = false;
            var configs = this.source.get('collection');
            var match = configs.find(function(sourceConfig) {
                return sourceConfig.get('name') === name;
            });
            if (_.isUndefined(match)) {
                valid = true;
            } else {
                valid = !this.fpidExists(match, fpid);
            }
            return valid;
        },
        fpidExists: function(model, fpid) {
            var modelConfig = model.get('currentConfiguration');
            var disabledConfigs = model.get('disabledConfigurations');
            var matchFound = false;

            if (!_.isUndefined(modelConfig) && (modelConfig.get('fpid') === fpid || modelConfig.get('fpid') + "_disabled" === fpid)) {
                matchFound = true;
            } else if (!_.isUndefined(disabledConfigs)) {
                matchFound = !_.isUndefined(disabledConfigs.find(function(modelDisableConfig) {
                    //check the ID property to ensure that the config we're checking exists server side
                    //otherwise assume it's a template/placeholder for filling in the default modal form data
                    if (_.isUndefined(modelDisableConfig.id)) {
                        return false;
                    } else {
                        return (modelDisableConfig.get('fpid') === fpid || modelDisableConfig.get('fpid') + "_disabled" === fpid);
                    }
                }));
            }
            return matchFound;
        },
        //should be able to remove this method when the 'shortname' is removed from existing source metatypes
        getId: function(config) {
            var properties = config.get('properties');
            return properties.get('shortname') || properties.get('id');
        },
        closeAndUnbind: function() {
            this.modelBinder.unbind();
            this.$el.modal("hide");
        },
        /**
         * unbind the model and dom during close.
         */
        onClose: function () {
            this.modelBinder.unbind();
        },
        cancel: function() {
            this.closeAndUnbind();
        },
        handleTypeChange: function(evt) {
            var view = this;
            var $select = this.$(evt.currentTarget);
            if ($select.hasClass('sourceTypesSelect')) {
                this.modelBinder.unbind();
                var config = view.findConfigFromId($select.val());
                view.model.set('editConfig', config);

                var properties = config.get('properties');
                view.checkName(view.$('.sourceName').find('input').val().trim());
                view.renderDetails(config);
                view.initRadioButtonUI(properties);
                view.rebind(properties);
            }
            view.$el.trigger('shown.bs.modal');
        },
        rebind: function (properties) {
            var $boundData = this.$el.find('.bound-controls');
            var bindings = Backbone.ModelBinder.createDefaultBindings($boundData, 'name');
            //this is done so that model binder wont watch these values. We need to handle this ourselves.
            delete bindings.value;
            this.modelBinder.bind(properties, $boundData, bindings);
        },
        findConfigFromId: function(id) {
            var model = this.model;
            var currentConfig = model.get('currentConfiguration');
            var disabledConfigs = model.get('disabledConfigurations');
            var config;

            if (!_.isUndefined(currentConfig) && currentConfig.get('fpid') === id) {
                config = currentConfig;
            } else {
                if (!_.isUndefined(disabledConfigs)) {
                    config = disabledConfigs.find(function(item) {
                        var service = item.get('service');
                        if (!_.isUndefined(service) && !_.isNull(service)) {
                            return service.get('id') === id;
                        }
                        return false;
                    });
                }
            }
            return config;
        },
        renderDetails: function (configuration) {
            var service = configuration.get('service');
            if (!_.isUndefined(service)) {
                var toDisplay = service.get('metatype').filter(function (mt) {
                    return !_.contains(['shortname', 'id'], mt.get('id'));
                });
                this.details.show(new ConfigurationEdit.ConfigurationCollection({
                    collection: new Service.MetatypeList(toDisplay),
                    service: service,
                    configuration: configuration}));
            } else {
                this.$(this.details.el).html('');
                this.$(this.buttons.el).html('');
            }
        }
    });

    return ModalSource;

});