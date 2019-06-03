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

const clustering = require('density-clustering')
const dbscan = new clustering.DBSCAN()

function removeInvalidCenters(results, centers) {
  for (let i = centers.length - 1; i >= 0; i--) {
    if (!centers[i]) {
      results.splice(i, 1)
      centers.splice(i, 1)
    }
  }
}

function convertIndicesToResults(results, cluster) {
  return cluster.map(index => results[index])
}

module.exports = {
  /*
      Takes in a list of geometries and a view height and returns a list of clusters
    */
  calculateClusters(results, map) {
    const centers = map.getWindowLocationsOfResults(results)
    removeInvalidCenters(results, centers)
    return dbscan
      .run(centers, 44, 2)
      .map(cluster => convertIndicesToResults(results, cluster))
  },
}
