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

import { storiesOf, action, number, boolean } from '../storybook'

import AutoComplete from '.'

const stories = storiesOf('AutoComplete', module)

stories.add('basic', () => {
  const timeout = number('Timeout (Milliseconds)', 0)
  const minimumInputLength = number('Minimum Input Length', 3)
  const error = boolean('Throw Error', false)

  const suggester = input =>
    new Promise((resolve, reject) => {
      if (error) {
        return reject('Error!!!')
      }

      setTimeout(() => {
        const suggestions = [input, input + input, input + input + input]
        resolve(suggestions.map(id => ({ id, name: id })))
      }, timeout)
    })

  return (
    <AutoComplete
      value="Selected Value"
      onChange={action('onChange')}
      suggester={suggester}
      minimumInputLength={minimumInputLength}
    />
  )
})
