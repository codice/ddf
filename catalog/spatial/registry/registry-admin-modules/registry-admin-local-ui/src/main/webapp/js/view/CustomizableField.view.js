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
define([
        'icanhaz',
        'marionette',
        'backbone',
        'wreqr',
        'underscore',
        'jquery',
        'js/view/Field.view.js',
        'text!templates/customFieldList.handlebars'
    ],
    function (ich, Marionette, Backbone, wreqr, _, $, Field, customList) {

        ich.addTemplate('customList', customList);

        var Customizable = {};

        Customizable.CustomizableCollectionView = Marionette.Layout.extend({
            template: 'customList',
            regions: {
                customFieldsRegion: '#custom-fields'
            },
            events: {
                "click .add-custom-field": 'addField'
            },
            initialize: function (options) {
                this.readOnly = options.readOnly;
                this.listenTo(wreqr.vent, 'removeField:' + this.model.get('segmentId'), this.removeField);
            },
            onRender: function () {
                var customFields = this.model.get('fields').models
                    .filter(function (field) {
                        return field.get('custom');
                    });
                this.customFieldsRegion.show(new Field.FieldCollectionView({
                    collection: new Backbone.Collection(customFields),
                    parentId: this.model.get("segmentId"),
                    readOnly: this.readOnly
                }));
            },
            addField: function (event) {
                event.stopImmediatePropagation();
                var fieldName = this.$('.field-name').val();
                if (!fieldName || fieldName.trim().length === 0) {
                    return;
                }
                var fieldType = this.$('.field-type-selector').val();
                var addedField = this.model.addField(fieldName, fieldType);
                this.$('.field-name').val('');
                if (addedField) {
                    if (this.customFieldError) {
                        this.clearCustomFieldError();
                    } else {
                        wreqr.vent.trigger('addedField:' + this.model.get("segmentId"), addedField);
                    }
                } else {
                    this.showCustomFieldError(fieldName);
                }
            },
            removeField: function (key) {
                var removedField = this.model.removeField(key);
                wreqr.vent.trigger('removedField:' + this.model.get("segmentId"), removedField);
            },
            showCustomFieldError: function (fieldName) {
                this.customFieldError = 'Field \'' + fieldName + '\' already exists.';
                this.render();
            },
            clearCustomFieldError: function () {
                this.customFieldError = undefined;
                this.render();
            },
            serializeData: function () {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                }
                data.customFieldError = this.customFieldError;
                data.readOnly = this.readOnly;
                return data;
            }
        });


        return Customizable;

    });