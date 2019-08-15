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
import { Shape } from '../../shape-utils'
import { GeoEditorDialogProps } from './geo-editor-to-dialog'
import { BBoxEditorDialog } from './bbox-editor-dialog'
import { CircleEditorDialog } from './circle-editor-dialog'
import { LineEditorDialog } from './line-editor-dialog'
import { PointEditorDialog } from './point-editor-dialog'
import { PolygonEditorDialog } from './polygon-editor-dialog'

type Props = GeoEditorDialogProps & {
  /** Geometry shape */
  shape: Shape
}

const AllShapesEditorDialog: React.SFC<Props> = ({ shape, geo, onOk }) => {
  switch (shape) {
    case 'Polygon':
      return <PolygonEditorDialog geo={geo} onOk={onOk} />
    case 'Line':
      return <LineEditorDialog geo={geo} onOk={onOk} />
    case 'Point':
      return <PointEditorDialog geo={geo} onOk={onOk} />
    case 'Point Radius':
      return <CircleEditorDialog geo={geo} onOk={onOk} />
    case 'Bounding Box':
      return <BBoxEditorDialog geo={geo} onOk={onOk} />
    default:
      throw new Error(`Shape ${shape} is not supported!`)
  }
}

export default AllShapesEditorDialog
