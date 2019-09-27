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
import React from 'react'

import { storiesOf, object, action, text } from '../../storybook'

import EnumInput from '.'

const stories = storiesOf('EnumInput', module)

stories.add('basic', () => {
  const suggestions = object('Suggestions', [
    { label: 'first label', value: 'first value' },
    { label: 'second label', value: 'second value' },
    { label: 'third label', value: 'third value' },
  ])
  return (
    <EnumInput
      value={text('value', 'first value')}
      suggestions={suggestions}
      onChange={action('onChange')}
    />
  )
})

stories.add('with custom input', () => {
  const suggestions = object('Suggestions', [
    { label: 'first label', value: 'first value' },
    { label: 'second label', value: 'second value' },
    { label: 'third label', value: 'third value' },
  ])
  return (
    <EnumInput
      allowCustom
      value={text('value', 'custom value')}
      suggestions={suggestions}
      onChange={action('onChange')}
    />
  )
})

stories.add('case sensitive', () => {
  const suggestions = object('Suggestions', [
    { label: 'first label', value: 'first value' },
    { label: 'second label', value: 'second value' },
    { label: 'third label', value: 'third value' },
  ])

  return (
    <EnumInput
      matchCase
      suggestions={suggestions}
      onChange={action('onChange')}
    />
  )
})
