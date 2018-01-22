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
/*global define, alert*/
define([
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    './multivalue.hbs',
    'js/CustomElements',
    'component/value/value.collection.view'
], function (Backbone, Marionette, _, $, template, CustomElements, ValueCollection) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('multivalue'),
        events: {
            'click .multivalue-add': 'addNewValue'
        },
        modelEvents: {
            'change:isEditing': 'handleEdit'
        },
        regions: {
            values: '.multivalue-values'
        },
        initialize: function(){
            this.handleMultivalue();
            this.handleEdit();
        },
        handleEdit: function () {
            this.$el.toggleClass('is-editing', this.model.get('isEditing'));
        },
        onBeforeShow: function () {
            this.values.show(ValueCollection.generateValueCollectionView(this.model));
        },
        handleMultivalue: function(){
            this.$el.toggleClass('is-multivalued', this.model.get('multivalued'));
        },
        serializeData: function(){
            return {
                numberOfValues: Object.keys(this.model.get('value')).length
            };
        },
        addNewValue: function(){
            this.values.currentView.addNewValue(this.model);
        }
    });
});