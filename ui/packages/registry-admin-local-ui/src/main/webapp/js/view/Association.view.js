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
            initialize: function(options) {
                this.readOnly = options.readOnly;
            },
            removeAssociation: function () {
                wreqr.vent.trigger('removeAssociation:' + this.model.get('sourceId'), this.model.get('id'));
            },
            serializeData: function() {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                }
                data.readOnly = this.readOnly;

                return data;
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
                this.readOnly = options.readOnly;
                this.parentId = options.parentId;
                this.simpleId = this.parentId.split(':').join('-');
                this.listenTo(wreqr.vent, 'removeAssociation:' + this.parentId, this.removeAssociation);
                this.listenTo(this.model.get('associationSegments'),'add',this.updateAssociations);
                this.listenTo(this.model.get('associationSegments'),'remove',this.updateAssociations);
                this.listenTo(this.model.get('associations'),'add',this.render);
                this.listenTo(this.model.get('associations'),'remove',this.render);
                this.$('.description').popover();
            },
            onRender: function(){
                this.setupPopOvers();
            },
            serializeData: function () {
                var data = {};
                data.availableAssociation = this.model.getAvailableAssociationSegments(this.parentId);
                data.simpleId = this.simpleId;
                data.readOnly = this.readOnly;
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
                }
            },
            updateAssociations: function (association) {
                var seg = _.find(this.collection.models, function (model) {
                    return model.get('sourceId') ===  association.get('segmentId') || model.get('targetId') === association.get('segmentId');
                });
                if (seg) {
                    this.collection.remove(seg);
                }
                this.render();
            },
            /**
             * Set up the popovers based on if the selector has a description.
             */
            setupPopOvers: function () {
                var view = this;
                var options,
                    selector = ".description";
                options = {
                    trigger: 'hover'
                };
                view.$(selector).popover(options);
            },
            buildItemView: function (item, ItemViewType, itemViewOptions) {
                var options = _.extend({
                    model: item,
                    readOnly: this.readOnly
                }, itemViewOptions);
                return new ItemViewType(options);
            }


        });

        return Association;

    });