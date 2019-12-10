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
import styled from 'styled-components'
import { hot } from 'react-hot-loader'
import Dropdown from '../presentation/dropdown'
import MenuAction from '../menu-action'
import NavigationBehavior from '../navigation-behavior'
import { ContextType } from '../presentation/dropdown'

const Clipboard = require('clipboard')
const announcement = require('component/announcement')

type Props = {
  coordinateValues: {
    lat: string
    lon: string
  }
  closeParent: () => void
  selectCoordHandler: () => void
  clearRulerHandler: () => void
  mapModel: Backbone.Model
}

const Label = styled.div`
  display: inline-box;
  margin-left: ${props => props.theme.minimumSpacing};
`
const Icon = styled.div`
  margin-left: ${props => props.theme.minimumSpacing};
  display: inline-block;
  text-align: center;
  width: ${props => props.theme.minimumFontSize};
`

const CustomDropdown = styled(Dropdown as any)`
  width: 100%;
`

const Text = styled.div`
  line-height: 1.2rem;
`

const Description = styled.div`
  opacity: ${props => props.theme.minimumOpacity};
`

const BLANK_DISTANCE_TEXT = '----'

/*
 * Formats the current distance value to a string with the appropriate unit of measurement.
 */
const getDistanceText = (distance: number, state: string) => {
  // displays a "blank" when there's no distance measurement (in NONE or START state)
  if (state === 'NONE' || state === 'START') {
    return BLANK_DISTANCE_TEXT
  }
  // use meters when distance is under 1000m and convert to kilometers when ≥1000m
  const distanceText =
    distance < 1000 ? `${distance} m` : `${distance * 0.001} km`

  return distanceText
}

/*
 * Handler function for selecting a coordinate on the map. Calls selectCoordHandler() from props
 * to handle drawing the map markers and displays the measured distances.
 */
const coordHandler = (
  context: ContextType,
  closeParent: () => void,
  selectCoordHandler: () => void
) => {
  return () => {
    selectCoordHandler()

    context.closeAndRefocus()
    closeParent()
  }
}

/*
 * Handler function for clearing all markers on the map. Calls clearRulerHandler() from props
 * to handle removing the map markers from the map.
 */
const clearHandler = (
  context: ContextType,
  closeParent: () => void,
  clearRulerHandler: () => void
) => {
  return () => {
    clearRulerHandler()

    context.closeAndRefocus()
    closeParent()
  }
}

/*
 * Handler function for copying the measured distance string to the clipboard.
 */
const generateClipboardHandler = (
  text: string,
  context: ContextType,
  closeParent: () => void
) => {
  return (e: React.MouseEvent) => {
    const clipboardInstance = new Clipboard(e.target, {
      text: () => {
        return text
      },
    })
    clipboardInstance.on('success', (e: any) => {
      announcement.announce({
        title: 'Copied to clipboard',
        message: e.text,
        type: 'success',
      })
    })
    clipboardInstance.on('error', (e: any) => {
      announcement.announce({
        title: 'Press Ctrl+C to copy',
        message: e.text,
        type: 'info',
      })
    })
    clipboardInstance.onClick(e)
    clipboardInstance.destroy()
    context.closeAndRefocus()
    closeParent()
  }
}

const render = (props: Props) => {
  const { lat, lon } = props.coordinateValues
  const { closeParent, selectCoordHandler, clearRulerHandler, mapModel } = props
  const currentState = mapModel.get('measurementState')
  const distanceText = getDistanceText(
    mapModel.get('currentDistance'),
    currentState
  )
  let measurementSelectText = ''

  // determines the text to display on the menu action based on the current measurement state
  switch (currentState) {
    case 'NONE':
      measurementSelectText = 'Select start point'
      break
    case 'START':
      measurementSelectText = 'Select end point'
      break
    case 'END':
      measurementSelectText = 'Select new start point'
      break
    default:
      break
  }

  return (
    <CustomDropdown
      content={(context: ContextType) => (
        <NavigationBehavior>
          <MenuAction
            key="current-distance"
            icon="fa fa-clipboard"
            data-help="Copies the current distance measurement to the clipboard"
            onClick={
              /* the copy coordinates functionality is disabled when there's no distance
               * measurement
               */
              distanceText === BLANK_DISTANCE_TEXT
                ? () => {}
                : generateClipboardHandler(distanceText, context, closeParent)
            }
          >
            <Text>
              <Description>Copy current distance</Description>
              {distanceText}
            </Text>
          </MenuAction>
          <MenuAction
            key="add-marker"
            icon="fa fa-map-marker"
            data-help="Adds the selected coordinates to the measurement"
            onClick={coordHandler(context, closeParent, selectCoordHandler)}
          >
            <Text>
              <Description>{measurementSelectText}</Description>
              {`${lat} ${lon}`}
            </Text>
          </MenuAction>
          <MenuAction
            key="clear-markers"
            icon="fa fa-eraser"
            data-help="Clears the selected coordinates and map markers"
            onClick={clearHandler(context, closeParent, clearRulerHandler)}
          >
            <Text>
              <Description>Clear all coordinates</Description>
            </Text>
          </MenuAction>
        </NavigationBehavior>
      )}
    >
      <div className="metacard-interaction interaction-copy-coordinates">
        <div className="interaction-icon fa fa-calculator" />
        <Label className="interaction-text">
          Measure Distance
          <Icon className="fa fa-chevron-down fa-chevron-withmargin" />
        </Label>
      </div>
    </CustomDropdown>
  )
}

export default hot(module)(render)
