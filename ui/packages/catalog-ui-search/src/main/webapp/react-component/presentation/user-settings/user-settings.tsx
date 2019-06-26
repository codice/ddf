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
import MarionetteRegionContainer from '../../container/marionette-region-container'
import styled from '../../styles/styled-components'
import { Button, buttonTypeEnum } from '../button'
import { hot } from 'react-hot-loader'

export type SettingsComponent = {
  component: JSX.Element
  text: string
  icon: string
  onClick?: () => void
  children?: React.ReactNode
}

export type withExtensions = {
  extensions: SettingsComponent[]
}

export type BaseProps = {}

type Props = BaseProps & withExtensions

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

const NavigationButton = styled(Button)`
  width: 100%;
  display: block;
  text-align: left;
  padding: 0px ${props => props.theme.mediumSpacing};
`

const BackButton = styled(Button)`
  margin-top: ${props => props.theme.minimumSpacing};
  font-weight: bolder;
  width: 100%;
  display: block;
  text-align: left;
  padding: 0px ${props => props.theme.mediumSpacing};
`

class UserSettings extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {}
  }
  updateComponent = (component?: JSX.Element) => {
    this.setState({
      component,
    })
  }
  render() {
    const { extensions } = this.props
    const { component } = this.state
    return (
      <Root component={component}>
        <div className="user-settings-navigation">
          <Header>Settings</Header>
          <div className="is-divider" />
          <NavigationButton
            buttonType={buttonTypeEnum.neutral}
            text="Theme"
            icon="fa fa-paint-brush"
            onClick={() => {
              this.updateComponent(<ThemeSettings />)
            }}
            disabled={Boolean(component)}
          />
          <NavigationButton
            buttonType={buttonTypeEnum.neutral}
            text="Notifications"
            icon="fa fa-bell"
            onClick={() => {
              this.updateComponent(<AlertSettings />)
            }}
            disabled={Boolean(component)}
          />
          <NavigationButton
            buttonType={buttonTypeEnum.neutral}
            text="Map"
            icon="fa fa-globe"
            onClick={() => {
              this.updateComponent(
                <MarionetteRegionContainer view={MapSettings} />
              )
            }}
            disabled={Boolean(component)}
          />
          <NavigationButton
            buttonType={buttonTypeEnum.neutral}
            text="Search Options"
            icon="fa fa-search"
            onClick={() => {
              this.updateComponent(<SearchSettings showFooter={false} />)
            }}
            disabled={Boolean(component)}
          />
          <NavigationButton
            buttonType={buttonTypeEnum.neutral}
            text="Time"
            icon="fa fa-clock-o"
            onClick={() => {
              this.updateComponent(<TimeSettings />)
            }}
            disabled={Boolean(component)}
          />
          <NavigationButton
            buttonType={buttonTypeEnum.neutral}
            text="Hidden"
            icon="fa fa-eye-slash"
            onClick={() => {
              this.updateComponent(<HiddenSettings />)
            }}
            disabled={Boolean(component)}
          />
          {extensions.map((extension: SettingsComponent) => (
            <NavigationButton
              key={extension.text}
              buttonType={buttonTypeEnum.neutral}
              text={extension.text}
              icon={extension.icon}
              onClick={
                typeof extension.onClick === 'function'
                  ? extension.onClick
                  : () => {
                      this.updateComponent(extension.component)
                    }
              }
              disabled={Boolean(component)}
            >
              {extension.children}
            </NavigationButton>
          ))}
        </div>
        <div className="user-settings-content">
          {component ? (
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
