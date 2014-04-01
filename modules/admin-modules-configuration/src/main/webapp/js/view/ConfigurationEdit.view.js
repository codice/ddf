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
define(function (require) {

    var Backbone = require('backbone'),
        Marionette = require('marionette'),
        _ = require('underscore'),
        ich = require('icanhaz');

    var ConfigurationEdit = {};
    
    var TempModel = Backbone.Model.extend({});
    var TempCollection = Backbone.Collection.extend({model: TempModel});
    
    ich.addTemplate('configurationEdit', require('text!/configurations/templates/configurationEdit.handlebars'));
    if(!ich.optionListType) {
        ich.addTemplate('optionListType', require('text!templates/optionListType.handlebars'));
    }
    if(!ich.textType) {
        ich.addTemplate('textType', require('text!templates/textType.handlebars'));
    }
    if(!ich.passwordType) {
        ich.addTemplate('passwordType', require('text!templates/passwordType.handlebars'));
    }
    if(!ich.numberType) {
        ich.addTemplate('numberType', require('text!templates/numberType.handlebars'));
    }
    if(!ich.checkboxType) {
        ich.addTemplate('checkboxType', require('text!templates/checkboxType.handlebars'));
    }
    if(!ich.textTypeListRegion) {
        ich.addTemplate('textTypeListRegion', require('text!templates/textTypeListRegion.handlebars'));
    }
    if(!ich.textTypeListHeader) {
        ich.addTemplate('textTypeListHeader', require('text!templates/textTypeListHeader.handlebars'));
    }
    if(!ich.textTypeList) {
        ich.addTemplate('textTypeList', require('text!templates/textTypeList.handlebars'));
    }

    ConfigurationEdit.ViewArrayEntry = Marionette.ItemView.extend({
        template: 'textTypeList',
        tagName: 'tr',
        events: {
            "click .minus-button": "minusButton"
        },
        initialize: function() {
            this.modelBinder = new Backbone.ModelBinder();
        },
        onRender: function() {
            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model, this.$el, bindings);
        },
        minusButton: function() {
            this.model.collection.remove(this.model);
        }
    });
    
        
    ConfigurationEdit.ViewArray = Marionette.CollectionView.extend({
        template: 'textTypeListRegion',
        itemView: ConfigurationEdit.ViewArrayEntry,
        itemViewContainer: 'tbody',
    });

    ConfigurationEdit.View = Marionette.Layout.extend({
        template: 'configurationEdit',
        tagName: 'div',
        className: 'modal-dialog',
        /**
         * Button events, right now there's a submit button
         * I do not know where to go with the cancel button.
         */
        events: {
            "click .submit-button": "submitData",
            "click .cancel-button": "cancel",
            "click .enable-checkbox" : "toggleEnable",
            "change .sourceTypesSelect" : "render",
            "click .plus-button": "plusButton"
        },
        
        /**
         * Initialize  the binder with the ManagedServiceFactory model.
         * @param options
         */
        initialize: function(options) {
            _.bindAll(this);
            this.modelBinder = new Backbone.ModelBinder();
            this.factory = options.factory;
        },

        onRender: function() {
            this.renderDynamicFields();
            this.setupPopOvers();
            this.bind();
            this.listenTo(this.model.get('service').get('response'), 'sync', this.bind);
        },

        bind: function() {
            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model.get('properties'), this.$el, bindings);
        },

        /**
         * Walk the collection of metatypes
         * Setup the ui based on the type
         * Append it to the bottom of this data-section selector
         */
        renderDynamicFields: function() {
            var view = this;
            //view.$(".data-section").append(ich.checkboxEnableType(view.managedServiceFactory.toJSON()));
            view.model.get('service').get('metatype').forEach(function(each) {
                var type = each.get("type");
                if(!_.isUndefined(type)) {
                    //from the Metatype specification
                    // int STRING = 1;
                    // int LONG = 2;
                    // int INTEGER = 3;
                    // int SHORT = 4;
                    // int CHARACTER = 5;
                    // int BYTE = 6;
                    // int DOUBLE = 7;
                    // int FLOAT = 8;
                    // int BIGINTEGER = 9;
                    // int BIGDECIMAL = 10;
                    // int BOOLEAN = 11;
                    // int PASSWORD = 12;
                    if (type === 1 || type === 5 || type === 6 || (type >= 7 && type <= 10)) {
                        if (each.get("cardinality") === 0) {
                            view.$(".data-section").append(ich.textType(each.toJSON()));
                        }
                    }
                    else if (type === 11) {
                        view.$(".data-section").append(ich.checkboxType(each.toJSON()));
                    }
                    else if (type === 12) {
                        view.$(".data-section").append(ich.passwordType(each.toJSON()));
                    }
                    else if (type === 2 || type === 3 || type === 4) { //this type can only be used for integers
                        view.$(".data-section").append(ich.numberType(each.toJSON()));
                    }
                }
            });
        },
        /**
         * Submit to the backend.
         */
        submitData: function() {
            this.setPropertyArray("newSave");
            this.model.save();
            if(this.model.get('service') && this.model.get('service').get('response')) {
                this.model.get('service').get('response').trigger('canceled');
            }
        },
        /**
         * unbind the model and dom during close.
         */
        onClose: function () {
            this.modelBinder.unbind();
            if(this.model.get('service')) {
                this.stopListening(this.model.get('service').get('response'));
            }
            this.stopListening(this.$('#' + this.model.get('uuid')));
        },
        cancel: function() {
            if(this.model.get('service') && this.model.get('service').get('response')) {
                this.model.get('service').get('response').trigger('canceled');
            }
            if(!this.model.get('properties').get('service.pid') && this.model.get('service')) {
                this.model.get('service').get('configurations').remove(this.model);
            }
            this.cancelRegion();
        },
        /**
         * Upon a cancel, it will remove the temporarily used region
         */
        cancelRegion: function() {
            var view = this;
            view.model.get('service').get('metatype').forEach(function(each) {
                this.type = each.get("type");
                if(!_.isUndefined(this.type)) {
                    if (this.type === 1 || this.type === 5 || this.type === 6 || (this.type >= 7 && this.type <= 10)) {
                        if(each.get('cardinality') !== 0) {
                            view[each.get("id")].close();
                        }
                    }
                }
            });
        },
        /**
         * Set up the popovers based on if the selector has a description.
         */
        setupPopOvers: function() {
            var view = this;
            view.model.get('service').get('metatype').forEach(function(each) {
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
            this.setPropertyArray("default");
            this.setPropertyArray();
        },
        /**
         * Properties with a nonzero cardinality will be parsed and entered into their own text boxes.
         */
        setPropertyArray: function(propertyType) {
            var view = this;
            view.model.get('service').get('metatype').forEach(function(each) {
                var type = each.get("type");
                if(!_.isUndefined(type)) {
                    if (type === 1 || type === 5 || type === 6 || (type >= 7 && type <= 10)) {
                        if(each.get('cardinality') !== 0) {
                            var valueArray = [];
                            var count = 0;
                            /**
                             * Initial load of the properties will be defaults
                             */
                            if(propertyType === "default") {
                                var defaultConfig = each.get('defaultValue');
                                if(defaultConfig) {
                                    count = 0;
                                    _.each(defaultConfig.split(/[,]+/), function(item) {
                                        var itemObj = {};
                                        itemObj.id = each.get('id') + "_" + count++;
                                        itemObj.value = item;
                                        valueArray.push(itemObj);
                                    });
                                }
                                view.collectionArray = new TempCollection(valueArray);
                                if(!view[each.get("id")]) {
                                    view.$(".data-section").append(ich.textTypeListHeader(each.toJSON()));
                                    view.addRegion(each.get("id"), "#"+each.get("id"));
                                }
                                view[each.get("id")].show(new ConfigurationEdit.ViewArray({collection: view.collectionArray}));
                            }
                            /**
                             * If the submit/save button is clicked, all current values will be stored
                             */
                            else if (propertyType === "newSave") {
                                while(view.collectionArray.at(count))
                                {
                                    var configValues = view.collectionArray.at(count).get('value');
                                    valueArray.push(configValues);
                                    count++;
                                }
                                var propertyString = valueArray.join();
                                view.model.get('properties').set(each.get('id'), propertyString);
                            }
                            /**
                             * Loads values stored in the metatype upon first load and refreshes
                             */
                            else {
                                var configurationProperties = view.model.get('properties').get(each.get('id'));
                                if(configurationProperties){
                                    if(configurationProperties instanceof Array) {
                                        var nonArray = configurationProperties.join();
                                        configurationProperties = nonArray;
                                    }
                                    count = 0;
                                    _.each(configurationProperties.split(/[,]+/), function(item) {
                                        var itemObj = {};
                                        itemObj.id = each.get('id') + "_" + count++;
                                        itemObj.value = item;
                                        valueArray.push(itemObj);
                                    });
                                }
                                view.collectionArray.set(valueArray);
                            }
                        }
                    }
                }
            });
        },
        /**
         * Creates a new text field for the properties collection.
         */
        plusButton: function() {
            var count = this.collectionArray.length;
            var id = this.collectionArray.at(0).get('id');
            var idTrimmed = id.split(/[_]+/);
            var item = {};
            item.id = [idTrimmed[0] + "_" + count];
            item.value = ["enter new value"];
            var array = [];
            array.push(item);
            this.collectionArray.add(array);
        }
    });

    return ConfigurationEdit;
});