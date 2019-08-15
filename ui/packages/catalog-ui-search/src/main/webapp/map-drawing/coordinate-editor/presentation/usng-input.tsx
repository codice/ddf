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
import styled from 'styled-components'
import { HTMLAttributes } from '../../../react-component/html'
import { Converter } from 'usng.js'

type Props = HTMLAttributes & {
  /** USNG string */
  value: string
  /** Called on change */
  onChange: (value: string) => void
}

type State = {
  value: string
}

type FormEvent = React.FormEvent<HTMLInputElement>

const TextInput = styled.input<{ type: 'text' }>`
  width: 12em;
`
TextInput.displayName = 'TextInput'

class USNGInput extends React.Component<Props, State> {
  unitConverter: {
    isUSNG: (usng: string) => 0 | string
  }
  constructor(props: Props) {
    super(props)
    this.state = {
      value: '',
    }
    this.unitConverter = new (Converter as any)()
  }
  componentDidMount() {
    const value = this.props.value
    if (this.state.value !== value) {
      this.setState({ value })
    }
  }
  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.value !== this.props.value &&
      this.props.value !== this.state.value
    ) {
      this.setState({ value: this.props.value })
    }
  }
  render() {
    const { onChange, ...rest } = this.props
    return (
      <TextInput
        type="text"
        {...rest}
        value={this.state.value}
        onChange={({ currentTarget: { value } }: FormEvent) => {
          this.setState({ value })
        }}
        onBlur={() => {
          const value = this.state.value
          if (this.unitConverter.isUSNG(value) !== 0) {
            onChange(value)
          }
        }}
      />
    )
  }
}

export default USNGInput
