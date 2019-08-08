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
import { Extent } from '../geometry'

type BBox = {
  north: number
  south: number
  east: number
  west: number
}

const bboxToExtent = ({ west, south, east, north }: BBox): Extent => [
  west,
  south,
  east,
  north,
]

const extentToBBox = ([west, south, east, north]: Extent): BBox => ({
  west,
  south,
  east,
  north,
})

type BBoxEditorProps = BBox & {
  setBBox: (bbox: BBox) => void
}

export { BBox, bboxToExtent, extentToBBox, BBoxEditorProps }
