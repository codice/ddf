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
import * as styledComponents from 'styled-components'
import { ThemedStyledComponentsModule } from 'styled-components'

import { ThemeInterface } from './theme'

const {
  default: styled,
  css,
  injectGlobal,
  keyframes,
  ThemeProvider,
  withTheme,
} = styledComponents as ThemedStyledComponentsModule<ThemeInterface>

/*
  Remove once https://github.com/DefinitelyTyped/DefinitelyTyped/pull/28207 is merged
*/
const keyframesWithTheme: (
  strings: TemplateStringsArray,
  ...interpolations: styledComponents.FlattenInterpolation<
    styledComponents.ThemeProps<ThemeInterface>
  >[]
) => string = keyframes as any

export {
  css,
  injectGlobal,
  keyframesWithTheme as keyframes,
  ThemeProvider,
  withTheme,
}
export default styled
