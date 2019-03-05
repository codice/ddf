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
/*global require*/
import * as React from 'react'
import styled from '../../../react-component/styles/styled-components'
import MarionetteRegionContainer from '../../container/marionette-region-container'
const PropertyView = require('../../../component/property/property.view.js')
const user = require('../../../component/singletons/user-instance.js')
const Property = require('../../../component/property/property.js')
const ThemeUtils = require('../../../js/ThemeUtils.js')

const getFontSize = () => {
  return getPreferences().get('fontSize')
}

const getPreferences = () => {
  return user.get('user').get('preferences')
}

const getAnimationMode = () => {
  return getPreferences().get('animation')
}

const getSpacingMode = () => {
  return getPreferences()
    .get('theme')
    .getSpacingMode()
}

const getTheme = () => {
  return getPreferences()
    .get('theme')
    .getColorMode()
}

const getCustomColorNames = () => {
  return getPreferences()
    .get('theme')
    .getCustomColorNames()
}

const getCustomColors = () => {
  return getCustomColorNames().map((colorVariable: any) => {
    return new Property({
      isEditing: true,
      label: colorVariable,
      value: [
        getPreferences()
          .get('theme')
          .get(colorVariable),
      ],
      type: 'COLOR',
    })
  })
}

const Root = styled.div`
  position: relative;
  display: block;
  text-align: center;
`
const ThemeCustom = styled.div`
  display: block;
`

const saveFontChanges = (fontSize: any) => {
  let preferences = getPreferences()
  let newFontSize = fontSize
  preferences.set('fontSize', ThemeUtils.getFontSize(newFontSize))
}

const saveSpacingChanges = (spacingMode: any) => {
  let preferences = getPreferences()
  let newSpacingMode = spacingMode
  preferences.get('theme').set('spacingMode', newSpacingMode)
  getPreferences().savePreferences()
}

const saveAnimationChanges = (animationMode: any) => {
  let preferences = getPreferences()
  let newAnimationMode = animationMode
  preferences.set('animation', newAnimationMode)
  getPreferences().savePreferences()
}

const saveHoverPreviewChanges = (hoverValue: any) => {
  let preferences = getPreferences()
  let newHoverPreview = hoverValue
  preferences.set('hoverPreview', newHoverPreview)
  getPreferences().savePreferences()
}

const saveThemeChanges = (themeValue: any) => {
  let preferences = getPreferences()
  let newTheme = themeValue
  preferences.get('theme').set('theme', newTheme)
  getPreferences().savePreferences()
}

const saveCustomColorVariable = (colorVariable: any, selectedValue: any) => {
  let preferences = getPreferences()
  let newValue = selectedValue
  preferences.get('theme').set(colorVariable, newValue)
  getPreferences().savePreferences()
}

class ThemeSettings extends React.Component<
  {},
  {
    fontSizeModel: any
    spacingModeModel: any
    animationModel: any
    hoverPreviewModel: any
    themeModel: any
    customToggle: boolean
    customColors: any[]
  }
