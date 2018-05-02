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
/*global define, setTimeout*/
define([
    'marionette',
    'backbone',
    'underscore',
    'jquery',
    './query-settings.hbs',
    'js/CustomElements',
    'js/store',
    'component/dropdown/dropdown',
    'component/dropdown/query-src/dropdown.query-src.view',
    'component/property/property.view',
    'component/property/property',
    'component/singletons/user-instance',
    'component/sort/sort.view',
    'js/Common',
    'component/result-form/result-form'
], function (Marionette, Backbone, _, $, template, CustomElements, store, DropdownModel,
             QuerySrcView, PropertyView, Property, user, SortItemCollectionView, Common, ResultForm) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-settings'),
        modelEvents: {},
        events: {
            'click .editor-edit': 'turnOnEditing',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save',
            'click .editor-saveRun': 'run'
        },
        regions: {
            settingsSortField: '.settings-sorting-field',
            settingsSrc: '.settings-src',
            resultForm: '.result-form'
        },
        ui: {},
        focus: function () {
        },
        initialize: function () {
            this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
            this.listenTo(this.model, 'change:sortField change:sortOrder change:src change:federation', Common.safeCallback(this.onBeforeShow));
        },
        onBeforeShow: function () {
            this.setupSortFieldDropdown();
            this.turnOnEditing();

            if (ResultForm.getResultTemplatesProperties()) {
                let detailLevelProperty = new Property({
                    label: 'Detail Level',
                    enum: ResultForm.getResultTemplatesProperties(),
                    id: 'Detail Level'
                });
                this.listenTo(detailLevelProperty, 'change:value', this.handleChangeDetailLevel);
                this.resultForm.show(new PropertyView({
                    model: detailLevelProperty
                }));
                this.resultForm.currentView.turnOnEditing();
            }
        },
        handleChangeDetailLevel: function (model, values) {
            $.each(model.get('enum'), (function (index, value) {
                if (values[0] === value.value) {
                    this.model.set('selectedResultTemplate', value);
                }
            }).bind(this));
        },
        onRender: function () {
            this.setupSrcDropdown();
        },
        setupSortFieldDropdown: function () {
            this.settingsSortField.show(new SortItemCollectionView({
                collection: new Backbone.Collection(this.model.get('sorts')),
                showBestTextOption: true
            }));
        },
        setupSrcDropdown: function () {
            var sources = this.model.get('src');
            this._srcDropdownModel = new DropdownModel({
                value: sources ? sources : [],
                federation: this.model.get('federation')
            });
            this.settingsSrc.show(new QuerySrcView({
                model: this._srcDropdownModel
            }));
            this.settingsSrc.currentView.turnOffEditing();
        },
        turnOffEditing: function () {
            this.$el.removeClass('is-editing');
            this.regionManager.forEach(function (region) {
                if (region.currentView && region.currentView.turnOffEditing) {
                    region.currentView.turnOffEditing();
                }
            });
        },
        turnOnEditing: function () {
            this.$el.addClass('is-editing');
            this.regionManager.forEach(function (region) {
                if (region.currentView && region.currentView.turnOnEditing) {
                    region.currentView.turnOnEditing();
                }
            });
            this.focus();
        },
        cancel: function () {
            this.$el.removeClass('is-editing');
            this.onBeforeShow();
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        },
        toJSON: function () {
            var federation = this._srcDropdownModel.get('federation');
            var src;
            if (federation === 'selected') {
                src = this._srcDropdownModel.get('value');
                if (src === undefined || src.length === 0) {
                    federation = 'local';
                }
            }
            var sorts = this.settingsSortField.currentView.collection.toJSON();
            return {
                src: src,
                federation: federation,
                sorts: sorts
            };
        },
        saveToModel: function () {
            this.model.set(this.toJSON());
        },
        save: function () {
            this.saveToModel();
            this.cancel();
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        },
        run: function () {
            this.saveToModel();
            this.cancel();
            this.model.startSearch();
            store.setCurrentQuery(this.model);
            this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        }
    });
});
