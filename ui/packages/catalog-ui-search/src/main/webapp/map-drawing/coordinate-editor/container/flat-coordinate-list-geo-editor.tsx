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
import * as React from 'react'
import {
  GeometryJSON,
  LengthUnit,
  METERS,
  geoToExtent,
  makeBufferedGeo,
} from '../../geometry'
import FlatCoordinateListEditor from '../presentation/flat-coordinate-list-editor'
import { GeoEditorProps } from './geo-editor-to-dialog'

type State = {
  editIndex: number
}

type Coordinates = [number, number][]

type Props = GeoEditorProps & {
  getCoordinatesFromGeo: (geo: GeometryJSON) => Coordinates
  updateGeoCoordinates: (
    geo: GeometryJSON,
    coordinates: Coordinates
  ) => GeometryJSON
}

class FlatCoordinateListGeoEditor extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      editIndex: 0,
    }
  }
  updateGeoProperties(
    geo: GeometryJSON,
    coordinates: Coordinates
  ): GeometryJSON {
    const updated = this.props.updateGeoCoordinates(geo, coordinates)
    const bufferedGeo = makeBufferedGeo(updated)
    updated.bbox = geoToExtent(bufferedGeo)
    return updated
  }
  render() {
    const { geo, coordinateUnit, onUpdateGeo } = this.props
    const editIndex = this.state.editIndex
    const coordinateList: Coordinates = this.props.getCoordinatesFromGeo(geo)
    const validIndex = Math.min(
      Math.max(editIndex, 0),
      coordinateList.length - 1
    )
    const lon = coordinateList[validIndex][0]
    const lat = coordinateList[validIndex][1]
    return (
      <FlatCoordinateListEditor
        selectedIndex={editIndex}
        buffer={geo.properties.buffer || 0}
        bufferUnit={geo.properties.bufferUnit || METERS}
        coordinateList={coordinateList}
        coordinateUnit={coordinateUnit}
        lat={lat}
        lon={lon}
        setBuffer={(buffer: number) => {
          const updated: GeometryJSON = {
            ...geo,
            properties: {
              ...geo.properties,
              buffer,
            },
          }
          onUpdateGeo(updated)
        }}
        setUnit={(bufferUnit: LengthUnit) => {
          const updated: GeometryJSON = {
            ...geo,
            properties: {
              ...geo.properties,
              bufferUnit,
            },
          }
          onUpdateGeo(updated)
        }}
        setCoordinate={(lat: number, lon: number) => {
          const updatedCoordinates = [...coordinateList]
          updatedCoordinates.splice(editIndex, 1, [lon, lat])
          const updated = this.updateGeoProperties(geo, updatedCoordinates)
          onUpdateGeo(updated)
        }}
        addCoordinate={() => {
          const updatedCoordinates = [...coordinateList]
          updatedCoordinates.splice(editIndex + 1, 0, [0, 0])
          const updated = this.updateGeoProperties(geo, updatedCoordinates)
          this.setState(
            {
              editIndex: editIndex + 1,
            },
            () => onUpdateGeo(updated)
          )
        }}
        deleteCoordinate={() => {
          const updatedCoordinates = [...coordinateList]
          updatedCoordinates.splice(editIndex, 1)
          const updated = this.updateGeoProperties(geo, updatedCoordinates)
          onUpdateGeo(updated)
        }}
        selectCoordinate={(editIndex: number) => {
          this.setState({
            editIndex,
          })
        }}
      />
    )
  }
}

export { FlatCoordinateListGeoEditor, Coordinates }
