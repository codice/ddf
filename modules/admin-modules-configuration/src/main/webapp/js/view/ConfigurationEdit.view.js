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

    ConfigurationEdit.View = Marionette.ItemView.extend({
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
            "change .sourceTypesSelect" : "render"
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
                //TODO re-enable this when this functionality is added back in
//                var cardinality = each.get("cardinality"); //this is ignored for now and lists will be rendered as a ',' separated list
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
                        view.$(".data-section").append(ich.textType(each.toJSON()));
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
        }
    });

    return ConfigurationEdit;
});