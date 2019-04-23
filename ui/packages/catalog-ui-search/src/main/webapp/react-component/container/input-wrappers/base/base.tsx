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
import { Subtract } from '../../../../typescript'

import * as React from 'react'
import MarionetteRegionContainer from '../../marionette-region-container'
import withListenTo, { WithBackboneProps } from '../../backbone-container'
import { hot } from 'react-hot-loader'
const PropertyView = require('../../../../component/property/property.view.js')
const PropertyModel = require('../../../../component/property/property.js')

export enum Type {
  autocomplete = 'AUTOCOMPLETE',
  range = 'RANGE',
  date = 'DATE',
  location = 'LOCATION',
  thumbnail = 'BINARY',
  checkbox = 'BOOLEAN',
  number = 'INTEGER',
  geometry = 'GEOMETRY',
  color = 'COLOR',
  inputWithParam = 'NEAR',
  text = 'STRING',
  textarea = 'TEXTAREA',
  password = 'PASSWORD',
}

/**
 * Meant to be available on all downstream implementations using Base.
 * However, we need to subtract these from the HTMLProps first so we don't confuse types.
 */
type DownstreamProps = {
  label?: string
  showLabel?: boolean
  showValidationIssues?: boolean
  initializeToDefault?: boolean
  required?: boolean
  showRequiredWarning?: boolean
  transformValue?: boolean
  className?: string
  style?: React.CSSProperties
}

/**
 * For use by downstream users of Base directly, not necessarily exposed to developers.
 * Basically, the implementation should be passing these in, not the person using the
 * implementation.  For instance, I use an enum, but all I see is multi as an option,
 * not enumMulti since we're wrapping it.
 */
type MyProps = {
  onChange?: (value: any) => void
  placeholder?: string
  value?: any[]
  values?: object
  enumeration?: any[]
  radio?: any[]
  description?: string
  readOnly?: boolean
  id?: string
  isEditing?: boolean
  bulk?: boolean
  multivalued?: boolean
  type?: Type
  param?: string
  url?: string
  minimumInputLength?: number
  min?: number
  max?: number
  units?: string
  enumFiltering?: boolean
  enumCustom?: boolean
  enumMulti?: boolean
  valueTransformer?: (value: any[]) => any
}

/**
 * Meant to avoid issues with value being an html prop for instance.  We want to expose
 * things like onKeyUp without taking along the rest of the kitchen sink.
 */
type SubsetHTML = Subtract<
  React.HTMLProps<HTMLDivElement>,
  DownstreamProps & MyProps
>

export type BaseProps = DownstreamProps & SubsetHTML

export const destructureBaseProps = (
  props: object & BaseProps
): BaseProps & { htmlProps: SubsetHTML } => {
  const {
    label,
    showLabel,
    showValidationIssues,
    initializeToDefault,
    required,
    showRequiredWarning,
    transformValue,
    ...htmlProps
  } = props
  return {
    label,
    showLabel,
    showValidationIssues,
    initializeToDefault,
    required,
    showRequiredWarning,
    transformValue,
    htmlProps,
  }
}

type Props = {
  htmlProps?: SubsetHTML
} & MyProps &
  WithBackboneProps &
  BaseProps

type State = {
  value?: any[]
}

class BasePropertyWrapper extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    const {
      value = [],
      values = {},
      enumeration,
      radio,
      label,
      description = '',
      readOnly = false,
      id = '',
      isEditing = true,
      bulk = false,
      multivalued = false,
      type = Type.text,
      showValidationIssues = true,
      showLabel = true,
      initializeToDefault = false,
      required = false,
      showRequiredWarning = false,
      transformValue = true,
      param,
      placeholder = 'Enter a region, country, or city',
      url = './internal/geofeature/suggestions',
      minimumInputLength = 2,
      min = 1,
      max = 100,
      units = '%',
      enumFiltering,
      enumCustom,
      enumMulti,
    } = props
    this.state = {
      value,
    }
    this.propertyModel = new PropertyModel({
      value: this.state.value,
      values,
      enum: enumeration,
      radio,
      label,
      description,
      readOnly,
      id,
      isEditing,
      bulk,
      multivalued,
      type,
      showValidationIssues,
      showLabel,
      initializeToDefault,
      required,
      showRequiredWarning,
      transformValue,
      param,
      placeholder,
      url,
      minimumInputLength,
      min,
      max,
      units,
      enumFiltering,
      enumCustom,
      enumMulti,
    })
  }
  propertyModel: any
  componentDidMount() {
    if (this.props.onChange !== undefined) {
      this.props.listenTo(
        this.propertyModel,
        'change:value',
        this.modelOnChange.bind(this)
      )
    }
  }
  defaultValueTransformer(value: any[]) {
    return value[0]
  }
  modelOnChange() {
    if (this.props.onChange !== undefined) {
      const transformValue =
        this.props.valueTransformer || this.defaultValueTransformer
      this.props.onChange(transformValue(this.propertyModel.getValue()))
    }
  }
  render() {
    const { className, style } = this.props
    return (
      <MarionetteRegionContainer
        view={PropertyView}
        viewOptions={() => ({
          model: this.propertyModel,
        })}
        className={className}
        style={style}
        {...this.props.htmlProps as any}
      />
    )
  }
}

export default hot(module)(withListenTo(BasePropertyWrapper))
