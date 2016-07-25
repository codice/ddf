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
/* global define,setTimeout,require*/
define([
    'application',
    'underscore',
    'marionette',
    'backbone',
    'jquery',
    'properties',
    'maptype',
    'js/view/Modal',
    'templates/preferences/preferences.modal.handlebars',
    'templates/preferences/layer.preferences.tab.handlebars',
    'templates/preferences/layer.list.handlebars',
    'templates/preferences/layerPicker.handlebars',
    'templates/preferences/preference.buttons.handlebars',
    'component/singletons/user-instance',
    'wreqr',
    // load dependencies
    'spectrum',
    'jquerySortable'
], function (Application, _, Marionette, Backbone, $, properties, maptype, Modal,
             preferencesModalTemplate, layerPrefsTabTemplate, layerListTemplate,
             layerPickerTemplate, preferenceButtonsTemplate, user, wreqr) {
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
            setTimeout(function () {
                this.layerTabView.setupMap();
            }.bind(this), 100);
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
            var viewLayerModels = [];
            this.model.each(function (layerModel) {
                var clonedLayerModel = layerModel.clone();
                clonedLayerModel.set('modelCid', layerModel.cid);
                viewLayerModels.push(clonedLayerModel);
            });
            var MapLayerConstructor = user.get('user>preferences>mapLayers').constructor;
            this.viewMapLayers = new MapLayerConstructor(viewLayerModels);
            // listen to any change on all models in collection.
            this.listenTo(this.viewMapLayers, 'change', this.onEdit);
            if (maptype.is3d()) {
                require(['cesium', 'js/controllers/cesium.layerCollection.controller'], function(Cesium, CesiumLayersController){
                    PrefsModalView.CesiumLayersController = CesiumLayersController.extend({
                        showMap: function () {
                            this.makeMap({
                                element: this.options.element.el.querySelector('#layerPickerMap'),
                                cesiumOptions: {
                                    sceneMode: Cesium.SceneMode.SCENE3D,
                                    animation: false,
                                    geocoder: false,
                                    navigationHelpButton: false,
                                    fullscreenButton: false,
                                    timeline: false,
                                    homeButton: false,
                                    sceneModePicker: false,
                                    baseLayerPicker: false
                                }
                            });
                        }
                    });
                    this.widgetController = new PrefsModalView.CesiumLayersController({
                        collection: this.viewMapLayers,
                        element: this
                    });

                    // HACK fix it
                    this.layerPickers = new PrefsModalView.LayerPickerTable({
                        childView: PrefsModalView.LayerPicker,
                        collection: this.viewMapLayers,
                        childViewOptions: { widgetController: this.widgetController }
                    });
                    this.layerButtons = new PrefsModalView.Buttons({ tabView: this });
                }.bind(this));
            } else if (maptype.is2d()) {
                require(['js/controllers/ol.layerCollection.controller'], function(OpenLayersController){
                    PrefsModalView.OpenLayersController = OpenLayersController.extend({
                        showMap: function () {
                            this.makeMap({
                                element: this.options.element.el.querySelector('#layerPickerMap'),
                                zoom: 2,
                                controls: []
                            });
                        }
                    });
                    this.widgetController = new PrefsModalView.OpenLayersController({
                        collection: this.viewMapLayers,
                        element: this
                    });

                    // HACK fix it
                    this.layerPickers = new PrefsModalView.LayerPickerTable({
                        childView: PrefsModalView.LayerPicker,
                        collection: this.viewMapLayers,
                        childViewOptions: { widgetController: this.widgetController }
                    });
                    this.layerButtons = new PrefsModalView.Buttons({ tabView: this });
                }.bind(this));
            }
        },
        onRender: function () {
            this.layerPickersRegion.show(this.layerPickers);
            this.layerButtonsRegion.show(this.layerButtons);
        },
        setupMap: function () {
            /*
                 maps are sensitive to DOM state.
                 cesium must be created sometime after containing DOM is "attached".
                 openlayers must be created sometime after containing DOM is "visible".
                 */
            this.widgetController.showMap('layerPickerMap');
        },
        onDestroy: function () {
            this.viewMapLayers = null;
            this.widgetController.destroy();
        },
        onEdit: function () {
            if (!this.isEdited) {
                this.layerButtons.ui.save.toggleClass('btn-default').toggleClass('btn-primary');
                this.isEdited = true;
            }
        },
        save: function () {
            if (this.isEdited) {
                var mapLayers = this.model;
                this.viewMapLayers.each(function (viewLayer) {
                    var layer = mapLayers.get(viewLayer.get('modelCid'));
                    if (viewLayer.get('alpha') !== layer.get('alpha')) {
                        layer.set('alpha', viewLayer.get('alpha'));
                    }
                    if (viewLayer.get('show') !== layer.get('show')) {
                        layer.set('show', viewLayer.get('show'));
                    }
                });
                this.model.sort();
                this.model.savePreferences();
                this.isEdited = false;
                this.layerButtons.ui.save.toggleClass('btn-default').toggleClass('btn-primary');
                wreqr.vent.trigger('preferencesModal:reorder:bigMap');
            }
        },
        resetDefaults: function () {
            this.onEdit();
            var model = this.model;
            this.viewMapLayers.each(function (viewLayer) {
                var url = viewLayer.get('url');
                var defaultConfig = model.getMapLayerConfig(url);
                viewLayer.set('show', defaultConfig.show);
                viewLayer.set('alpha', defaultConfig.alpha);
                viewLayer.set('index', defaultConfig.index);
            });
            this.viewMapLayers.sort();
        }
    });
    /*
         * using CompositeView because it supports table header in template.
         */
    PrefsModalView.LayerPickerTable = Marionette.CompositeView.extend({
        template: layerListTemplate,
        childViewContainer: '#pickerList',
        ui: { tbody: 'tbody' },
        initialize: function() {
            this.collection.each(function(model) {
                this.listenTo(model, 'change', this.updateSort);
            }, this);
        },
        updateSort: function() {
            var sort = false;
            var prevAlpha = 0;
            for (var index=0;index<this.collection.models.length;index++) {
                if (index !== 0) {
                    if (this.collection.at(index).get('alpha') > prevAlpha) {
                        sort = true;
                        break;
                    } else {
                        prevAlpha = this.collection.at(index).get('alpha');
                    }
                } else {
                    prevAlpha = this.collection.at(index).get('alpha');
                }
            }
            if (sort) {
                this.collection.sort();
            }
        }
    });
    PrefsModalView.LayerPicker = Marionette.ItemView.extend({
        template: layerPickerTemplate,
        tagName: 'tr',
        className: 'layerPicker-row',
        ui: { range: 'input[type="range"]' },
        initialize: function (options) {
            this.widgetController = options.widgetController;
            this.$el.data('layerPicker', this);    // make model available to sortable.update()
        },
        onRender: function () {
            this.modelBinder = new Backbone.ModelBinder();
            var layerBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model, this.$el, layerBindings);
            // bound on viewModel.
            this.listenTo(this.model, 'change:show', this.changeShow);
            this.ui.range.prop('disabled', !this.model.get('show'));
        },
        changeShow: function (model) {
            this.ui.range.prop('disabled', !model.get('show'));
        },
        onDestroy: function () {
            this.modelBinder.unbind();
        }
    });
    return PrefsModalView;
});
