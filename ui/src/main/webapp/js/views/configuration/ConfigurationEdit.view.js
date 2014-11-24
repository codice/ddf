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
    'marionette',
    'icanhaz',
    'underscore',
    'backbone',
    'js/wreqr.js',
    'text!templates/configuration/configurationEdit.handlebars',
    'text!templates/configuration/configurationItem.handlebars',
    'text!templates/configuration/textTypeListHeader.handlebars',
    'text!templates/configuration/textTypeList.handlebars'
    ], function (Marionette, ich, _, Backbone, wreqr, configurationEdit, configurationItem, textTypeListHeader, textTypeList ) {
	
    var ConfigurationEditView = {};

    if(!ich['configuration.configurationItem']) {
        ich.addTemplate('configuration.configurationItem', configurationItem);
    }
    if(!ich['configuration.configurationEdit']) {
        ich.addTemplate('configuration.configurationEdit', configurationEdit);
    }
    if(!ich['configuration.textTypeListHeader']) {
        ich.addTemplate('configuration.textTypeListHeader', textTypeListHeader);
    }
    if(!ich['configuration.textTypeList']) {
        ich.addTemplate('configuration.textTypeList', textTypeList);
    }

    ConfigurationEditView.ConfigurationMultiValuedEntry = Marionette.ItemView.extend({
        template: 'configuration.textTypeList',
        tagName: 'tr',
        initialize: function() {
            this.modelBinder = new Backbone.ModelBinder();
        },
        events: {
            "click .minus-button": "minusButton"
        },
        minusButton: function() {
            this.model.collection.remove(this.model);
        },
        onRender: function() {
            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model, this.$el, bindings);
        },
        onClose: function() {
            this.modelBinder.unbind();
        }
    });

    ConfigurationEditView.ConfigurationMultiValueCollection = Marionette.CollectionView.extend({
        itemView: ConfigurationEditView.ConfigurationMultiValuedEntry,
        tagName: 'table'
    });

    ConfigurationEditView.ConfigurationMultiValuedItem = Marionette.Layout.extend({
        template: 'configuration.textTypeListHeader',
        itemView: ConfigurationEditView.ConfigurationMultiValueCollection,
        tagName: 'div',
        regions: {
            listItems: '#listItems'
        },
        /**
         * Button events, right now there's a submit button
         * I do not know where to go with the cancel button.
         */
        events: {
            "click .plus-button": "plusButton"
        },
        modelEvents: {
            "change": "updateValues"
        },        
        initialize: function(options) {
            _.bindAll(this);
            this.configuration = options.configuration;
            this.collectionArray = new Backbone.Collection();
            this.listenTo(wreqr.vent, 'refresh', this.updateValues);
            this.listenTo(wreqr.vent, 'beforesave', this.saveValues);
        },
        updateValues: function() {
            var csvVal, view = this;
            if(this.configuration.get('properties') && this.configuration.get('properties').get(this.model.get('id'))) {
                csvVal = this.configuration.get('properties').get(this.model.get('id'));
            } else {
                csvVal = this.model.get('defaultValue');
            }
            this.collectionArray.reset();
            if(csvVal && csvVal !== '') {
                if(_.isArray(csvVal)) {
                    _.each(csvVal, function (item) {
                        view.addItem(item);
                    });
                } else {
                    _.each(csvVal.split(/[,]+/), function (item) {
                        view.addItem(item);
                    });
                }
            }
        },
        saveValues: function() {
            var values = [];
            _.each(this.collectionArray.models, function(model) {
                values.push(model.get('value'));                
            });
            this.configuration.get('properties').set(this.model.get('id'), values.join());
        },
        onRender: function() {
            this.listItems.show(new ConfigurationEditView.ConfigurationMultiValueCollection({
                collection: this.collectionArray
            }));

            this.updateValues();
        },
        addItem: function(value) {
            this.collectionArray.add(new Backbone.Model({value: value}));
        },
        /**
         * Creates a new text field for the properties collection.
         */
        plusButton: function() {
            this.addItem('');
        }        
    });

    ConfigurationEditView.ConfigurationItem = Marionette.ItemView.extend({
        template: 'configuration.configurationItem'
    });

    ConfigurationEditView.ConfigurationCollection = Marionette.CollectionView.extend({
        itemView: ConfigurationEditView.ConfigurationItem,
        buildItemView: function(item, ItemViewType, itemViewOptions){
            var view;
            var configuration = this.options.configuration;
            this.collection.forEach(function(property) {
                if(item.get('id') === property.id) {
                    if(property.description) {
                        item.set({ 'description': property.description });
                    }
                    if(property.note) {
                        item.set({ 'note': property.note});
                    }
                    var options = _.extend({model: item, configuration: configuration}, itemViewOptions);

                    view = new ItemViewType(options);
                }
            });
            if(view) {
                return view;
            }
            return new Marionette.ItemView();
        },
        getItemView: function( item ) {
            if(item.get('cardinality') > 0 || item.get('cardinality') < 0) {
                return ConfigurationEditView.ConfigurationMultiValuedItem;
            } else {
                return ConfigurationEditView.ConfigurationItem;
            }
        }
    });

    ConfigurationEditView.View = Marionette.Layout.extend({
        template: 'configuration.configurationEdit',
        tagName: 'div',
        className: 'modal-dialog',
        regions: {
            configurationItems: '#config-div'
        },        
        /**
         * Button events, right now there's a submit button
         * I do not know where to go with the cancel button.
         */
        events: {
            "click .submit-button": "submitData",
            "click .cancel-button": "cancel",
            "click .enable-checkbox" : "toggleEnable",
            "change .sourceTypesSelect" : "render"
        },
        
        /**
         * Initialize  the binder with the ManagedServiceFactory model.
         * @param options
         */
        initialize: function(options) {
            _.bindAll(this);
            this.modelBinder = new Backbone.ModelBinder();
            this.service = options.service;
            this.listenTo(wreqr.vent, 'sync', this.bind);
        },

        serializeData: function() {
            var data = this.model.toJSON();
            data.service = this.service.toJSON();
            return data;
        },

        onRender: function() {
            this.configurationItems.show(new ConfigurationEditView.ConfigurationCollection({
                collection: this.service.get('metatype'),
                configuration: this.model}));
            this.bind();
            this.setupPopOvers();
        },

        bind: function() {
            var view = this;
            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            //this is done so that model binder wont watch these values. We need to handle this ourselves.
            delete bindings.value;
            var bindObjs = _.values(bindings);
            _.each(bindObjs, function(value) {
                if(view.$(value.selector).attr('type') === 'checkbox') {
                    value.converter = function(direction, bindValue) {
                        switch(direction) {
                            case 'ViewToModel':
                                return bindValue.toString();
                            case 'ModelToView':

                                if(bindValue && bindValue !== ""){
                                    var bindValueString = "" + bindValue;
                                    return JSON.parse(bindValueString.toLowerCase());
                                }
                                return null;  // TODO determine if this is correct

                        }
                    };
                }
            });
            this.modelBinder.bind(this.model.get('properties'), this.$el, bindings);
        },
        /**
         * Submit to the backend.
         */
        submitData: function() {
            wreqr.vent.trigger('beforesave');
            if(this.service) {
                if (!this.model.get('properties').has('service.pid')) {
                    this.model.get('properties').set('service.pid', this.service.get('id'));
                }
            }
            this.model.save();
            wreqr.vent.trigger('sync');
            wreqr.vent.trigger('poller:start');

            var view = this;
            _.defer(function() {
                view.close();
                wreqr.vent.trigger('refreshConfigurations');
            });
        },
        /**
         * unbind the model and dom during close.
         */
        onClose: function () {
            this.modelBinder.unbind();
        },
        cancel: function() {
            wreqr.vent.trigger('poller:start');
            var view = this;
            _.defer(function() {
                view.close();
            });
        },
        /**
         * Set up the popovers based on if the selector has a description.
         */
        setupPopOvers: function() {
            var view = this;
            this.service.get('metatype').forEach(function(each) {
                if(!_.isUndefined(each.get("description"))) {
                   var options,
                        selector = ".description[data-title='" + each.id + "']";
                    options = {
                        title: each.get("name"),
                        content: each.get("description"),
                        trigger: 'hover'
                    };
                    view.$(selector).popover(options);
                }
            });
        },
        /**
         * This will set the defaults and set values for properties with cardinality of nonzero
         */
        
        refresh: function() {
            wreqr.vent.trigger('refreshConfigurations');
        }
    });

    return ConfigurationEditView;
});