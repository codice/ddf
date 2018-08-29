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
const Marionette = require('marionette');
const template = require('./search-interactions.hbs');
const CustomElements = require('js/CustomElements');
const SearchSettingsDropdownView = require('component/dropdown/search-settings/dropdown.search-settings.view');
const DropdownModel = require('component/dropdown/dropdown');
const SearchFormSelectorDropdownView = require('component/dropdown/search-form-selector/dropdown.search-form-selector.view');
const ConfirmationView = require('component/confirmation/confirmation.view');
const user = require('component/singletons/user-instance');
const properties = require('properties');
const ResultFormSelectorDropdownView = properties.hasExperimentalEnabled() ? require('component/dropdown/result-form-selector/dropdown.result-form-selector.view') : {};
const SearchFormInteractionsView = require('component/search-form-interactions/search-form-interactions.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('search-interactions'),
    className: 'composed-menu',
    regions: {
        searchType: '.interaction-type',
        resultType: '.interaction-result-type',
        searchAdvanced: '.interaction-type-advanced',
        customInteractions: '.interaction-custom',
        searchSettings: '.interaction-settings'
    },
    events: {
        'click > .interaction-reset': 'triggerReset',
        'click > .interaction-type-advanced': 'triggerTypeAdvanced'
    },
    initialize() {
        this.listenTo(this.model, 'change:type', this.toggleCustomActions);
    },
    onRender(){
        this.listenTo(this.model, 'change:type closeDropdown', this.triggerCloseDropdown);
        this.generateSearchFormSelector();
        if(properties.hasExperimentalEnabled()) { 
            this.generateResultFormSelector() 
        }
        this.generateSearchSettings();
        this.generateCustomFormSelector();
        this.toggleCustomActions();
    },
    generateResultFormSelector() {
        this.resultType.show(new ResultFormSelectorDropdownView({
            model: new DropdownModel(),
            modelForComponent: this.model,
        }), {
            replaceElement: true
        });
    },
    generateSearchFormSelector() {
        this.searchType.show(new SearchFormSelectorDropdownView({
            model: new DropdownModel(),
            modelForComponent: this.model,
            selectionInterface: this.options.selectionInterface
        }), {
            replaceElement: true
        });
    },
    generateSearchSettings() {
        this.searchSettings.show(new SearchSettingsDropdownView({
            model: new DropdownModel(),
            modelForComponent: this.model,
            selectionInterface: this.options.selectionInterface,
            showFooter: true
        }), {
            replaceElement: true
        });
    },
    generateCustomFormSelector() {
        this.customInteractions.show(new SearchFormInteractionsView({
            model: new DropdownModel(),
            modelForComponent: this.model,
            collectionWrapperModel: this.options.collectionWrapperModel,
            queryModel: this.options.queryModel,
            dropdownCompanionBehaviors: {
                navigation: {}
            }
        }));
    },
    triggerCloseDropdown() {
        this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
    },
    triggerReset() {
        this.listenTo(ConfirmationView.generateConfirmation({
            prompt: 'Are you sure you want to reset the search?',
            no: 'Cancel',
            yes: 'Reset'
        }),
        'change:choice',
        function(confirmation) {
            if (confirmation.get('choice')) {
                const defaults = this.model.get('type') === 'custom' ? this.model.toJSON(): undefined;
                this.model.resetToDefaults(defaults);
                this.triggerCloseDropdown();
            }
        }.bind(this));
    },
    triggerTypeAdvanced() {
        let oldType = this.model.get('type');
        if (oldType === 'custom' || oldType === 'new-form') {
            this.model.set('title', 'Search Name');
        }
        this.model.set('type', 'advanced');
        user.getQuerySettings().set('type', 'advanced');
        user.savePreferences();
        this.triggerCloseDropdown();
    },
    toggleCustomActions() {
        const isCustom = this.model && this.model.get('type') === 'custom';
        this.$el.toggleClass('is-custom', isCustom);
    },
    serializeData() {
        return {
            experimental: properties.hasExperimentalEnabled()
        };
    }
});
