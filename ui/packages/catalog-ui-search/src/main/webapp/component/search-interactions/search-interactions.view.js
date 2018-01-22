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
var lightboxInstance = require('component/lightbox/lightbox.view.instance');
var SearchSettingsDropdownView = require('component/dropdown/search-settings/dropdown.search-settings.view');
var DropdownModel = require('component/dropdown/dropdown');
var SearchTypeDropdownView = require('component/dropdown/search-type/dropdown.search-type.view');
var _merge = require('lodash/merge');
var ConfirmationView = require('component/confirmation/confirmation.view');
var user = require('component/singletons/user-instance');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('search-interactions'),
    className: 'composed-menu',
    regions: {
        searchType: '.interaction-type',
        searchSettings: '.interaction-settings'
    },
    events: {
        'click > .interaction-reset': 'triggerReset',
        'click > .interaction-type-text': 'triggerTypeText',
        'click > .interaction-type-basic': 'triggerTypeBasic',
        'click > .interaction-type-advanced': 'triggerTypeAdvanced',
        'click > .interaction-form': 'triggerCloseDropdown'
    },
    initialize: function() {
        this.handleType();
        this.listenTo(this.model, 'change:type', this.handleType);
    },
    handleType: function() {
        this.$el.removeClass('is-text').removeClass('is-basic').removeClass('is-advanced');
        switch(this.model.get('type')) {
            case 'text':
                this.$el.addClass('is-text');
            break;
            case 'basic':
                this.$el.addClass('is-basic');
            break;
            case 'advanced':
                this.$el.addClass('is-advanced');
            break;
        }
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
            showFooter: true
        }), {
            replaceElement: true
        });
    },
    triggerCloseDropdown: function() {
        this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
    },
    triggerReset: function() {
        this.listenTo(ConfirmationView.generateConfirmation({
            prompt: 'Are you sure you want to reset the search?',
            no: 'Cancel',
            yes: 'Reset'
        }),
        'change:choice',
        function(confirmation) {
            if (confirmation.get('choice')) {
                this.model.resetToDefaults();
                this.triggerCloseDropdown();
            }
        }.bind(this));
    },
    triggerType: function(type) {
        this.model.set('type', type);
        user.getQuerySettings().set('type', type);
        user.savePreferences();
    },
    triggerTypeText: function() {
        this.triggerType('text');
    },
    triggerTypeBasic: function() {
        this.triggerType('basic');
    },
    triggerTypeAdvanced: function() {
        this.triggerType('advanced');
    }
});