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
import { HTMLAttributes } from '../../../react-component/html'

type Props = HTMLAttributes & {
  /** Numeric value */
  value: number | null
  /** Maximum allowed value */
  maxValue?: number
  /** Minimum allowed value */
  minValue?: number
  /** Number of displayed decimal places */
  decimalPlaces?: number
  /** Called on change */
  onChange: (value: number) => void
}

type State = {
  stringValue: string
}

type FormEvent = React.FormEvent<HTMLInputElement>

class NumberInput extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      stringValue: '',
    }
  }
  componentDidMount() {
    const stringValue = this.constrainedString(this.props.value)
    if (this.state.stringValue !== stringValue) {
      this.setState({ stringValue })
    }
  }
  componentDidUpdate(prevProps: Props) {
    const value = this.constrainedString(this.props.value)
    const prevValue = this.constrainedString(prevProps.value)
    if (
      (prevValue !== value && value !== this.state.stringValue) ||
      prevProps.maxValue !== this.props.maxValue ||
      prevProps.minValue !== this.props.minValue ||
      prevProps.decimalPlaces !== this.props.decimalPlaces
    ) {
      this.setState({ stringValue: value }, () => {
        const number = this.constrainedNumber(this.props.value)
        if (number !== this.props.value) {
          this.props.onChange(number)
        }
      })
    }
  }
  constrainedNumber(value: number | null): number | null {
    const {
      maxValue = Number.POSITIVE_INFINITY,
      minValue = Number.NEGATIVE_INFINITY,
    } = this.props
    if (value === undefined || value === null || isNaN(value)) {
      return null
    } else {
      let constrained = value
      if (maxValue !== undefined && !isNaN(maxValue)) {
        constrained = Math.min(constrained, maxValue)
      }
      if (minValue !== undefined && !isNaN(minValue)) {
        constrained = Math.max(constrained, minValue)
      }
      return constrained
    }
  }
  constrainedString(value: number | null): string {
    const decimalPlaces = this.props.decimalPlaces || 0
    const n = this.constrainedNumber(value)
    if (n === null) {
      return ''
    } else {
      if (decimalPlaces === undefined || isNaN(decimalPlaces)) {
        return n.toString()
      } else {
        return n.toFixed(decimalPlaces)
      }
    }
  }
  render() {
    const { maxValue, minValue, decimalPlaces, onChange, ...rest } = this.props
    return (
      <input
        type="text"
        {...rest}
        value={this.state.stringValue}
        onChange={({ currentTarget: { value } }: FormEvent) => {
          this.setState({ stringValue: value })
        }}
        onBlur={() => {
          const number = this.constrainedNumber(
            parseFloat(this.state.stringValue)
          )
          const stringValue = this.constrainedString(number)
          this.setState({ stringValue })
          if (number !== null) {
            onChange(number)
          }
        }}
      />
    )
  }
}

export default NumberInput
