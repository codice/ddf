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
    'js/controllers/ol.layerCollection.controller',
    'js/controllers/cesium.layerCollection.controller',
    'maptype',
    'js/view/Modal',
    'text!templates/preferences/preferences.modal.handlebars',
    'text!templates/preferences/color.preferences.tab.handlebars',
    'text!templates/preferences/layer.preferences.tab.handlebars',
    'text!templates/preferences/layer.list.handlebars',
    'text!templates/preferences/layerPicker.handlebars',
    'text!templates/preferences/preference.buttons.handlebars',
    'js/model/user',
    'cesium',
    'wreqr',
    // load dependencies
    'spectrum',
    'jquerySortable'
], function (Application, _, Marionette, Backbone, $, properties, OpenLayersController,
             CesiumLayersController, maptype, Modal, preferencesModalTemplate,
             colorPrefsTabTemplate, layerPrefsTabTemplate, layerListTemplate,
             layerPickerTemplate, preferenceButtonsTemplate, User, Cesium, wreqr) {
    var PrefsModalView = Modal.extend({
        template: preferencesModalTemplate,
        className: 'well well-small prefsModal',
        regions: {
            colorTabRegion: '#colorTab',
            layerTabRegion: '#layerTab'
        },
        events: { 'shown.bs.tab .nav-tabs > li > a[href="#layerTab"]': 'layerTabShown' },
        initialize: function () {
            // there is no automatic chaining of initialize.
            Modal.prototype.initialize.apply(this, arguments);
            var prefs = this.model.get('preferences');
            this.colorTabView = new PrefsModalView.ColorTabView({ model: prefs.get('mapColors') });
            this.layerTabView = new PrefsModalView.LayerTabView({ model: prefs.get('mapLayers') });
            // only create map the first time user selects map tab.
            this.layerTabShown = _.once(function () {
                this.layerTabView.setupMap();
            });
        },
        onRender: function () {
            this.colorTabRegion.show(this.colorTabView);
            this.layerTabRegion.show(this.layerTabView);
        }
    });
    PrefsModalView.ColorTabView = Marionette.LayoutView.extend({
        template: colorPrefsTabTemplate,
        regions: { colorButtonsRegion: '#colorButtons' },
        initialize: function () {
            this.viewModel = this.model.clone();
            this.modelBinder = new Backbone.ModelBinder();
            this.colorButtons = new PrefsModalView.Buttons({ tabView: this });
            // listen to any change on all models in collection.
            this.viewModel.on('change', this.onEdit, this);
        },
        onRender: function () {
            this.colorBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.viewModel, this.$el, this.colorBindings);
            this.colorButtonsRegion.show(this.colorButtons);
        },
        initSpectrum: function () {
            /*
                 the following fixes color input types for IE/chrome/firefox.

                 - see http://caniuse.com/#feat=input-color

                 - when spectrum is defined/required, it feature detects native support for
                 input[type=color]. Since chrome/firefox have native support, spectrum does
                 NOT "replace" the inputs at define/require time. IE does not have native support,
                 and spectrum DOES "replace" color inputs at define/require time.

                 - $.spectrum() does not do feature detection and always replaces the
                 input[type=color] element it's called on.

                 - also fixes native input color picker bug (chrome/firefox) where the native color
                 picker stays visible when the modal is dismissed.

                 - could not get modelbinder to work directly with spectrum; thus the colorChange
                 function and re-running initSpectrum from resetDefaults.
                 */
            this.$colorInputs = $('div[id="colorTab"] input[type="color"]');
            var colorChange = function (spectrumTinyColor) {
                var newColor = spectrumTinyColor.toHexString();
                this.$colorInput.val(newColor);
            };
            this.$colorInputs.each(function () {
                var $color = $(this);
                $color.spectrum({ change: _.bind(colorChange, { $colorInput: $color }) });
            });
        },
        onShow: function () {
            // initSpectrum can only find $colorInputs after onShow completes.
            _.defer(this.initSpectrum);
        },
        onEdit: function () {
            if (!this.isEdited) {
                this.colorButtons.ui.save.toggleClass('btn-default').toggleClass('btn-primary');
                this.isEdited = true;
            }
        },
        save: function () {
            if (this.isEdited) {
                this.model.set(this.viewModel.attributes);
                this.model.savePreferences();
                this.isEdited = false;
                this.colorButtons.ui.save.toggleClass('btn-default').toggleClass('btn-primary');
            }
        },
        resetDefaults: function () {
            this.onEdit();
            this.viewModel.set(this.model.getDefaults());
            this.initSpectrum();
        },
        onDestroy: function () {
            this.viewModel = null;
            this.modelBinder.unbind();
            $(this.$colorInputs).each(function () {
                $(this).spectrum('destroy');
            });
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
            this.viewMapLayers = new User.MapLayers(viewLayerModels);
            // listen to any change on all models in collection.
            this.viewMapLayers.on('change', this.onEdit, this);
            if (maptype.is3d()) {
                this.widgetController = new PrefsModalView.CesiumLayersController({ collection: this.viewMapLayers });
            } else if (maptype.is2d()) {
                this.widgetController = new PrefsModalView.OpenLayersController({ collection: this.viewMapLayers });
            }
            this.layerPickers = new PrefsModalView.LayerPickerTable({
                childView: PrefsModalView.LayerPicker,
                collection: this.viewMapLayers,
                childViewOptions: { widgetController: this.widgetController }
            });
            this.layerButtons = new PrefsModalView.Buttons({ tabView: this });
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
                    if (viewLayer.get('index') !== layer.get('index')) {
                        layer.set('index', viewLayer.get('index'));
                    }
                });
                wreqr.vent.trigger('preferencesModal:reorder:bigMap');
                this.model.savePreferences();
                this.isEdited = false;
                this.layerButtons.ui.save.toggleClass('btn-default').toggleClass('btn-primary');
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
    PrefsModalView.CesiumLayersController = CesiumLayersController.extend({
        showMap: function (divId) {
            this.makeMap({
                divId: divId,
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
    PrefsModalView.OpenLayersController = OpenLayersController.extend({
        showMap: function (divId) {
            this.makeMap({
                zoom: 2,
                controls: [],
                divId: divId
            });
        }
    });
    /*
         * using CompositeView because it supports table header in template.
         */
    PrefsModalView.LayerPickerTable = Marionette.CompositeView.extend({
        template: layerListTemplate,
        childViewContainer: '#pickerList',
        /*
             "reverse" the sort order of the picker rows, so that the lowest layer
             is represented by the "lowest" row in the table, and "raising/lowering" rows
             makes sense with raising/lowering map layers.
             */
        viewComparator: function (model) {
            return this.getReverseIndex(model.get('index'));
        },
        getReverseIndex: function (index) {
            return this.collection.length - 1 - index;
        },
        reorderOnSort: true,
        // do not override resortView(), else defeat default reorderOnSort.
        ui: { tbody: 'tbody' },
        onAttach: function () {
            var pickerList = this;
            this.ui.tbody.sortable({
                axis: 'y',
                containment: 'parent',
                handle: '.sortHandle',
                cursor: 'move',
                forceHelperSize: true,
                tolerance: 'pointer',
                forcePlaceholderSize: true,
                placeholder: 'sortable-placeholder',
                helper: function (e, tr) {
                    var $originals = tr.children();
                    var $helper = tr.clone();
                    $helper.addClass('layerPickerHelper');
                    // color helper.
                    // prevent "helper" resizing while dragging.
                    $helper.children().each(function (index) {
                        $(this).width($originals.eq(index).outerWidth());
                    });
                    return $helper;
                },
                update: function (event, ui) {
                    // the ui index is the "reverse" of how the models are indexed.
                    var layerPicker = ui.item.data('layerPicker');
                    var viewIndex = ui.item.index();
                    var newIndex = pickerList.getReverseIndex(viewIndex);
                    layerPicker.changeIndex(newIndex);
                    /*
                            update the marionette composite view indexes so that it's
                            reorder on sort works correctly.
                         */
                    pickerList.collection.forEach(function (model) {
                        var view = this.children.findByModel(model);
                        var viewIndex = pickerList.getReverseIndex(model.get('index'));
                        view._index = viewIndex;
                    }, pickerList);
                    pickerList.collection.sort();    // triggers view reorder and layers reorder.
                },
                start: function (event, ui) {
                    /*
                         default placeholder height is too small.
                         prevent visible widget resizing on drag/drop
                         */
                    ui.placeholder.height(ui.helper.outerHeight());
                }
            });
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
        changeIndex: function (newIndex) {
            this.model.set('index', newIndex);
            this.widgetController.move(this.model);
        },
        onDestroy: function () {
            this.modelBinder.unbind();
        }
    });
    return PrefsModalView;
});
