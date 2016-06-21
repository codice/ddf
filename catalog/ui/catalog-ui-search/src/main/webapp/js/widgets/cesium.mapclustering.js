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
/* global define */
define(['backbone',
        'cesium',
        'jquery'
], function(Backbone, Cesium, $) {

    var DEFAULT_PIXEL_DISTANCE = 100;

    var MAX_RADIUS_DISTANCE = 2500000;

    var MapClustering = {};

    var clusters = [];

    var entities, viewer;

    MapClustering = Backbone.Model.extend({
        setResultLists : function(entityList) {
            entities = entityList;
        },
        setViewer : function(mapView) {
            viewer = mapView;
        },
        clusteringAlgorithm : function (cameraHeight) {
            if(typeof entities === "undefined") {
                return;
            } else if(cameraHeight < 5) {
                this.resetBillboard();
            } else {
                this.removeClusters();
                var models = entities.geoController.mapViews.collection;
                var views = entities.geoController.mapViews.children;
                var modelsLength = models.length;

                for(var c = 0; c < modelsLength; c++) {
                    var model = models.at(c).get("metacard");
                    var view = views.findByModel(model);
                    if(typeof view !== "undefined") {

                        var billboard = view.billboard;
                        if(typeof billboard !== "undefined") {
                            var cluster = this.fitsInExistingCluster(billboard);

                            // If the current point fits in an existing cluster add it,
                            // otherwise create a new cluster
                            if(cluster !== false) {
                                this.addPointToCluster(cluster, billboard, view);
                            } else {
                                this.createNewCluster(billboard, view);
                            }
                            // hide entity at position i
                            this.hideEntity(billboard, view);
                        }
                    }
                }

                var that = this;
                $.each(clusters, function(index, value) {
                    if(value.points.length > 1) {
                        var newCluster = {
                            position : value.position,
                            point : {
                                pixelSize : 25,
                                color : value.color
                            },
                            label : {
                                text : "" + value.points.length,
                                font : '14px Helvetica',
                                style : Cesium.LabelStyle.FILL
                            }
                        };
                        value.entity = viewer.entities.add(newCluster);
                    } else {
                        that.showEntity(value.ref, view);
                    }
                });
            }
        },
        showEntity : function(billboard, view) {
            billboard.show = true;
            if(typeof view.polygons !== "undefined" ) {
                var polygons = view.polygons;
                $.each(polygons, function(index, value) {
                    value.fill.show = true;
                    value.outline.show = true;
                });
            }
        },
        hideEntity : function(billboard, view) {
            billboard.show = false;
            if(typeof view.polygons !== "undefined" ) {
                var polygons = view.polygons;
                $.each(polygons, function(index, value) {
                    value.fill.show = false;
                    value.outline.show = false;
                });
            }
        },
        fitsInExistingCluster: function(position) {
            var that = this;
            var cluster = false;
            $.each(clusters, function(index, value) {
                if(that.isInRadius(that.getRadius(value), position, value)) {
                    cluster = value;
                    return false;
                }
            });
            return cluster;
        },
        createNewCluster : function (billboard, view) {
            var points = [];
            var views = [];
            points.push(billboard.position);
            views.push(view);
            clusters.push({color: view.color, ref : billboard, views : views, position : billboard.position, points: points, radius : DEFAULT_PIXEL_DISTANCE});
        },
        addPointToCluster: function(cluster, billboard, view) {
            var points = cluster.points;
            var views = cluster.views;
            points.push(billboard.position);
            views.push(view);
            var boundingSphere = Cesium.BoundingSphere.fromPoints(points);
            var center = boundingSphere.center;
            cluster.position = center;
            /*if(view.color !== cluster.color) {
                cluster.color = MARKER_COLOR;
            } */
        },
        removeClusters : function () {
            $.each(clusters, function(index, cluster) {
                viewer.entities.remove(cluster.entity);
            });
            clusters = [];
        },
        resetBillboard: function()  {
            var that = this;
            $.each(clusters, function(index, cluster) {
                viewer.entities.remove(cluster.entity);
                $.each(cluster.views, function(index, view) {
                    that.showEntity(view.billboard, view);
                });
            });
            clusters = [];
        },
        isInRadius: function isInRadius(radius, entity, centerEntity) {
            if(typeof entity.position === "undefined" || typeof centerEntity === "undefined" ) {
                return false;
            }
            var distance = Cesium.Cartesian3.distance(entity.position, centerEntity.position);
            return (distance < radius);
        },
        /* Get a 20% threshold radius from the current view at the center of the canvas.  */
        getRadius: function getRadius(entity) {
            var width = viewer.canvas.width;
            var height = viewer.canvas.height;
            var left = this.getEllipsoidAt40PercentOfMapView(width, height);
            var right = this.getEllipsoidAt60PercentOfMapView(width, height);
            //  Left and Right are undefined at the maximum radius distance
            if(typeof left === "undefined"  || typeof right === "undefined" ) {
                return MAX_RADIUS_DISTANCE;
            }
            var distanceInKm = Cesium.Cartesian3.distance(left, right);
            var distancePerPixel = distanceInKm / (width * 20 / 100);
            var distanceInPixels = distancePerPixel * entity.radius;
            return distanceInPixels;
        },
        getEllipsoidAt40PercentOfMapView: function(width, height) {
            return viewer.camera.pickEllipsoid(new Cesium.Cartesian2(width * 40 / 100, height / 2));
        },
        getEllipsoidAt60PercentOfMapView: function(width, height) {
            return viewer.camera.pickEllipsoid(new Cesium.Cartesian2(width * 60 / 100, height / 2));
        }
    });
    return MapClustering;
});