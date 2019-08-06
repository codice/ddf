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
import ThemeSettings from '../../container/theme-settings'
import AlertSettings from '../../container/alert-settings'
import SearchSettings from '../../../react-component/presentation/search-settings/search-settings'
import HiddenSettings from '../../../react-component/container/user-blacklist/user-blacklist'
const MapSettings = require('../../../component/layers/layers.view.js')
import TimeSettings from '../../container/time-settings'

import styled from '../../styles/styled-components'
import { Button, buttonTypeEnum } from '../button'
import { hot } from 'react-hot-loader'
import MarionetteRegionContainer from '../../container/marionette-region-container'

export type SettingsProps = {
  children: React.ReactNode[]
}

export type ComponentProps = {
  updateComponent?: (component?: React.ReactNode) => void
}

export const noOp = () => {}

type State = {
  component?: JSX.Element
}

const Root = styled<State, 'div'>('div')`
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  .user-settings-content {
    position: absolute;
    left: 0px;
    top: 0px;
    height: 100%;
    width: 100%;
    overflow: auto;
    transition: transform ${props => props.theme.coreTransitionTime} ease-in-out;
    transform: translate(${props => (props.component ? '0%' : '100%')});
  }
  .user-settings-navigation {
    position: absolute;
    left: 0px;
    top: 0px;
    height: 100%;
    width: 100%;
    overflow: auto;
    transition: transform ${props => props.theme.coreTransitionTime} ease-in-out;
    transform: translate(${props => (props.component ? '-100%' : '0%')});
  }
`

const Header = styled.div`
  margin-top: ${props => props.theme.minimumSpacing};
  font-size: ${props => props.theme.largeFontSize};
  font-weight: bolder;
  padding: 0px ${props => props.theme.mediumSpacing};
`

export const NavigationButton = styled(Button)`
  width: 100%;
  display: block;
  text-align: left;
  padding: 0px ${props => props.theme.mediumSpacing};
`

export const BackButton = styled(Button)`
  margin-top: ${props => props.theme.minimumSpacing};
  font-weight: bolder;
  width: 100%;
  display: block;
  text-align: left;
  padding: 0px ${props => props.theme.mediumSpacing};
`

export const ThemeSettingsComponent: React.FC<ComponentProps> = ({
  updateComponent = noOp,
}) => {
  return (
    <NavigationButton
      buttonType={buttonTypeEnum.neutral}
      text="Theme"
      icon="fa fa-paint-brush"
      onClick={() => {
        updateComponent(<ThemeSettings />)
      }}
    />
  )
}

export const AlertSettingsComponent: React.FC<ComponentProps> = ({
  updateComponent = noOp,
}) => {
  return (
    <NavigationButton
      buttonType={buttonTypeEnum.neutral}
      text="Notifications"
      icon="fa fa-bell"
      onClick={() => {
        updateComponent(<AlertSettings />)
      }}
    />
  )
}

export const MapSettingsComponent: React.FC<ComponentProps> = ({
  updateComponent = noOp,
}) => {
  return (
    <NavigationButton
      buttonType={buttonTypeEnum.neutral}
      text="Map"
      icon="fa fa-globe"
      onClick={() => {
        updateComponent(<MarionetteRegionContainer view={MapSettings} />)
      }}
    />
  )
}

export const SearchSettingsComponent: React.FC<ComponentProps> = ({
  updateComponent = noOp,
}) => {
  return (
    <NavigationButton
      buttonType={buttonTypeEnum.neutral}
      text="Search Options"
      icon="fa fa-search"
      onClick={() => {
        updateComponent(<SearchSettings showFooter={false} />)
      }}
    />
  )
}

export const TimeSettingsComponent: React.FC<ComponentProps> = ({
  updateComponent = noOp,
}) => {
  return (
    <NavigationButton
      buttonType={buttonTypeEnum.neutral}
      text="Time"
      icon="fa fa-clock-o"
      onClick={() => {
        updateComponent(<TimeSettings />)
      }}
    />
  )
}

export const HiddenSettingsComponent: React.FC<ComponentProps> = ({
  updateComponent = noOp,
}) => {
  return (
    <NavigationButton
      buttonType={buttonTypeEnum.neutral}
      text="Hidden"
      icon="fa fa-eye-slash"
      onClick={() => {
        updateComponent(<HiddenSettings />)
      }}
    />
  )
}

class UserSettings extends React.Component<SettingsProps, State> {
  constructor(props: SettingsProps) {
    super(props)
    this.state = {}
  }
  updateComponent = (component?: JSX.Element) => {
    this.setState({
      component,
    })
  }
  render() {
    const { component } = this.state
    const children = React.Children.map(this.props.children, child => {
      return React.cloneElement(child as JSX.Element, {
        updateComponent: this.updateComponent,
      })
    })
    return (
      <Root component={component}>
        <div className="user-settings-navigation">
          <Header>Settings</Header>
          <div className="is-divider" />
          {children}
        </div>
        <div className="user-settings-content">
          {children ? (
            <>
              <div className="content-header">
                <BackButton
                  buttonType={buttonTypeEnum.neutral}
                  icon="fa fa-chevron-left"
                  text="Back to Settings"
                  onClick={() => {
                    this.updateComponent()
                  }}
                />
              </div>
              <div className="is-divider" />
              <div className="content-settings">{component}</div>
            </>
          ) : (
            ''
          )}
        </div>
      </Root>
    )
  }
}

export default hot(module)(UserSettings)
