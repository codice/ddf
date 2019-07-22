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
import { storiesOf as of } from '@connexta/ace/@storybook/react'

import withTheme from './withTheme'

export const storiesOf = (name, m) => {
  const stories = of(name, m)
  stories.addDecorator(withTheme)
  return stories
}

export { action } from '@connexta/ace/@storybook/addon-actions'
export {
  array,
  text,
  number,
  boolean,
} from '@connexta/ace/@storybook/addon-knobs'
