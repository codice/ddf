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
/* global define*/
define([
    'application',
    'underscore',
    'marionette',
    'backbone',
    'jquery',
    'properties',
    'maptype',
    'templates/preferences/preferences.modal.handlebars',
    'templates/preferences/layer.preferences.tab.handlebars',
    'templates/preferences/layer.list.handlebars',
    'templates/preferences/layerPicker.handlebars',
    'templates/preferences/preference.buttons.handlebars',
    'component/singletons/user-instance'
], function (Application, _, Marionette, Backbone, $, properties, maptype,
             preferencesModalTemplate, layerPrefsTabTemplate, layerListTemplate,
             layerPickerTemplate, preferenceButtonsTemplate, user) {
    var PrefsModalView = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = user.get('user>preferences');
        },
        template: preferencesModalTemplate,
        className: 'prefsModal',
        regions: {
            layerTabRegion: '#layerTab'
        },
        initialize: function (options) {
            if (options.model===undefined){
                this.setDefaultModel();
            }
            this.layerTabView = new PrefsModalView.LayerTabView({
                model: this.model.get('mapLayers')
            });
        },
        onRender: function () {
            this.layerTabRegion.show(this.layerTabView);
        }
    });
    PrefsModalView.Buttons = Marionette.LayoutView.extend({
        template: preferenceButtonsTemplate,
        events: {
            'click button.save': 'save',
            'click button.reset-defaults': 'resetDefaults'
        },
        className: 'preferenceTabButtons',
        ui: { save: 'button.save' },
        initialize: function (options) {
            this.tabView = options.tabView;
        },
        save: function () {
            this.tabView.save();
        },
        resetDefaults: function () {
            this.tabView.resetDefaults();
        }
    });
    PrefsModalView.LayerTabView = Marionette.LayoutView.extend({
        template: layerPrefsTabTemplate,
        regions: {
            layerPickersRegion: '#layerPickers',
            layerButtonsRegion: '#layerButtons'
        },
        initialize: function () {
        },
        onRender: function () {
            // listen to any change on all models in collection.
            this.listenTo(this.model, 'change', this.save);
            // HACK fix it
            this.layerPickers = new PrefsModalView.LayerPickerTable({
                childView: PrefsModalView.LayerPicker,
                collection: this.model
            });
            this.layerButtons = new PrefsModalView.Buttons({ tabView: this });
            this.showLayerPickersAndLayerButtons();
        },
        showLayerPickersAndLayerButtons: function(){
            this.layerPickersRegion.show(this.layerPickers);
            this.layerButtonsRegion.show(this.layerButtons);
        },
        save: function () {
            this.model.sort();
            this.model.savePreferences();
            this.layerButtons.ui.save.toggleClass('btn-default').toggleClass('btn-primary');
        },
        resetDefaults: function () {
            this.model.each(function (viewLayer) {
                var url = viewLayer.get('url');
                var defaultConfig = _.find(properties.imageryProviders, function (layerObj) {
                    return url === layerObj.url;
                });
                viewLayer.set('show', true);
                viewLayer.set('alpha', defaultConfig.alpha);
            });
            this.model.sort();
            this.save();
        }
    });
    /*
         * using CompositeView because it supports table header in template.
         */
    PrefsModalView.LayerPickerTable = Marionette.CompositeView.extend({
        template: layerListTemplate,
        childViewContainer: '#pickerList',
        ui: { tbody: 'tbody' },
        viewComparator: 'label',
        initialize: function() {
            this.collection.each(function(model) {
                this.listenTo(model, 'change:alpha', this.updateSort);
            }, this);
        },
        updateSort: function() {
            var sort = false;
            var prevAlpha = 0;
            for (var index=0;index<this.collection.models.length;index++) {
                if (index !== 0) {
                    if (this.getAlpha(index) > prevAlpha) {
                        sort = true;
                        break;
                    } else {
                        prevAlpha = this.getAlpha(index);
                    }
                } else {
                    prevAlpha = this.getAlpha(index);
                }
            }
            if (sort) {
                this.collection.sort();
            }
        },
        /*
         * Get the alpha value set for a specific layer in the range [0,1].
         */
        getAlpha: function(index) {
            return this.collection.at(index).get('alpha') / 100.0;
        }
    });
    PrefsModalView.LayerPicker = Marionette.ItemView.extend({
        template: layerPickerTemplate,
        tagName: 'tr',
        className: 'layerPicker-row',
        ui: { range: 'input[type="range"]' },
        events: { 'sort': 'render' },
        initialize: function () {
            this.modelBinder = new Backbone.ModelBinder();
            this.$el.data('layerPicker', this);    // make model available to sortable.update()
            this.listenTo(this.model, 'change:show', this.changeShow);
        },
        onRender: function () {
            var layerBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model, this.$el, layerBindings);
            this.changeShow();           
        },
        changeShow: function () {
            this.$el.toggleClass('is-disabled', !this.model.get('show'));
            this.ui.range.prop('disabled', !this.model.get('show'));
        },
        onDestroy: function () {
            this.modelBinder.unbind();
        }
    });
    return PrefsModalView;
});
