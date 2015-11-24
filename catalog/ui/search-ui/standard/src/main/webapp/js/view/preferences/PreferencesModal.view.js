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
/* global define, require */
define([
        'application',
        'icanhaz',
        'underscore',
        'marionette',
        'backbone',
        'jquery',
        'cesium',
        'maptype',
        'js/view/Modal',
        'text!templates/preferences/preferences.modal.handlebars',
        'text!templates/preferences/color.preferences.tab.handlebars',
        'text!templates/preferences/layer.preferences.tab.handlebars',
        'text!templates/preferences/layerPicker.handlebars',
        'wreqr',
        'handlebars',
        'newuser'
    ],
    function (Application, ich, _, Marionette, Backbone, $, Cesium, maptype, Modal,
              newpreferencesModalTemplate, colorPrefsTabTemplate, layerPickerViewTemplate,
              layerPickerTemplate, wreqr, Handlebars, Newuser) {

        ich.addTemplate('newpreferencesModalTemplate', newpreferencesModalTemplate);
        ich.addTemplate('colorPrefsTabTemplate', colorPrefsTabTemplate);
        ich.addTemplate('layerPickerViewTemplate', layerPickerViewTemplate);
        ich.addTemplate('layerPickerTemplate', layerPickerTemplate);

        var PrefsModalView = Modal.extend({
            template: 'newpreferencesModalTemplate',
            className: 'well well-small',
            regions: {
                colorTabRegion: "#colorTab",
                layerTabRegion: "#layerTab"
            },
            events: {
                'shown.bs.tab .tabs-below>.nav-tabs>li>a': 'tabShown',
                'hidden.bs.modal': 'savePrefsChanges'
            },
            initialize: function () {
                // work-around for newuser; to be removed when user "new" user model is working
                this.model = new Newuser.Model();
            },
            tabShown: function (e) {
                wreqr.vent.trigger('prefsModal:tabshown', e.target.hash);
            },
            onRender: function () {
                this.colorTabRegion.show(new PrefsModalView.ColorTabView({model: this.model}));
                this.layerTabRegion.show(new PrefsModalView.LayerTabView({model: this.model}));
                wreqr.vent.trigger('prefsModal:tabshown', this.$('.nav-tabs > .active a').attr('href'));
            },
            savePrefsChanges: function () {
                //this.model.get('user').get('user>colorPrefs').savePreferences();
                this.model.get('colorPrefs').savePreferences();
            }
        });

        PrefsModalView.LayerPicker = Marionette.ItemView.extend({
            template: 'layerPickerTemplate',
            tagName: 'tr',
            className: 'layerPicker-row',
            events: {
                'click input[type="range"]': 'changeAlpha',
                'click input[type="checkbox"]': 'changeShow',
                'click button[data-value="up"]': 'changeUp',
                'click button[data-value="down"]': 'changeDown'
            },
            initialize: function (options) {
                this.viewOptions = options;
                Handlebars.registerHelper('canRaise', function () {
                    var index = options.collection.indexOf(options.model);
                    return index < options.collection.length - 1 ? '' : 'pickerNoRaise';
                });
                Handlebars.registerHelper('canLower', function () {
                    var index = options.collection.indexOf(options.model);
                    return index > 0 ? '' : 'pickerNoLower';
                });
            },
            changeAlpha: function () {
                var newAlphaValue = this.$el.find('input[type="range"]').val();
                this.model.set('alpha', newAlphaValue);

                var layer = this.viewOptions.layerForCid[this.model.cid];
                layer.alpha = newAlphaValue;
            },
            changeShow: function () {
                var newShowValue = this.$el.find('input[type="checkbox"]').is(':checked');
                this.model.set('show', newShowValue);

                var layer = this.viewOptions.layerForCid[this.model.cid];
                layer.show = newShowValue;
            },
            changeUp: function () {
                var layer = this.viewOptions.layerForCid[this.model.cid];
                this.viewOptions.getCesiumViewer().imageryLayers.raise(layer);
                this.model.collection.moveUp(this.model);
            },
            changeDown: function () {
                var layer = this.viewOptions.layerForCid[this.model.cid];
                this.viewOptions.getCesiumViewer().imageryLayers.lower(layer);
                this.model.collection.moveDown(this.model);
            }
        });

        PrefsModalView.LayerPickers = Marionette.CollectionView.extend({
            template: 'layerPickerViewTemplate',
            childView: PrefsModalView.LayerPicker,
            tagName: 'table',
            className: 'table layerPickerTable',
            childViewOptions: {},
            initialize: function (options) {
                // Marionette.CollectionView implicitly handles options.collection
                this.childViewOptions = options; // pass options to child views.
            },
            /*
             * following code is copy/paste of
             * https://github.com/marionettejs/backbone.marionette/blob/master/src/collection-view.js#L579-L618
             * but changed to "reverse" order of rendered list.
             */
            attachHtml: function (collectionView, childView, index) {
                if (collectionView.isBuffering) {
                    collectionView._bufferedChildren.splice(0, 0, childView); // instead of (index,0,childview)
                } else {
                    if (!collectionView.insertAfter(childView, index)) {
                        collectionView.insertBefore(childView);
                    }
                }
            },
            insertAfter: function(childView, index) {
                var currentView;
                var findPosition = this.getOption('sort') && (index < this.children.length - 1);
                if (findPosition) {
                    // Find the view after this one
                    currentView = this.children.find(function(view) {
                        return view._index === index + 1;
                    });
                }

                if (currentView) {
                    currentView.$el.after(childView.el); // instead of before().
                    return true;
                }

                return false;
            },
            insertBefore: function(childView) {
                this.$el.prepend(childView.el); // instead of append().
            }

        });

        PrefsModalView.LayerTabView = Marionette.LayoutView.extend({
            template: 'layerPickerViewTemplate',
            className: 'height-full',
            regions: {
                layerPickersRegion: "#layerPickers"
            },
            initialize: function () {
                var tabView = this;

                this.pickerOptions = {
                    //collection: this.model.get('user').get('user>layerPrefs'),
                    collection: this.model.get('layerPrefs'),
                    layerForCid: {},
                    getCesiumViewer: function () {
                        return tabView.cesiumViewer;
                    }
                };
            },
            onRender: function () {
                var layerPickers = new PrefsModalView.LayerPickers(this.pickerOptions);
                this.layerPickersRegion.show(layerPickers);
            },
            onAttach: function () {
                // must cesium map after containing div is attached to DOM.
                var threeDoptions = {
                    sceneMode: Cesium.SceneMode.SCENE3D,
                    animation: false,
                    geocoder: false,
                    navigationHelpButton: false,
                    fullscreenButton: false,
                    timeline: false,
                    homeButton: false,
                    sceneModePicker: false,
                    baseLayerPicker: false
                };

                var cesiumViewer = new Cesium.Viewer('layerPickerMap', threeDoptions);
                this.cesiumViewer = cesiumViewer;

                /*
                 * baseLayerPicker:false has side effect of creating default
                 * baselayer from default imageryProvider value; remove it here.
                 */
                cesiumViewer.imageryLayers.removeAll();

                var pickerOptions = this.pickerOptions;

                pickerOptions.collection.each(function (layerPref) {
                    var layer = cesiumViewer.imageryLayers.addImageryProvider(layerPref.get('provider'));
                    layer.alpha = Cesium.defaultValue(layerPref.get('alpha'), 0.5);
                    layer.show = Cesium.defaultValue(layerPref.get('show'), true);
                    layer.name = layerPref.get('label');
                    pickerOptions.layerForCid[layerPref.cid] = layer;
                });
            }
        });

        PrefsModalView.ColorTabView = Marionette.LayoutView.extend({
            template: 'colorPrefsTabTemplate',
            initialize: function () {
                this.modelBinder = new Backbone.ModelBinder();
            },
            onRender: function () {
                //var preferences = this.model.get('user').get('user>colorPrefs');
                var preferences = this.model.get('colorPrefs');
                var preferencesBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                this.modelBinder.bind(preferences, this.$el, preferencesBindings);
            },
            onShow: function () {
                require(['spectrum']);
            }
        });

        return PrefsModalView;
    });
