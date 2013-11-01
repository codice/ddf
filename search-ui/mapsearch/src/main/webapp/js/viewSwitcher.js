/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

/**
 *
 * This file creates a 'ViewSwitcher' object, which is used to integrate the RecordView
 *  HTML into the Search Page.
 *
 *  A ViewSwitcher is created by passing in \<div\> IDs for both the area to display
 *   results and the area to display specific record information.  When showResultsView
 *   or showRecordView is called, the relevant div is populated and shown, and the other
 *   is hidden.
 *
 *  Additionally, showResultsView creates 'back', 'next', and 'previous' buttons for
 *   navigating between views or records.
 *
 *  DEPENDENCIES: recordView.js, searchPage.js
 *
 */

var ViewSwitcher = function (resultsDivId, recordDivId, mapDivId) {
    this.resultsDivId = resultsDivId;
    this.recordDivId = recordDivId;
    this.mapDivId = mapDivId;
    this.currentView = this.RESULTS_VIEW;
    this.currentIndex = 1;

    this._addAddtionalLayer = function (imageryLayerCollection, imageryProvider) {
        var layer;

        layer = imageryLayerCollection.addImageryProvider(imageryProvider);

        layer.alpha = 0.5;
        layer.brightness = 2.0;
    };

    this._addAdditionalLayers = function (imageryLayerCollection) {
        this._addAddtionalLayer(imageryLayerCollection, new Cesium.TileMapServiceImageryProvider({
            url: 'http://cesium.agi.com/blackmarble',
            maximumLevel: 8,
            credit: 'Black Marble imagery courtesy NASA Earth Observatory'
        }));

        //dev.virtualearth.net

        /* TODO: install and use proxy for WMS to resolve cross domain restrictions
         this._addAddtionalLayer(imageryLayerCollection, new Cesium.WebMapServiceImageryProvider({
         url: 'https://home2.gvs.dev/OGCOverlay/wms',
         layers : 'gvs',
         parameters : {
         transparent : 'true',
         format : 'image/png'
         }
         }));

         */
    };
    this._createPicker = function (scene, ellipsoid) {
        var labels, label, handler, cartesian, cartographic;
        labels = new Cesium.LabelCollection();
        label = labels.add();
        scene.getPrimitives().add(labels);

        // Mouse over the globe to see the cartographic position
        handler = new Cesium.ScreenSpaceEventHandler(scene.getCanvas());
        handler.setInputAction(function (movement) {
            cartesian = scene.getCamera().controller.pickEllipsoid(movement.endPosition, ellipsoid);
            if (cartesian) {
                cartographic = ellipsoid.cartesianToCartographic(cartesian);
                label.setShow(true);
                label.setText('(' + Cesium.Math.toDegrees(cartographic.longitude).toFixed(2) + ', ' + Cesium.Math.toDegrees(cartographic.latitude).toFixed(2) + ')');
                label.setPosition(cartesian);
            } else {
                label.setText('');
            }
        }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);

    };

    this._createMap = function (mapDivId) {
        var viewer, transitioner;
        viewer = new Cesium.Viewer(mapDivId, {
            // Start in Columbus Viewer
            // sceneMode : Cesium.SceneMode.COLUMBUS_VIEW,
            sceneMode: Cesium.SceneMode.SCENE3D,
            animation: false,
            fullscreenButton: false,
            timeline: false,

            // Hide the base layer picker for OpenStreetMaps
            baseLayerPicker: false,
            // Use OpenStreetMaps
            imageryProvider: new Cesium.OpenStreetMapImageryProvider({
                url: 'http://tile.openstreetmap.org/'
            })

        });

        transitioner = viewer.sceneTransitioner;

        // Create toolbar template
//        $('#toolbar')
//            .html(
//                '<button id="mode3D" class="cesium-button aButton">3D globe</button> '
//                    + '<button id="modeColumbus" class="cesium-button aButton">Columbus view</button> '
//                    + '<button id="mode2D" class="cesium-button aButton">2D map</button>');

        // Activate toolbar buttons with jQuery UI
//        $(".aButton").button({
//            text: true,
//            icons: {
//                primary: "ui-icon-blank"
//            }
//        }).click(function () {
//                // Emulate radio buttons
//                $(".aButton").button("option", {
//                    icons: {
//                        primary: "ui-icon-blank"
//                    }
//                });
//                $(this).button("option", {
//                    icons: {
//                        primary: "ui-icon-check"
//                    }
//                });
//            });

        // 3D is the default view
//        $('#mode3D').button("option", {
//            icons: {
//                primary: "ui-icon-check"
//            }
//        }).click(function () {
//                transitioner.morphTo3D();
//            });
//
//        $('#modeColumbus').click(function () {
//            transitioner.morphToColumbusView();
//        });
//
//        $('#mode2D').click(function () {
//            transitioner.morphTo2D();
//        });


        return viewer;
    };

    this.mapViewer = this._createMap(this.mapDivId);
    this._createPicker(this.mapViewer.scene, this.mapViewer.centralBody.getEllipsoid());
    this._addAdditionalLayers(this.mapViewer.centralBody.getImageryLayers());


};

