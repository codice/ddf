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
const Marionette = require('marionette')
const Backbone = require('backbone')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./query-settings.hbs')
const CustomElements = require('js/CustomElements')
const store = require('js/store')
const DropdownModel = require('component/dropdown/dropdown')
const QuerySrcView = require('component/dropdown/query-src/dropdown.query-src.view')
const PropertyView = require('component/property/property.view')
const Property = require('component/property/property')
const SortItemCollectionView = require('component/sort/sort.view')
const Common = require('js/Common')
const properties = require('properties')
const plugin = require('plugins/query-settings')
const ResultForm = properties.hasExperimentalEnabled() ? require('component/result-form/result-form') : {}

module.exports = plugin(Marionette.LayoutView.extend({
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
            resultForm: '.result-form',
            extensions: '.query-extensions'
        },
        ui: {},
        focus: function () {
        },
        initialize: function () {
            this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
            this.listenTo(this.model, 'change:sortField change:sortOrder change:src change:federation', Common.safeCallback(this.onBeforeShow));
            if(properties.hasExperimentalEnabled())
            {
                this.resultFormCollection = ResultForm.getResultCollection();
                this.listenTo(this.resultFormCollection, 'change:added', this.handleFormUpdate)
            }
        },
        handleFormUpdate: function(newForm) {
            this.renderResultForms(this.resultFormCollection.filteredList)
        },
        onBeforeShow: function () {
            this.setupSortFieldDropdown();
            this.setupSrcDropdown();
            this.turnOnEditing();
            if(properties.hasExperimentalEnabled())
            {
                this.renderResultForms(this.resultFormCollection.filteredList)
            }
        },
        renderResultForms: function(resultTemplates){
            resultTemplates = resultTemplates ? resultTemplates : []
            resultTemplates.push({
                label: 'All Fields',
                value: 'allFields',
                id: 'All Fields',
                descriptors: [],
                description: 'All Fields'
            });
            resultTemplates =  _.uniq(resultTemplates, 'id');
            let lastIndex = resultTemplates.length - 1;
                let detailLevelProperty = new Property({
                label: 'Result Form',
                enum: resultTemplates,
                value: [this.model.get('detail-level') || (resultTemplates && resultTemplates[lastIndex] && resultTemplates[lastIndex].value)],
                showValidationIssues: false,
                id: 'Result Form'
                });
                this.listenTo(detailLevelProperty, 'change:value', this.handleChangeDetailLevel);
                this.resultForm.show(new PropertyView({
                    model: detailLevelProperty
                }));
                this.resultForm.currentView.turnOnEditing();

            const extensions = this.getExtensions()
            if (extensions !== undefined) {
                this.extensions.show(extensions)
            }
        },
        getExtensions: function () {},
        handleChangeDetailLevel: function (model, values) {
            $.each(model.get('enum'), (function (index, value) {
                if (values[0] === value.value) {
                    this.model.set('detail-level', value);
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
            this.regionManager.forEach(function(region){
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
            let detailLevel = this.resultForm.currentView && this.resultForm.currentView.model.get('value')[0]
            if (detailLevel && detailLevel === 'allFields') {
                detailLevel = undefined;
            }
            return {
                src: src,
                federation: federation,
                sorts: sorts,
                'detail-level': detailLevel
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
    }));
