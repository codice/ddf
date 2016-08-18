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
/*global require*/
var utility = require('component/visualization/maps/cesium/utility');
var clustering = require('density-clustering');
var Cesium = require('cesium');
var dbscan = new clustering.DBSCAN();

function isNotVisible(cartesian3CenterOfGeometry, geocontroller, occluder){
  return !occluder.isPointVisible(cartesian3CenterOfGeometry, geocontroller);
}

module.exports = {
  /*
    Takes in a list of geometries and a view height and returns a list of clusters
  */
  calculateClusters: function(results, geocontroller){
    var occluder;
    if (geocontroller.scene.mode === Cesium.SceneMode.SCENE3D){
      occluder = new Cesium.EllipsoidalOccluder(Cesium.Ellipsoid.WGS84, geocontroller.scene.camera.position);
    }
    var centers = results.map(function(result){
      var cartesian3CenterOfGeometry = utility.calculateCartesian3CenterOfGeometry(result.get('metacard').get('geometry'), geocontroller);
      if (occluder && isNotVisible(cartesian3CenterOfGeometry, geocontroller, occluder)){
        return undefined;
      }
      var center = utility.calculateWindowCenterOfGeometry(cartesian3CenterOfGeometry, geocontroller);
      if (center) {
        return [center.x, center.y];
      } else {
        return undefined;
      }
    });
    for (var i = centers.length - 1; i >=0 ; i--){
        if (!centers[i]){
          results.splice(i, 1);
          centers.splice(i, 1);
        }
    }
    var clusters = dbscan.run(centers, 44, 2);
    clusters = clusters.map(function(cluster){
      return cluster.map(function(index){
        return results[index];
      });
    });
    var individuals = dbscan.noise.map(function(index){
        return results[index];
    });
    return {
      clusters: clusters,
      individuals: individuals
    };
  }
};