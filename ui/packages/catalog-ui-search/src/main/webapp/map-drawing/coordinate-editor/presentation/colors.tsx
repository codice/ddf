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
import { darken, readableColor } from 'polished'

const Black = (props: any): string =>
  readableColor(props.theme.backgroundContent)

const Silver = (props: any): string => darken(0.2)(White(props))

const Grey = (props: any): string => darken(0.4)(White(props))

const White = (props: any): string => props.theme.backgroundContent

const ButtonColor = (props: any): string => props.theme.primaryColor

const SubmitButtonColor = (props: any): string => props.theme.positiveColor

export { Black, Silver, Grey, White, ButtonColor, SubmitButtonColor }
