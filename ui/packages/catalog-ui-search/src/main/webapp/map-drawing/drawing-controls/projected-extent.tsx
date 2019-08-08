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
import * as ol from 'openlayers'
import { Extent } from '../geometry'

class ProjectedExtent {
  map: Extent
  user: Extent
  constructor(
    userProjection: string,
    mapProjection: string,
    coordinates: Extent,
    isMapProjection: boolean
  ) {
    this.user = coordinates
    this.map = coordinates
    if (isMapProjection) {
      this.user = ol.proj.transformExtent(
        this.map,
        mapProjection,
        userProjection
      )
    } else {
      this.map = ol.proj.transformExtent(
        this.user,
        userProjection,
        mapProjection
      )
    }
  }
  public getMapCoordinates(): Extent {
    return this.map
  }
  public getUserCoordinates(): Extent {
    return this.user
  }
}

export default ProjectedExtent
