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
import BooleanInput from './filter-boolean-input'
import LocationInput from './filter-location-input'
import { FloatInput, IntegerInput, RangeInput } from './filter-number-inputs'
import {
  DateInput,
  RelativeTimeInput,
  BetweenTimeInput,
} from './filter-date-inputs'
import { TextInput, NearInput, EnumInput } from './filter-text-inputs'
import { isIntegerType } from '../filterHelper'

export const determineInput = (
  comparator,
  type,
  suggestions,
  value,
  onChange
) => {
  const props = { value, onChange }
  switch (comparator) {
    case 'IS EMPTY':
      return null
    case 'NEAR':
      return <NearInput {...props} />
    case 'BETWEEN':
      return <BetweenTimeInput {...props} />
    case 'RELATIVE':
      return <RelativeTimeInput {...props} />
    case 'RANGE':
      props.isInteger = isIntegerType(type)
      return <RangeInput {...props} />
  }

  switch (type) {
    case 'BOOLEAN':
      return <BooleanInput {...props} />
    case 'DATE':
      return <DateInput {...props} />
    case 'LOCATION':
      return <LocationInput {...props} />
    case 'FLOAT':
      return <FloatInput {...props} />
    case 'INTEGER':
      return <IntegerInput {...props} />
  }

  if (suggestions && suggestions.length > 0) {
    props.suggestions = suggestions
    props.matchCase = ['MATCHCASE', '='].includes(comparator)
    return <EnumInput {...props} />
  }
  return <TextInput {...props} />
}