> {
  constructor(props: any) {
    super(props)
    const themeScale = ThemeUtils.getZoomScale(getFontSize())
    this.state = {
      fontSizeModel: new Property({
        isEditing: true,
        label: 'Zoom Percentage',
        value: [themeScale],
        min: 62,
        max: 200,
        units: '%',
        type: 'RANGE',
      }),
      spacingModeModel: new Property({
        isEditing: true,
        enum: [
          {
            label: 'Comfortable',
            value: 'comfortable',
          },
          {
            label: 'Cozy',
            value: 'cozy',
          },
          {
            label: 'Compact',
            value: 'compact',
          },
        ],
        value: [getSpacingMode()],
        id: 'Spacing',
      }),
      animationModel: new Property({
        isEditing: true,
        label: 'Animation',
        value: [getAnimationMode()],
        enum: [
          {
            label: 'On',
            value: true,
          },
          {
            label: 'Off',
            value: false,
          },
        ],
        id: 'Animation',
      }),
      hoverPreviewModel: new Property({
        isEditing: true,
        label: 'Preview Full Image on Hover',
        value: [user.getHoverPreview()],
        enum: [
          {
            label: 'On',
            value: true,
          },
          {
            label: 'Off',
            value: false,
          },
        ],
        id: 'Full Image on Hover',
      }),
      themeModel: new Property({
        isEditing: true,
        enum: [
          {
            label: 'Dark',
            value: 'dark',
          },
          {
            label: 'Light',
            value: 'light',
          },
          {
            label: 'Sea',
            value: 'sea',
          },
          {
            label: 'Custom',
            value: 'custom',
          },
        ],
        value: [getTheme()],
        id: 'Theme',
      }),
      customToggle: this.isCustomSet(getTheme()),
      customColors: getCustomColors(),
    }

    this.state.fontSizeModel.on('change:value', () => {
      saveFontChanges(this.state.fontSizeModel.getValue()[0])
    })
    this.state.spacingModeModel.on('change:value', () => {
      saveSpacingChanges(this.state.spacingModeModel.getValue()[0])
    })
    this.state.animationModel.on('change:value', () => {
      saveAnimationChanges(this.state.animationModel.getValue()[0])
    })
    this.state.hoverPreviewModel.on('change:value', () => {
      saveHoverPreviewChanges(this.state.hoverPreviewModel.getValue()[0])
    })
    this.state.themeModel.on('change:value', () => {
      let themeValue = this.state.themeModel.getValue()[0]
      saveThemeChanges(themeValue)
      this.setState({
        customToggle: this.isCustomSet(themeValue),
      })
    })
  }
  isCustomSet(themeValue: string) {
    return themeValue === 'custom' ? true : false
  }
  render() {
    return (
      <Root {...this.props}>
        <div className="theme-font-size">
          <MarionetteRegionContainer
            view={PropertyView}
            viewOptions={() => {
              return {
                model: this.state.fontSizeModel,
              }
            }}
            replaceElement={false}
          />
        </div>
        <div className="theme-spacing-mode">
          <MarionetteRegionContainer
            view={PropertyView}
            viewOptions={() => {
              return {
                model: this.state.spacingModeModel,
              }
            }}
            replaceElement={false}
          />
        </div>
        <div className="theme-animation">
          <MarionetteRegionContainer
            view={PropertyView}
            viewOptions={() => {
              return {
                model: this.state.animationModel,
              }
            }}
            replaceElement={false}
          />
        </div>
        <div className="theme-hover-preview">
          <MarionetteRegionContainer
            view={PropertyView}
            viewOptions={() => {
              return {
                model: this.state.hoverPreviewModel,
              }
            }}
            replaceElement={false}
          />
        </div>
        <div className="theme-theme">
          <MarionetteRegionContainer
            view={PropertyView}
            viewOptions={() => {
              return {
                model: this.state.themeModel,
              }
            }}
            replaceElement={false}
          />
        </div>
        {this.state.customToggle ? <ThemeCustomComponent /> : null}
      </Root>
    )
  }
}

class ThemeCustomComponent extends React.Component<
  {},
  {
    customColorArray: any[]
  }
> {
  constructor(props: any) {
    super(props)
    this.state = {
      customColorArray: getCustomColors(),
    }
  }
  componentWillMount() {
    this.state.customColorArray.map((colorVariable: any) => {
      colorVariable.on('change:value', () => {
        saveCustomColorVariable(
          colorVariable.get('label'),
          colorVariable.getValue()[0]
        )
      })
    })
  }
  render() {
    return (
      <ThemeCustom>
        {this.state.customColorArray.map((colorVariable: any) => {
          return (
            <div>
              <MarionetteRegionContainer
                view={PropertyView}
                viewOptions={() => {
                  return {
                    model: colorVariable,
                  }
                }}
                replaceElement={false}
              />
            </div>
          )
        })}
      </ThemeCustom>
    )
  }
}
export default ThemeSettings
