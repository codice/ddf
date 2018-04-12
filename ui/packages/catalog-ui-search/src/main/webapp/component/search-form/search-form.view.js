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
/*global require*/
const Marionette = require('marionette');
const $ = require('jquery');
const template = require('./search-form.hbs');
const CustomElements = require('js/CustomElements');
const user = require('../singletons/user-instance');
const DropdownModel = require('../dropdown/dropdown');
const SearchFormInteractionsDropdownView = require('../dropdown/search-form-interactions/dropdown.search-form-interactions.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('search-form'),
    className: 'is-button',
    events: {
        'click': 'changeView'
    },
    regions: {
        workspaceActions: '.choice-actions'
    },
    onRender: function() {
        if (this.model.get('type') === 'basic' || this.model.get('type') === 'text' || this.model.get('type') === 'new-form') {
            this.$el.addClass('is-static');
        }
        else {
            this.workspaceActions.show(new SearchFormInteractionsDropdownView({
                model: new DropdownModel(),
                modelForComponent: this.model,
                collectionWrapperModel: this.options.collectionWrapperModel,
                dropdownCompanionBehaviors: {
                    navigation: {}
                }
            }));
        }
    },
    changeView: function() {
        switch(this.model.get('type')) {
            case 'new-form':
                this.options.queryModel.set('type', 'new-form');
                user.getQuerySettings().set('type', 'new-form');
                break;
            case 'basic':
                this.options.queryModel.set('type', 'basic');
                user.getQuerySettings().set('type', 'basic');
                break;
            case 'text':
                this.options.queryModel.set('type', 'text');
                user.getQuerySettings().set('type', 'text');
                break;
            case 'custom':
                var oldType = this.options.queryModel.get('type');
                this.options.queryModel.set({
                    type: 'custom',
                    title: this.model.get('name'),
                    filterTree: this.model.toJSON(),
                    modelId: this.model.get('id'),
                    accessGroups: this.model.get('accessGroups'),
                    accessIndividuals: this.model.get('accessIndividuals')
                });

                // user.getQuerySettings().set({
                //     type: 'custom',
                //     title: this.model.get('name'),
                //     filterTree: this.model.toJSON()
                // });

                this.options.queryModel.set({
                    type: 'custom',
                    title: this.model.get('name')
                });

                if (oldType === 'custom') {
                    this.options.queryModel.trigger('change:type');
                }

                break;
        }

        user.savePreferences();
        this.triggerCloseDropdown();
    },
    triggerCloseDropdown: function() {
        this.$el.trigger('closeDropdown.' + CustomElements.getNamespace());
        this.options.queryModel.trigger('closeDropDown');
    }
});
