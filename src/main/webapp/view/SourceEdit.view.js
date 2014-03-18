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

    ich.addTemplate('sourceEdit', require('text!/sources/templates/editSource.handlebars'));
    //these templates are part of the admin ui and we expect them to be there
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

    var SourceEdit = {};

    SourceEdit.View = Marionette.ItemView.extend({
        template: 'sourceEdit',
        tagName: 'div',
        className: 'modal',
        /**
         * Button events, right now there's a submit button
         * I do not know where to go with the cancel button.
         */
        events: {
            "click .submit-button": "submitData",
            "click .cancel-button": "cancel"
        },

        /**
         * Initialize  the binder with the ManagedServiceFactory model.
         * @param options
         */
        initialize: function() {
            _.bindAll(this);
            this.modelBinder = new Backbone.ModelBinder();
        },
        onRender: function() {
            this.$el.attr('tabindex', "-1");
            this.$el.attr('role', "dialog");
            this.$el.attr('aria-hidden', "true");
            this.renderTypeDropdown();
            this.renderDynamicFields();
            this.setupPopOvers();
            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model.get('currentConfiguration').get('properties'), this.$el, bindings);
        },
        /**
         * Renders the type dropdown box
         */
        renderTypeDropdown: function() {
            this.$(".sourceTypesSelect").append(ich.optionListType({"list": this.model.get('currentConfiguration').toJSON()}));

            //set the selected type so the page is rendered correctly if we are editing
            //see if the source has an id, if it does, we are editing
            if(this.model.get('currentConfiguration').get('id'))
            {
                //if this doesn't have an fpid it isn't a managed service factory
                //if it isn't a managed service factory then we can't select anything in the drop down
                if(this.model.get('currentConfiguration').get("fpid"))
                {
                    this.$(".sourceTypesSelect").val(this.model.get('currentConfiguration').get("fpid"));
                }
            }
        },

        /**
         * Walk the collection of metatypes
         * Setup the ui based on the type
         * Append it to the bottom of this data-section selector
         */
        renderDynamicFields: function() {
            var view = this;
            //view.$(".data-section").append(ich.checkboxEnableType(view.managedServiceFactory.toJSON()));

            view.model.get('currentConfiguration').get('service').get('metatype').forEach(function(each) {
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
            this.model.get('currentConfiguration').save();
        },
        /**
         * unbind the model and dom during close.
         */
        onClose: function () {
            this.modelBinder.unbind();
        },
        cancel: function() {
            //TODO discard changes somehow
        },
        /**
         * Set up the popovers based on if the selector has a description.
         */
        setupPopOvers: function() {
            var view = this;
            view.model.get('currentConfiguration').get('service').get('metatype').forEach(function(each) {
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

    return SourceEdit;

});