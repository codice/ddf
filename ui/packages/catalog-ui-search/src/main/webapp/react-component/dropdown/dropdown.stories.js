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

import { storiesOf, action, array, text, boolean } from '../storybook'

import Dropdown from '.'
import { Menu, MenuItem } from '../menu'

const stories = storiesOf('Dropdown', module)

const ExampleChild = () => (
  <div
    style={{
      height: 200,
      padding: 20,
      boxSizing: 'border-box',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
    }}
  >
    Dropdown Children
  </div>
)

stories.add('basic', () => {
  const open = boolean('Dropdown Open', false)

  return (
    <Dropdown
      open={open ? open : undefined}
      label="test"
      onClose={action('onClose')}
      onOpen={action('onOpen')}
    >
      <ExampleChild />
    </Dropdown>
  )
})

stories.add('with menu', () => {
  const value = text('Menu Value', 'first option')

  const values = array('Menu Items', [
    'first option',
    'second option',
    'third option',
  ])

  return (
    <Dropdown
      label="test"
      onClose={action('onClose')}
      onOpen={action('onOpen')}
    >
      <Menu value={value} onChange={action('onChange')}>
        {values.map(value => {
          return <MenuItem value={value} />
        })}
      </Menu>
    </Dropdown>
  )
})

stories.add('with custom anchor', () => {
  const open = boolean('Dropdown Open', false)

  const anchor = (
    <div
      style={{
        padding: 10,
        boxSizing: 'border-box',
        border: '1px dashed rgba(0, 0, 0, 0.4)',
      }}
    >
      My Custom Anchor
    </div>
  )

  return (
    <Dropdown
      open={open ? open : undefined}
      label="test"
      onClose={action('onClose')}
      onOpen={action('onOpen')}
      anchor={anchor}
    >
      <ExampleChild />
    </Dropdown>
  )
})

stories.add('with small anchor', () => {
  const open = boolean('Dropdown Open', false)

  const anchor = (
    <div
      style={{
        padding: 10,
        boxSizing: 'border-box',
        border: '1px dashed rgba(0, 0, 0, 0.4)',
        width: 60,
        textAlign: 'center',
        margin: '0 auto',
      }}
    >
      (+)
    </div>
  )

  return (
    <div style={{ display: 'flex', justifyContent: 'flex-end', width: 400 }}>
      <Dropdown
        open={open ? open : undefined}
        label="test"
        onClose={action('onClose')}
        onOpen={action('onOpen')}
        anchor={anchor}
      >
        <ExampleChild />
      </Dropdown>
    </div>
  )
})

stories.add('with overflow', () => {
  const open = boolean('Dropdown Open', false)

  const anchor = (
    <div
      style={{
        padding: 10,
        boxSizing: 'border-box',
        border: '1px dashed rgba(0, 0, 0, 0.4)',
        width: 60,
        textAlign: 'center',
        margin: '0 auto',
      }}
    >
      (+)
    </div>
  )

  return (
    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
      <Dropdown
        open={open ? open : undefined}
        label="test"
        onClose={action('onClose')}
        onOpen={action('onOpen')}
        anchor={anchor}
      >
        <ExampleChild />
      </Dropdown>

      <Dropdown
        open={open ? open : undefined}
        label="test"
        onClose={action('onClose')}
        onOpen={action('onOpen')}
        anchor={anchor}
      >
        <ExampleChild />
      </Dropdown>
    </div>
  )
})
