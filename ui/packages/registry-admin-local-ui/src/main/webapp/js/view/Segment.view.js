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
            initialize: function (options) {
                this.readOnly = options.readOnly;
                this.listenTo(wreqr.vent, 'fieldChange:' + this.model.get('segmentId'), this.updateTitle);
                this.listenTo(wreqr.vent, 'fieldErrorChange:' + this.model.get('segmentId'), this.updateTitleError);
                this.listenTo(wreqr.vent, 'valueAdded:' + this.model.get('segmentId'), this.setupPopOvers);
                this.listenTo(wreqr.vent, 'valueRemoved:' + this.model.get('segmentId'), this.setupPopOvers);
                this.addRegions({
                    associations: '#associations-' + this.model.get('simpleId'),
                    customizable: '#customizable-' + this.model.get('simpleId'),
                    formAdvancedFields: '.form-fields-advanced-' + this.model.get('simpleId')
                });
                this.events['click .'+this.model.get('simpleId')] = this.toggleCheveron;
                this.events['click .advanced-button-'+this.model.get('simpleId')] = this.toggleAdvanced;
                this.delegateEvents();

                var standardFields = [];
                var advancedFields = [];
                this.model.get('fields').forEach(function (field) {
                    field.set('identityNode', this.model.get('identityNode'));
                    if (!field.get('custom') && !field.get('advanced')) {
                        standardFields.push(field);
                    }
                    if(field.get('advanced')){
                        advancedFields.push(field);
                        this.addvancedFields = true;
                    }
                }.bind(this));
                this.formFieldsView = new Field.FieldCollectionView({collection: new Backbone.Collection(standardFields), readOnly: this.readOnly});
                this.formSegmentsView = new Segment.SegmentCollectionView({
                    model: this.model,
                    collection: this.model.get('segments'),
                    showHeader: standardFields.length > 0 ? false : true,
                    readOnly: this.readOnly
                });
                this.formAdvancedFieldsView = new Field.FieldCollectionView({collection: new Backbone.Collection(advancedFields), readOnly: this.readOnly});

                if (FieldDescriptors.isCustomizableSegment(this.model.get('segmentType'))) {
                    this.customizableView = new Customizable.CustomizableCollectionView({
                        model: this.model,
                        readOnly: this.readOnly
                    });
                    if (this.model.get('fields').models.length > 0) {
                        var associationModel = this.model.get('associationModel');
                        this.associationsView = new Association.AssociationCollectionView({
                            parentId: this.model.get("segmentId"),
                            model: associationModel,
                            collection: new Backbone.Collection(associationModel.getAssociationsForId(this.model.get('segmentId'))),
                            readOnly: this.readOnly
                        });
                    }
                }
            },
            onRender: function () {
                this.formFields.show(this.formFieldsView);
                this.formSegments.show(this.formSegmentsView);
                this.formAdvancedFields.show(this.formAdvancedFieldsView);
                
                if (FieldDescriptors.isCustomizableSegment(this.model.get('segmentType'))) {
                    this.customizable.show(this.customizableView);
                    if (this.model.get('fields').models.length > 0) {

                        this.associations.show(this.associationsView);
                    }
                }

                var errors = this.model.get('fields').find(function (field){
                    return field.get('error');
                });
                if(errors){
                    this.updateTitleError();
                }

                this.setupPopOvers();
            },
            removeSegment: function () {
                wreqr.vent.trigger('removeSegment:' + this.model.get('parentId'), this.model.get('segmentId'));
            },
            addSegment: function (event) {
                event.stopPropagation();
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
                this.setupPopOvers();
            },
            toggleAdvanced: function (evt) {
                evt.stopPropagation();
                var advancedSections = this.$('.advanced-section-' + this.model.get('simpleId'));
                var advancedButton = this.$('.advanced-button-' + this.model.get('simpleId'));
                if (this.advancedVisible) {
                    this.advancedVisible = false;
                    advancedSections.hide();
                    advancedButton[0].innerHTML = "Show Advanced";
                } else {
                    this.advancedVisible = true;
                    advancedSections.show();
                    advancedButton[0].innerHTML = "Hide Advanced";
                }
            },
            serializeData: function () {
                var data = this.model.toJSON();
                if (!this.model.get('multiValued') && this.model.constructTitle) {
                    data.title = this.model.constructTitle();
                } else {
                    data.title = this.model.get('segmentName');
                }
                data.associationsAreAdvanced = FieldDescriptors.configurations.associationsAreAdvanced;
                data.customFieldsAreAdvanced = FieldDescriptors.configurations.customFieldsAreAdvanced;
                data.advancedFields = this.advancedFields;
                data.errors = this.model.validationError;
                data.customizable = FieldDescriptors.isCustomizableSegment(this.model.get('segmentType')) && !this.model.get('multiValued');
                data.showFields = this.model.get('fields').models.length > 0;
                data.showAssociations = FieldDescriptors.isCustomizableSegment(this.model.get('segmentType')) && this.model.get('fields').models.length > 0;
                data.showAdvanced = (data.associationsAreAdvanced || data.customFieldsAreAdvanced || this.addvancedFields)&&(data.customizable);
                data.readOnly = this.readOnly;
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
            initialize: function (options) {
                this.readOnly = options.readOnly;
                this.listenTo(wreqr.vent, 'removeSegment:' + this.model.get('segmentId'), this.removeSegment);
            },
            buildItemView: function (item, ItemViewType, itemViewOptions) {
                item.set('editableSegment', this.model.get('multiValued'));
                var options = _.extend({
                    model: item,
                    readOnly: this.readOnly
                }, itemViewOptions);
                return new ItemViewType(options);
            },
            addSegment: function (event) {
                event.stopPropagation();
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
                data.readOnly = this.readOnly;
                return data;
            }
        });

        return Segment;

    });