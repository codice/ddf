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
function matchesFilter(filterValue, str, matchcase) {
  filterValue = getAppropriateString(filterValue, matchcase)
  var reg = new RegExp('\\b' + filterValue + '.*')
  if (getAppropriateString(str, matchcase).match(reg) !== null) {
    return true
  }
  if (
    wordStartsWithFilter(getWords(str, matchcase), filterValue) === undefined
  ) {
    return false
  }
  return true
}
function getAppropriateString(str, matchcase) {
  str = str.toString()
  return matchcase === true ? str : str.toLowerCase()
}
function getWords(str, matchcase) {
  //Handle camelcase
  str = str.replace(/([A-Z])/g, ' $1')
  str = getAppropriateString(str, matchcase)
  //Handle dashes, dots, and spaces
  return str.split(/[-\.\s]+/)
}
function wordStartsWithFilter(words, filter) {
  return words.find(function(word) {
    return word.indexOf(filter) === 0
  })
}

export { matchesFilter, getAppropriateString }
