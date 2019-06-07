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

export type SpecificSizingInterface = {
  minimumButtonSize: string
  minimumLineSize: string
  minimumSpacing: string
}

type SizingInterface = {
  mediumSpacing: string
  largeSpacing: string
}

type BorderRadiusInterface = {
  borderRadius: string
}

type ScreenSizes = {
  minimumScreenSize: string
  mobileScreenSize: string
  smallScreenSize: string
  mediumScreenSize: string
}

type ZIndexes = {
  zIndexMenubar: number
  zIndexLightbox: number
  zIndexLoadingCompanion: number
  zIndexSlideout: number
  zIndexContent: number
  zIndexConfirmation: number
  zIndexHelp: number
  zIndexVerticalMenu: number
  zIndexDropdown: number
  zIndexMenuItem: number
  zIndexBlocking: number
}

type Transitions = {
  transitionTime: string
  coreTransitionTime: string
}

type FontSizes = {
  minimumFontSize: string
  mediumFontSize: string
  largeFontSize: string
}

type Dividers = {
  dividerHeight: string
  minimumDividerSize: string
}

type Opacity = {
  minimumOpacity: number
}

export type ThemeColorInterface = {
  primaryColor: string
  positiveColor: string
  negativeColor: string
  warningColor: string
  favoriteColor: string
  backgroundNavigation: string
  backgroundAccentContent: string
  backgroundDropdown: string
  backgroundContent: string
  backgroundModal: string
  backgroundSlideout: string
}

type Current = {
  background: string
}

type ThemeName = {
  theme: string
}

type Helpers = {
  screenSize: number
  multiple: (multiplier: number, variable: string, unit?: string) => string
  screenBelow: (specifiedSize: string) => boolean
}

export type ThemeInterface = SizingInterface &
  SpecificSizingInterface &
  BorderRadiusInterface &
  ScreenSizes &
  ZIndexes &
  Transitions &
  FontSizes &
  Dividers &
  Opacity &
  ThemeColorInterface &
  ThemeName &
  Helpers &
  Current
