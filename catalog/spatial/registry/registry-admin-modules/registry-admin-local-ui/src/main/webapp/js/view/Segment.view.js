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
        'js/view/CustomizableField.view.js',
        'js/view/Association.view.js',
        'js/model/FieldDescriptors.js',
        'text!templates/segmentRow.handlebars',
        'text!templates/segmentList.handlebars'

    ],
    function (ich, Marionette, Backbone, wreqr, _, $, Field, Customizable, Association, FieldDescriptors,  segmentRow, segmentList) {
        
        ich.addTemplate('segmentRow', segmentRow);
        ich.addTemplate('segmentList', segmentList);

        var Segment = {};

        Segment.SegmentView = Marionette.Layout.extend({
            template: 'segmentRow',
            regions: {
                formFields: '.form-fields',
                formSegments: '.form-segments'
            },
            events: {
                "click .remove-segment": 'removeSegment',
                "click .add-segment": 'addSegment'
            },
            initialize: function () {
                this.listenTo(wreqr.vent, 'fieldChanged:' + this.model.get('segmentId'), this.updateTitle);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.get('segmentId'), this.updateTitleError);
                this.listenTo(wreqr.vent, 'valueAdded:' + this.model.get('segmentId'), this.setupPopOvers);
                this.listenTo(wreqr.vent, 'valueRemoved:' + this.model.get('segmentId'), this.setupPopOvers);
                this.addRegions({
                    associations: '#associations-' + this.model.get('simpleId'),
                    customizable: '#customizable-' + this.model.get('simpleId')
                });
                this.events['click .'+this.model.get('simpleId')] = this.toggleCheveron;
                this.delegateEvents();
            },
            onRender: function () {
                var knownFields = [];
                var allFields = this.model.get('fields').models;
                _.each(allFields, function (field) {
                    if (!field.get('custom')) {
                        knownFields.push(field);
                    }
                });
                this.formFields.show(new Field.FieldCollectionView({collection: new Backbone.Collection(knownFields)}));
                this.formSegments.show(new Segment.SegmentCollectionView({
                    model: this.model,
                    collection: this.model.get('segments'),
                    showHeader: knownFields.length > 0 ? false : true
                }));
                if (FieldDescriptors.isCustomizableSegment(this.model.get('segmentType'))) {
                    this.customizable.show(new Customizable.CustomizableCollectionView({
                        model: this.model
                    }));
                    if (this.model.get('fields').models.length > 0) {
                        var associationModel = this.model.get('associationModel');
                        this.associations.show(new Association.AssociationCollectionView({
                            parentId: this.model.get("segmentId"),
                            model: associationModel,
                            collection: new Backbone.Collection(associationModel.getAssociationsForId(this.model.get('segmentId')))
                        }));
                    }
                }
                this.setupPopOvers();
            },
            removeSegment: function () {
                wreqr.vent.trigger('removeSegment:' + this.model.get('parentId'), this.model.get('segmentId'));
            },
            addSegment: function () {
                this.model.addSegment();
                this.render();
            },
            updateTitle: function () {
                if (!this.model.get('multiValued') && this.model.constructTitle) {
                    var title = this.$('.segment-title-'+this.model.get('simpleId'));
                    title.html(this.model.constructTitle());
                }
            },
            updateTitleError: function () {
                var title = this.$('.segment-title-error-'+this.model.get('simpleId'));
                if(this.model.validate()){
                    title.show();
                } else {
                    title.hide();
                }
                wreqr.vent.trigger('fieldErrorChange:' + this.model.get('parentId'));
            },
            serializeData: function () {
                var data = this.model.toJSON();
                if (!this.model.get('multiValued') && this.model.constructTitle) {
                    data.title = this.model.constructTitle();
                } else {
                    data.title = this.model.get('segmentName');
                }
                data.errors = this.model.validationError;
                data.customizable = FieldDescriptors.isCustomizableSegment(this.model.get('segmentType')) && !this.model.get('multiValued');
                data.showFields = this.model.get('fields').models.length > 0;
                data.showAssociations = FieldDescriptors.isCustomizableSegment(this.model.get('segmentType')) && this.model.get('fields').models.length > 0;
                return data;
            },
            toggleCheveron: function() {
                this.$el.find(".chevron-"+this.model.get('simpleId')).toggleClass('fa-chevron-down fa-chevron-right');
            },
            /**
             * Set up the popovers based on if the selector has a description.
             */
            setupPopOvers: function () {
                var view = this;
                this.model.get('fields').each(function (each) {
                    if (!_.isUndefined(each.get("desc"))) {
                        var options,
                            selector = ".description[data-title='" + each.get('key') + "']";
                        options = {
                            title: each.get("name"),
                            content: each.get("desc"),
                            trigger: 'hover'
                        };
                        view.$(selector).popover(options);
                    }
                });
            }
        });

        Segment.SegmentCollectionView = Marionette.CompositeView.extend({
            template: 'segmentList',
            itemView: Segment.SegmentView,
            events: {
                "click .add-segment": 'addSegment'
            },
            modelEvents: {
                "change:segments": "render"
            },
            initialize: function () {
                this.listenTo(wreqr.vent, 'removeSegment:' + this.model.get('segmentId'), this.removeSegment);
            },
            buildItemView: function (item, ItemViewType, itemViewOptions) {
                item.set('editableSegment', this.model.get('multiValued'));
                var options = _.extend({
                    model: item
                }, itemViewOptions);
                return new ItemViewType(options);
            },
            addSegment: function () {
                var selector = this.$('.auto-populate-selector');
                var prePopulateId;
                if (selector) {
                    var selected = selector.find(':selected');
                    if (selected.attr('id') !== 'empty') {
                        prePopulateId = selected.attr('id');
                    }
                }
                this.model.addSegment(prePopulateId);
                this.render();
            },
            removeSegment: function (id) {
                this.model.removeSegment(id);
            },
            serializeData: function () {
                var data = {};

                if (this.model) {
                    data = this.model.toJSON();
                }
                var autoValues = FieldDescriptors.autoPopulateValues[this.model.get('segmentType')];
                data.autoValues = autoValues;
                data.showHeader = this.options.showHeader;
                data.segmentName = this.model.get('segmentName');
                return data;
            }
        });

        return Segment;

    });