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

export type LayerInfo = {
  name: string
  id: string
  warning: string
  isRemovable: boolean
}

export type Order = {
  order: number
  isBottom: boolean
  isTop: boolean
}

export type Visibility = {
  alpha: number
  show: boolean
}

export type Actions = {
  updateLayerShow: () => void
  updateLayerAlpha: (e: any) => void
  moveDown: (e: any) => void
  moveUp: (e: any) => void
  onRemove: () => void
}

export { default } from './layer-item'
