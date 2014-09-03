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

        /**
         * Initialize  the binder with the ManagedServiceFactory model.
         * @param options
         */
        initialize: function(options) {
            _.bindAll(this);
            this.parentModel = options.parentModel;
            this.modelBinder = new Backbone.ModelBinder();
        },
        onRender: function() {
            var $boundData = this.$el.find('.bound-controls');
            var currentConfig = this.model.get('currentConfiguration');

            this.$el.attr('role', "dialog");
            this.$el.attr('aria-hidden', "true");
            this.renderNameField();
            this.renderTypeDropdown();
            if (!_.isNull(this.model) && !_.isUndefined(currentConfig)) {
                this.modelBinder.bind(currentConfig.get('properties'),
                        $boundData,
                        Backbone.ModelBinder.createDefaultBindings($boundData, 'name'));
            }
        },
        /**
         * Renders editable name field.
         */
        renderNameField: function() {
            var model = this.model;
            var $sourceName = this.$(".sourceName");
            var initialName = model.get('name') || 'New Configuration';
            var data = {
                id: model.id,
                name: 'Source Name',
                defaultValue: [initialName],
                description: 'Unique identifier for all source configurations of this type.'
            };
            model.set('name', initialName);
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
            var model = this.model.get('editConfig');
            var parentModel = this.parentModel;
            if (model) {
                model.save();
                if(model.get('enabled')) {
                    parentModel.get('collection').each(function(config) {
                        if (config.get('name') === model.get('name')) {
                            config.set('enabled', false);
                            config.save(); //TODO these saves need to happen atomically
                        }
                    });
                }
            }
            this.closeAndUnbind();
        },
        sourceNameChanged: function(evt) {
            var newName = $(evt.currentTarget).find('input').val().trim();
            this.checkName(newName);
        },
        checkName: function(newName) {
            var view = this;
            var model = view.model;
            var config = model.get('currentConfiguration');
            var disConfigs = model.get('disabledConfigurations');

            if (newName === '') {
                view.showError('A configuration must have a name.');
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
                properties.set('shortname', name);
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
            var configs = this.parentModel.get('collection');
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

            if (!_.isUndefined(modelConfig) && modelConfig.get('fpid') === fpid) {
                matchFound = true;
            } else if (!_.isUndefined(disabledConfigs)) {
                matchFound = !_.isUndefined(disabledConfigs.find(function(modelConfig) {
                    return modelConfig.get('fpid') === fpid;
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
            var $boundData = view.$el.find('.bound-controls');
            var $select = $(evt.currentTarget);
            if ($select.hasClass('sourceTypesSelect')) {
                this.modelBinder.unbind();
                var config = view.findConfigFromId($select.val());
                view.model.set('editConfig', config);

                var properties = config.get('properties');
                view.checkName(view.$('.sourceName').find('input').val().trim());
                view.renderDetails(config, config.get('service'));
                view.modelBinder.bind(properties, $boundData,
                      Backbone.ModelBinder.createDefaultBindings($boundData, 'name'));
            }
        },
        findConfigFromId: function(id) {
            var model = this.model;
            var currentConfig = model.get('currentConfiguration');
            var disabledConfigs = model.get('disabledConfigurations');
            var config = null;

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
        renderDetails: function(model, configuration) {
            var toDisplay = configuration.get('metatype').filter(function(mt) {
                return !_.contains(['shortname', 'id', 'parameters'], mt.get('id'));
            });
            this.details.show(new ConfigurationEdit.ConfigurationCollection({
                collection: new Service.MetatypeList(toDisplay),
                service: configuration,
                configuration: this.model}));
        }
    });

    return ModalSource;

});