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
/* global define, window */
define(['marionette',
        'cesium',
        'underscore',
        'handlebars',
        'text!templates/clusteringMarker.handlebars'
], function(Marionette, Cesium, _, Handlebars, clusteringMarker) {

    var DEFAULT_PIXEL_DISTANCE = 100;

    var MAX_RADIUS_METER_DISTANCE = 2500000;

    var MIN_CAMERA_METER_HEIGHT = 10;

    var MapClustering = {};

    MapClustering = Marionette.Controller.extend({
        initialize : function() {
            this.clusters = [];
            this.clustering = false;
        },
        setResultLists : function(entityList) {
            this.entities = entityList;
        },
        setViewer : function(mapView) {
            this.viewer = mapView;
        },
        cluster : function (cameraHeight) {
            if(_.isUndefined(this.entities)) {
                return;
            } else if(cameraHeight < MIN_CAMERA_METER_HEIGHT) {
                this.uncluster();
            } else {
                var models = this.entities.geoController.mapViews.collection;
                var views = this.entities.geoController.mapViews.children;
                var clusters = [];

                models.each(function(value) {
                    var model = value.get("metacard");
                    var view = views.findByModel(model);

                    if(!_.isUndefined(view)) {
                        var billboard = view.billboard;
                        if(!_.isUndefined(billboard)) {
                            var cluster = this.fitsInExistingCluster(billboard, clusters);

                            // If the current point fits in an existing cluster add it,
                            // otherwise create a new cluster
                            if(cluster) {
                                this.addPointToCluster(cluster, billboard, view);
                            } else {
                                this.createNewCluster(billboard, view, clusters);
                            }
                            // hide entity
                            this.hideEntity(billboard, view);
                        }
                    }
                }, this);

                // Remove old entities
                _.each(this.clusters, function(value) {
                    var cluster = this.getCluster(value, clusters);
                    if(_.isUndefined(cluster)) {
                        this.viewer.entities.remove(value.entity);
                    }
                }, this);

                _.each(clusters, function(value) {
                    var cluster = this.getCluster(value, this.clusters);
                    // Render new entities
                    if(_.isUndefined(cluster)) {
                        if(value.points.length > 1) {
                            var imageLink = this.getSvgImage(value);
                            var newCluster = {
                                position : value.position,
                                billboard : {
                                    image : imageLink
                                }
                            };
                            value.entity = this.viewer.entities.add(newCluster);
                        } else {
                            this.showEntity(value.ref, value.views[0]);
                        }
                    } else {
                        // Set existing entities in the new cluster array and show non clustered points
                        value.entity = cluster.entity;
                        if(cluster.points.length === 1) {
                            this.showEntity(cluster.ref, cluster.views[0]);
                        }
                    }
                }, this);
                this.clusters = clusters;
            }
        },
        getCluster : function(cluster, clusters) {
            var result = _.find(clusters, function(value) {
                return _.isEqual(value.views, cluster.views);
            });
            return result;
        },
        showEntity : function(billboard, view) {
            billboard.show = true;
            if(!_.isUndefined(view.polygons)) {
                var polygons = view.polygons;
                _.each(polygons, function(value) {
                    value.fill.show = true;
                    value.outline.show = true;
                });
            }
        },
        hideEntity : function(billboard, view) {
            billboard.show = false;
            if(!_.isUndefined(view.polygons)) {
                var polygons = view.polygons;
                _.each(polygons, function(value) {
                    value.fill.show = false;
                    value.outline.show = false;
                });
            }
        },
        fitsInExistingCluster : function(position, clusters) {
            var cluster = false;
            _.each(clusters, function(value) {
                if(this.isInRadius(this.getRadius(value), position, value)) {
                    cluster = value;
                    return false;
                }
            }, this);
            return cluster;
        },
        createNewCluster : function (billboard, view, clusters) {
            var points = [];
            var views = [];
            points.push(billboard.position);
            views.push(view);
            clusters.push({color: view.color, ref : billboard, views : views, position : billboard.position, points: points, radius : DEFAULT_PIXEL_DISTANCE});
        },
        addPointToCluster : function(cluster, billboard, view) {
            var points = cluster.points;
            var views = cluster.views;
            points.push(billboard.position);
            views.push(view);
            var boundingSphere = Cesium.BoundingSphere.fromPoints(points);
            var center = boundingSphere.center;
            cluster.position = center;
        },
        uncluster : function()  {
            _.each(this.clusters, function(cluster) {
                this.viewer.entities.remove(cluster.entity);
                _.each(cluster.views, function(view) {
                    this.showEntity(view.billboard, view);
                }, this);
            }, this);
            this.clusters = [];
        },
        isInRadius : function isInRadius(radius, entity, centerEntity) {
            if(_.isUndefined(entity.position) || _.isUndefined(centerEntity)) {
                return false;
            }
            var distance = Cesium.Cartesian3.distance(entity.position, centerEntity.position);
            return (distance < radius);
        },
        /* Get a 20% threshold radius from the current view at the center of the canvas.  */
        getRadius : function getRadius(entity) {
            var width = this.viewer.canvas.width;
            var height = this.viewer.canvas.height;
            var left = this.getEllipsoidAt40PercentOfMapView(width, height);
            var right = this.getEllipsoidAt60PercentOfMapView(width, height);
            //  Left and Right are undefined at the maximum radius distance
            if(_.isUndefined(left) || _.isUndefined(right)) {
                return MAX_RADIUS_METER_DISTANCE;
            }
            var distanceInKm = Cesium.Cartesian3.distance(left, right);
            var distancePerPixel = distanceInKm / (width * 20 / 100);
            var distanceInPixels = distancePerPixel * entity.radius;
            return distanceInPixels;
        },
        getEllipsoidAt40PercentOfMapView : function(width, height) {
            return this.viewer.camera.pickEllipsoid(new Cesium.Cartesian2(width * 40 / 100, height / 2));
        },
        getEllipsoidAt60PercentOfMapView : function(width, height) {
            return this.viewer.camera.pickEllipsoid(new Cesium.Cartesian2(width * 60 / 100, height / 2));
        },
        toggleClustering : function() {
            this.clustering = !this.clustering;
            if(this.clustering) {
                this.cluster();
            } else {
                this.uncluster();
            }
        },
        getSvgImage : function(value) {
            var svg = Handlebars.compile(clusteringMarker)({
              fill: value.color.toCssColorString(),
              count: value.points.length
            });
            return 'data:image/svg+xml;base64,' + window.btoa(svg);
        }
    });
    return MapClustering;
});