ViewSwitcher.prototype = {
    RESULTS_VIEW: 1,
    METACARD_VIEW: 2,
    MAP_VIEW: 3,


    getCurrentView: function () {
        return this.currentView;
    },

    getCurrentIndex: function () {
        return this.currentIndex;
    },

    getMapViewer: function () {
        return this.mapViewer;
    },

    setCurrentIndex: function (index) {
        this.currentIndex = index;
    },

    showResultsView: function (index) {
        this.currentView = this.RESULTS_VIEW;
        $("#" + this.recordDivId).hide();
        $("#" + this.mapDivId).hide();

        if (index) {
            loadPageForItem(index);
        }
        $("#" + this.resultsDivId).show();
    },

    showMapView: function (index) {
        this.currentView = this.MAP_VIEW;
        $("#" + this.recordDivId).hide();
        $("#" + this.resultsDivId).hide();

//		$("#" + this.mapDivId).show();
        $("#cesiumContainer").show();

    },


    showRecordView: function (index) {
        var metacard, previousLi, nextLi, previousA, nextA, recordViewDivId, javascriptPrefix, backA, showResultsHref, showRecordHref, rv;

        this.currentView = this.METACARD_VIEW;
        metacard = getMetacard(index);
        if (metacard) {

            $("#" + this.resultsDivId).hide();
            $("#" + this.mapDivId).hide();

            // Variables referencing element IDs or var names. Hopefully we find
            //  a way to remove these in the future for better practices.
            previousLi = "#previousRecordLi";
            nextLi = "#nextRecordLi";
            previousA = previousLi + " a";
            nextA = nextLi + " a";
            recordViewDivId = "recordContentDiv";
            backA = "#backToResultsBtn a";

            javascriptPrefix = "javascript";
            showResultsHref = javascriptPrefix
                + ":viewSwitcher.showResultsView";
            showRecordHref = javascriptPrefix + ":showMetacard";

            $(backA).attr("href", showResultsHref + "(" + index + ")");

            if (index === 1) {
                $(previousLi).attr("class", "disabled");
                $(previousA).removeAttr("href");
            } else {
                $(previousA).attr("href",
                    showRecordHref + "(" + (index - 1) + ")");
                $(previousLi).removeAttr("class");
            }
            if (index === getMaxResults()) {
                $(nextLi).attr("class", "disabled");
                $(nextA).removeAttr("href");
            } else {
                $(nextA).attr("href", showRecordHref + "(" + (index + 1) + ")");
                $(nextLi).removeAttr("class");
            }

            $("#" + this.recordDivId).show();

            rv = new RecordView(metacard);
            rv.buildView(recordViewDivId);
        }
    }
};
