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
import * as ol from 'openlayers'
import {
  storiesOf,
  action,
  select,
  array,
  text,
  number,
  boolean,
} from '../../react-component/storybook'
import { makeEmptyGeometry } from '../geometry'
import { Map, shapeList } from '../storybook-helpers'
import DrawingMenu from './drawing-menu'
import styled from 'styled-components'

const MenuContainer = styled.div`
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  width: 100%;
  height: 45px;
`

const STYLE = feature =>
  new ol.style.Style({
    stroke: new ol.style.Stroke({
      color: feature.get('color'),
      width: 2,
    }),
    fill: new ol.style.Fill({
      color: 'rgba(0, 0, 0, 0)',
    }),
    image: new ol.style.Circle({
      radius: 4,
      fill: new ol.style.Fill({
        color: feature.get('color'),
      }),
    }),
  })

const stories = storiesOf('map-drawing/drawing-menu/DrawingMenu', module)

const renderMap = DrawingMenuWithMap => (
  <React.Fragment>
    <Map
      getOlMap={olMap => {
        map = olMap
      }}
      projection="EPSG:4326"
    >
      <DrawingMenuWithMap />
    </Map>
  </React.Fragment>
)

stories.add('full featured', () => {
  const title = text('title', 'Untitled')
  const saveAndContinue = boolean('saveAndContinue', false)
  const isActive = boolean('isActive', true)
  const showCoordinateEditor = boolean('showCoordinateEditor', false)
  const id = 'someID'
  const color = text('color', '#0000FF')
  const shape = select('shape', shapeList, 'Polygon')
  const geometry = makeEmptyGeometry(id, shape, {
    color,
  })
  const DrawingMenuWithMap = ({ map }) => (
    <MenuContainer class="menu-container">
      <DrawingMenu
        shape={shape}
        map={map}
        isActive={isActive}
        showCoordinateEditor={showCoordinateEditor}
        saveAndContinue={saveAndContinue}
        title={title}
        geometry={geometry}
        toggleCoordinateEditor={action('toggleCoordinateEditor')}
        onCancel={action('Cancel')}
        onOk={action('Ok')}
        onSetShape={action('setShape')}
        onUpdate={action('update')}
        mapStyle={STYLE}
      />
    </MenuContainer>
  )
  return renderMap(DrawingMenuWithMap)
})

stories.add('simplified', () => {
  const isActive = boolean('isActive', true)
  const id = 'someID'
  const color = text('color', '#0000FF')
  const shape = select('shape', shapeList, 'Polygon')
  const geometry = makeEmptyGeometry(id, shape, {
    id,
    color,
  })
  const DrawingMenuWithMap = ({ map }) => (
    <MenuContainer class="menu-container">
      <DrawingMenu
        shape={shape}
        map={map}
        isActive={isActive}
        geometry={geometry}
        onCancel={action('Cancel')}
        onOk={action('Ok')}
        onSetShape={action('setShape')}
        onUpdate={action('update')}
        mapStyle={STYLE}
      />
    </MenuContainer>
  )
  return renderMap(DrawingMenuWithMap)
})

stories.add('minimal', () => {
  const isActive = boolean('isActive', true)
  const id = 'someID'
  const color = text('color', '#0000FF')
  const shape = select('shape', ['Line', 'Polygon'], 'Polygon')
  const geometry = makeEmptyGeometry(id, shape, {
    id,
    color,
  })
  const DrawingMenuWithMap = ({ map }) => (
    <MenuContainer class="menu-container">
      <DrawingMenu
        shape={shape}
        map={map}
        disabledShapes={['Bounding Box', 'Point Radius', 'Point']}
        isActive={isActive}
        geometry={geometry}
        onCancel={action('Cancel')}
        onOk={action('Ok')}
        onSetShape={action('setShape')}
        onUpdate={action('update')}
        mapStyle={STYLE}
      />
    </MenuContainer>
  )
  return renderMap(DrawingMenuWithMap)
})
