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
import { expect } from 'chai'

var filterHelper = require('./filterHelper.js')

describe('fitler attrs', () => {
  it('filter anyText', () => {
    var str = 'anyText'
    var matchcase = false

    var filterValue = 'w'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 't'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'te'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'tex'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'text'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'a'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'an'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'any'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'anyt'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'anyT'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'anyG'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'anyText'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )
  })

  it('filter aString.WithDots-andDashes', () => {
    var str = 'aString.WithDots-andDashes'
    var matchcase = false

    var filterValue = 'z'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'w'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'with'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'withdots'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'withDots'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'withDots-and'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'aString.WithDots-andDashes'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )
  })

  it('filter ALLCAPSSTRING', () => {
    var str = 'ALLCAPSSTRING'
    var matchcase = false

    var filterValue = 'z'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'C'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    // A limitation to full capitalized attrs is they can't be filtered by words
    var filterValue = 'CA'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'all'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'allcaps'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'allcapsstring'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'ALLCAPSSTRING'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )
  })

  it('filter matches case', () => {
    var str = 'aCamelCasedString'
    var matchcase = true

    var filterValue = 'z'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'a'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'Camel'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'Cased'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'String'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'A'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'camel'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'cased'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'string'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'aCamelCasedString'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )
  })

  it('filter spaces', () => {
    var str = 'A few strings divided by spaces'
    var matchcase = false

    var filterValue = 'z'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'a'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'few'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'strings'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'divided'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'by'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'spaces'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'a strings by'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )

    var filterValue = 'strings divided by'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'a few strings divided by spaces'
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      true
    )

    var filterValue = 'a few strings divided by spaces '
    expect(filterHelper.matchesFilter(filterValue, str, matchcase)).to.equal(
      false
    )
  })
})
