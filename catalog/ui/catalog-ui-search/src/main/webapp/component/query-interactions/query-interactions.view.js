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
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    'text!./query-interactions.hbs',
    'js/CustomElements',
    'js/store'
], function (wreqr, Marionette, _, $, template, CustomElements, store) {

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('query-interactions'),
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .interaction-run': 'handleRun',
            'click .interaction-delete': 'handleDelete',
            'click .interaction-duplicate': 'handleDuplicate',
            'click': 'handleClick'
        },
        ui: {
        },
        initialize: function(){
        },
        onRender: function(){
        },
        handleRun: function(){
            this.model.startSearch();
        },
        handleDelete: function(){
            this.model.collection.remove(this.model);
        },
        handleDuplicate: function(){
            if (this.model.collection.canAddQuery()){
                var copyAttributes = JSON.parse(JSON.stringify(this.model.attributes));
                delete copyAttributes.id;
                delete copyAttributes.result;
                var newQuery = new this.model.constructor(copyAttributes);
                store.setQueryByReference(newQuery);
            }
        },
        handleClick: function(){
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        }
    });
});
