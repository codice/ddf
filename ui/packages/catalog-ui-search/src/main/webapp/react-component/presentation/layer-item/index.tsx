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
export {
  GrabCursor,
  IsButton,
  HighlightBehavior,
  DisabledBehavior,
} from '../../styles/mixins'
import { Order, Visibility, Actions } from '../../container/layer-item'

export {LayerRearrange}  from './rearrange'
export { LayerInteractions } from './layer-properties/interactions'
export { LayerAlpha } from './layer-properties/alpha'
export { LayerName } from './layer-properties/name'
export type Props = {
  name: string
  id: string
  warning: string
  isRemoveable: boolean
  order: Order
  visibility: Visibility
  actions: Actions
  options?: any
}
export { default } from './layers'
