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
import { ThemeInterface } from '../styles/styled-components/theme'

type Spacing = {
  minimumButtonSize: number
  minimumLineSize: number
  minimumSpacing: number
}

type SpacingVariables =
  | 'minimumButtonSize'
  | 'minimumLineSize'
  | 'minimumSpacing'

const compact = {
  minimumButtonSize: 1.8,
  minimumLineSize: 1.5,
  minimumSpacing: 0.3,
}

const comfortable = {
  minimumButtonSize: 2.75,
  minimumLineSize: 1.875,
  minimumSpacing: 0.625,
}

const cozy = Object.keys(comfortable).reduce(
  (result: any, key: SpacingVariables) => {
    result[key] = (comfortable[key] + compact[key]) / 2
    return result
  },
  {}
)

const spacings = { compact, cozy, comfortable }

const base = {
  //sizing
  borderRadius: '1px',

  //screensizes
  minimumScreenSize: '20rem', //320px
  mobileScreenSize: '26.25rem', //420px
  smallScreenSize: '58.75rem', //940px
  mediumScreenSize: '90rem', // 1440px

  //z-indexes
  zIndexMenubar: 101,
  zIndexLightbox: 101,
  zIndexLoadingCompanion: 101,
  zIndexSlideout: 103,
  zIndexContent: 101,
  zIndexConfirmation: 103,
  zIndexHelp: 104,
  zIndexVerticalMenu: 101,
  zIndexDropdown: 103,
  zIndexMenuItem: 102,
  zIndexBlocking: 105,

  //transitions
  transitionTime: '0s',
  coreTransitionTime: '0.25s',

  //font sizes
  minimumFontSize: '0.75rem',
  mediumFontSize: '1rem',
  largeFontSize: '1.25rem',

  //spacing
  mediumSpacing: '1.25rem',
  largeSpacing: '1.875rem',

  //dividers.
  dividerHeight: '0.0625rem',
  minimumDividerSize: '1.3125rem',

  //opacity
  minimumOpacity: 0.6,
}

const dark = {
  //color palette
  primaryColor: '#32a6ad',
  positiveColor: '#5b963e',
  negativeColor: '#943838',
  warningColor: '#decd39',
  favoriteColor: '#d1d179',

  //color usage
  backgroundNavigation: '#213137',
  backgroundAccentContent: '#34434c',
  backgroundDropdown: '#253540',
  backgroundContent: '#253540',
  backgroundModal: '#253540',
  backgroundSlideout: '#435059',
}

const light = {
  //color palette
  primaryColor: '#3c6dd5',
  positiveColor: '#428442',
  negativeColor: '#8a423c',
  warningColor: '#c89600',
  favoriteColor: '#d1d179',

  //color usage
  backgroundNavigation: '#3c6dd5',
  backgroundAccentContent: '#edf9fc',
  backgroundDropdown: '#f3fdff',
  backgroundContent: '#f3fdff',
  backgroundModal: '#edf9fc',
  backgroundSlideout: '#edf9fc',
}

const sea = {
  //color palette
  primaryColor: '#32a6ad',
  positiveColor: '#154e7d',
  negativeColor: '#a32c00',
  warningColor: '#b65e1f',
  favoriteColor: '#709e33',

  //color usage
  backgroundNavigation: '#0f3757',
  backgroundAccentContent: '#ffffff',
  backgroundDropdown: '#ffffff',
  backgroundContent: '#ffffff',
  backgroundModal: '#e5e6e6',
  backgroundSlideout: '#e5e6e6',
}

const themes = { dark, light, sea }

const addUnits = (spacing: Spacing) => {
  return Object.keys(spacing).reduce((result: any, key: SpacingVariables) => {
    result[key] = spacing[key] + 'rem'
    return result
  }, {})
}

export type ColorMode = 'dark' | 'light' | 'sea'
export type SpacingMode = 'comfortable' | 'cozy' | 'compact'

type ThemeOptions = {
  colors: ColorMode
  spacing: SpacingMode
}

const defaultOptions = {
  colors: 'dark',
  spacing: 'comfortable',
} as ThemeOptions

export default (options = defaultOptions) => {
  const { colors, spacing } = options

  return {
    ...base,
    ...themes[colors],
    ...addUnits(spacings[spacing]),
  } as ThemeInterface
}
