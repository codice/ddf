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
var Marionette = require('marionette');
var _ = require('underscore');
var $ = require('jquery');
var template = require('./search-interactions.hbs');
var CustomElements = require('js/CustomElements');
var MenuNavigationDecorator = require('decorator/menu-navigation.decorator');
var Decorators = require('decorator/Decorators');
var lightboxInstance = require('component/lightbox/lightbox.view.instance');
var SearchSettingsDropdownView = require('component/dropdown/search-settings/dropdown.search-settings.view');
var DropdownModel = require('component/dropdown/dropdown');
var SearchTypeDropdownView = require('component/dropdown/search-type/dropdown.search-type.view');
var _merge = require('lodash/merge');
var ConfirmationView = require('component/confirmation/confirmation.view');

module.exports = Marionette.LayoutView.extend(Decorators.decorate({
    template: template,
    tagName: CustomElements.register('search-interactions'),
    modelEvents: {
    },
    regions: {
        searchType: '.interaction-type',
        searchSettings: '.interaction-settings'
    },
    events: {
        'click > .interaction-reset': 'triggerReset',
        'click > .interaction-revert': 'triggerRevert'
    },
    ui: {
    },
    initialize: function(){
        this.handleAllowRevert();
    },
    handleAllowRevert: function() {
        this.$el.toggleClass('allow-revert', this.options.allowRevert === true);
    },
    onRender: function(){
        this.generateSearchType();
        this.generateSearchSettings();
    },
    generateSearchType: function() {
        this.searchType.show(new SearchTypeDropdownView({
            model: new DropdownModel(),
            modelForComponent: this.model,
            selectionInterface: this.options.selectionInterface
        }), {
            replaceElement: true
        });
    },
    generateSearchSettings: function() {
        this.searchSettings.show(new SearchSettingsDropdownView({
            model: new DropdownModel(),
            modelForComponent: this.model,
            selectionInterface: this.options.selectionInterface,
            showSave: true
        }), {
            replaceElement: true
        });
    },
    triggerReset: function() {
        this.listenTo(ConfirmationView.generateConfirmation({
            prompt: 'Are you sure you want to save over the current search with defaults?',
            no: 'Cancel',
            yes: 'Overwrite'
        }),
        'change:choice',
        function(confirmation) {
            if (confirmation.get('choice')) {
                this.model.resetToDefaults();
                this.handleClick();
            }
        }.bind(this));
    },
    triggerRevert: function() {
        this.listenTo(ConfirmationView.generateConfirmation({
            prompt: 'Are you sure you want to save over the current search with the last known version?',
            no: 'Cancel',
            yes: 'Overwrite'
        }),
        'change:choice',
        function(confirmation) {
            if (confirmation.get('choice')) {
                this.model.revert();
                this.handleClick();
            }
        }.bind(this));
    },
    handleClick: function(){
        this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
    }
}, MenuNavigationDecorator));