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
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    'js/CustomElements'
], function (Backbone, Marionette, _, $, CustomElements) {

    var RowView = Marionette.LayoutView.extend({
        className: 'editable-rows-item',
        template: '<div class="embed"></div>' +
                  '<div class="remove"><button class="is-negative"><span class="fa fa-minus"></span></button></div>',
        events:  { 'click .remove': 'removeRow' },
        regions: { embed: '.embed' },
        removeRow: function () {
            this.model.destroy();
        },
        onRender: function () {
            this.embed.show(this.options.embed(this.model, this.options.embedOptions));
        }
    });

    var RowsView = Marionette.CollectionView.extend({
        childView: RowView,
        childViewOptions: function () { return this.options }
    });
    
    var JsonView = Marionette.ItemView.extend({
        tagName: 'pre',
        template: 'JSON: {{json this}}'
    });

    var EditableRowsView = Marionette.LayoutView.extend({
        tagName: CustomElements.register('editable-rows'),
        template: '<div class="rows"></div><button class="add-row is-positive"><span class="fa fa-plus"></span></button>',
        events:      { 'click .add-row': 'addRow' },
        regions:     { rows: '.rows' },
        initialize: function(){
            this.listenTo(this.collection, 'add remove update reset', this.checkEmpty);
        },
        checkEmpty: function(){
            this.$el.toggleClass('is-empty', this.collection.isEmpty());
        },
        addRow: function () {
            this.collection.add({});
        },
        embed: function (model) {
            return new JsonView({ model: model });
        },
        onRender: function () {
            this.rows.show(new RowsView({
                collection: this.collection,
                embed: this.embed,
                embedOptions: this.options
            }));
            this.checkEmpty();
        }
    });

    return EditableRowsView;
});
