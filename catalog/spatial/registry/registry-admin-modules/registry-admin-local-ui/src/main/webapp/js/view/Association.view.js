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
        'text!templates/associationList.handlebars',
        'text!templates/associationRow.handlebars'

    ],
    function (ich, Marionette, Backbone, wreqr, _, $,  associationList, associationRow) {

        ich.addTemplate('associationList', associationList);
        ich.addTemplate('associationRow', associationRow);


        var Association = {};

        Association.AssociationView = Marionette.ItemView.extend({
            template: 'associationRow',
            tagName: 'tr',
            events: {
                "click .remove-association": 'removeAssociation'
            },
            removeAssociation: function () {
                wreqr.vent.trigger('removeAssociation:' + this.model.get('sourceId'), this.model.get('id'));
            }
        });
        Association.AssociationCollectionView = Marionette.CompositeView.extend({
            template: 'associationList',
            itemView: Association.AssociationView,
            itemViewContainer: ".association-list",
            events: {
                "click .add-association": 'addAssociation'
            },
            initialize: function (options) {
                this.parentId = options.parentId;
                this.simpleId = this.parentId.split(':').join('-');
                this.listenTo(wreqr.vent, 'removeAssociation:' + this.parentId, this.removeAssociation);
                this.listenTo(wreqr.vent, 'associationSegmentRemoved', this.updateAssociations);
                this.listenTo(wreqr.vent, 'associationSegmentAdded', this.updateAssociations);
                this.listenTo(this.collection, 'add', this.render);
                this.listenTo(this.collection, 'remove', this.render);
            },
            serializeData: function () {
                var data = {};
                data.availableAssociation = this.model.getAvailableAssociationSegments(this.parentId);
                data.simpleId = this.simpleId;
                return data;
            },
            removeAssociation: function (associationId) {
                this.collection.remove(this.model.removeAssociation(associationId));
            },
            addAssociation: function () {
                var selectedOption = this.$('.association-selector').find(':selected');
                if (selectedOption.length === 1) {
                    var addedItem = this.model.addAssociation(this.parentId, selectedOption.attr('name'), 'AssociatedWith');
                    this.collection.add(addedItem);
                    this.render();
                }
            },
            updateAssociations: function (id) {
                var seg = _.find(this.collection.models, function (model) {
                    return model.get('sourceId') === id || model.get('targetId') === id;
                });
                if (seg) {
                    this.collection.remove(seg);
                }
            }
        });

        return Association;

    });