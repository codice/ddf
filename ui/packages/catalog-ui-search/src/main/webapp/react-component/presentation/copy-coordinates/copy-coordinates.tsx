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
import * as React from 'react'
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'
import Dropdown from '../dropdown'
import MenuAction from '../menu-action'
import NavigationBehavior from '../navigation-behavior'
import { ContextType } from '../../../react-component/presentation/dropdown'

const Clipboard = require('clipboard')
const announcement = require('component/announcement')

type Props = {
  clickLat: string
  clickLon: string
  clickDms: string
  clickMgrs: string
  clickUtmUps: string
  closeParent: () => void
}

const Label = styled.div`
  display: inline-box;
  margin-left: ${props => props.theme.minimumSpacing};
`
const Icon = styled.div`
  margin-left: ${props => props.theme.minimumSpacing};
  display: inline-block;
  text-align: center;
  width: ${props => props.theme.mediumSpacing};
`

const CustomDropdown = styled(Dropdown)`
  width: 100%;
`

const Text = styled.div`
  line-height: 1.2rem;
`

const Description = styled.div`
  opacity: ${props => props.theme.minimumOpacity};
`

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
  return (
    <CustomDropdown
      content={context => (
        <NavigationBehavior>
          <MenuAction
            icon="fa fa-clipboard"
            data-help="Copies the coordinates to your clipboard."
            onClick={generateClipboardHandler(
              `${props.clickLat} ${props.clickLon}`,
              context,
              props.closeParent
            )}
          >
            <Text>
              <Description>Decimal Degrees (DD)</Description>
              {props.clickLat + ' ' + props.clickLon}
            </Text>
          </MenuAction>
          <MenuAction
            icon="fa fa-clipboard"
            data-help="Copies the DMS coordinates to your clipboard."
            onClick={generateClipboardHandler(
              props.clickDms,
              context,
              props.closeParent
            )}
          >
            <Text>
              <Description>Degrees Minutes Seconds (DMS)</Description>
              {props.clickDms}
            </Text>
          </MenuAction>
          {props.clickMgrs ? (
            <MenuAction
              icon="fa fa-clipboard"
              data-help="Copies the MGRS coordinates to your clipboard."
              onClick={generateClipboardHandler(
                props.clickMgrs,
                context,
                props.closeParent
              )}
            >
              <Text>
                <Description>MGRS</Description>
                {props.clickMgrs}
              </Text>
            </MenuAction>
          ) : null}
          <MenuAction
            icon="fa fa-clipboard"
            data-help="Copies the UTM/UPS coordinates to your clipboard."
            onClick={generateClipboardHandler(
              props.clickUtmUps,
              context,
              props.closeParent
            )}
          >
            <Text>
              <Description>UTM/UPS</Description>
              {props.clickUtmUps}
            </Text>
          </MenuAction>
          <MenuAction
            icon="fa fa-clipboard"
            data-help="Copies the WKT of the coordinates to your clipboard."
            onClick={generateClipboardHandler(
              `POINT (${props.clickLon} ${props.clickLat})`,
              context,
              props.closeParent
            )}
          >
            <Text>
              <Description>Well Known Text (WKT)</Description>
              POINT ({props.clickLon} {props.clickLat})
            </Text>
          </MenuAction>
        </NavigationBehavior>
      )}
    >
      <div className="metacard-interaction interaction-copy-coordinates">
        <div className="interaction-icon fa fa-clipboard" />
        <Label className="interaction-text">
          Copy Coordinates as
          <Icon className="fa fa-chevron-down fa-chevron-withmargin" />
        </Label>
      </div>
    </CustomDropdown>
  )
}

export default hot(module)(render)
