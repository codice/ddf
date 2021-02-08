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
const metacardDefinitions = require('../../component/singletons/metacard-definitions')

const getSuggestions = async (attribute: any, suggester: any) => {
  console.log('here')
  debugger
  let suggestions = []
  if (metacardDefinitions.enums[attribute]) {
    suggestions = metacardDefinitions.enums[attribute].map(
      (suggestion: any) => {
        return { label: suggestion, value: suggestion }
      }
    )
  } else if (suggester) {
    suggestions = (await suggester(
      metacardDefinitions.metacardTypes[attribute]
    )).map((suggestion: any) => ({
      label: suggestion,
      value: suggestion,
    }))
  }

  suggestions.sort((a: any, b: any) =>
    a.label.toLowerCase().localeCompare(b.label.toLowerCase())
  )
  console.log(suggestions)

  return suggestions
}

export default getSuggestions
