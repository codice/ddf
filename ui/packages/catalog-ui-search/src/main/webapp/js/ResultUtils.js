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

const store = require('./store.js')
const alert = require('../component/alert/alert.js')
const _ = require('underscore')
const metacardDefinitions = require('../component/singletons/metacard-definitions.js')

module.exports = {
  refreshResult(result) {
    const id = result.get('metacard').id
    result.refreshData()
    store.get('workspaces').forEach(workspace => {
      workspace.get('queries').forEach(query => {
        if (query.get('result')) {
          query
            .get('result')
            .get('results')
            .forEach(result => {
              if (
                result
                  .get('metacard')
                  .get('properties')
                  .get('id') === id
              ) {
                result.refreshData()
              }
            })
        }
      })
    })
    alert
      .get('currentResult')
      .get('results')
      .forEach(result => {
        if (
          result
            .get('metacard')
            .get('properties')
            .get('id') === id
        ) {
          result.refreshData()
        }
      })
  },
  updateResults(results, response) {
    const attributeMap = response.reduce(
      (attributeMap, changes) =>
        changes.attributes.reduce((attrMap, chnges) => {
          attrMap[chnges.attribute] = metacardDefinitions.metacardTypes[
            chnges.attribute
          ].multivalued
            ? chnges.values
            : chnges.values[0]
          if (
            attrMap[chnges.attribute] &&
            attrMap[chnges.attribute].constructor === Array &&
            attrMap[chnges.attribute].length === 0
          ) {
            attrMap[chnges.attribute] = undefined
          }
          return attrMap
        }, attributeMap),
      {}
    )
    const unsetAttributes = []
    _.forEach(attributeMap, (value, key) => {
      if (
        value === undefined ||
        (value.constructor === Array && value.length === 0)
      ) {
        unsetAttributes.push(key)
        delete attributeMap[key]
      }
    })
    if (results.length === undefined) {
      results = [results]
    }
    const ids = results.map(result => result.get('metacard').id)
    results.forEach(metacard => {
      metacard
        .get('metacard')
        .get('properties')
        .set(attributeMap)
      unsetAttributes.forEach(attribute => {
        metacard
          .get('metacard')
          .get('properties')
          .unset(attribute)
      })
    })
    store.get('workspaces').forEach(workspace => {
      workspace.get('queries').forEach(query => {
        if (query.get('result')) {
          query
            .get('result')
            .get('results')
            .forEach(result => {
              if (
                ids.indexOf(
                  result
                    .get('metacard')
                    .get('properties')
                    .get('id')
                ) !== -1
              ) {
                result
                  .get('metacard')
                  .get('properties')
                  .set(attributeMap)
                unsetAttributes.forEach(attribute => {
                  result
                    .get('metacard')
                    .get('properties')
                    .unset(attribute)
                })
              }
            })
        }
      })
    })
    alert
      .get('currentResult')
      .get('results')
      .forEach(result => {
        if (
          ids.indexOf(
            result
              .get('metacard')
              .get('properties')
              .get('id')
          ) !== -1
        ) {
          result
            .get('metacard')
            .get('properties')
            .set(attributeMap)
          unsetAttributes.forEach(attribute => {
            result
              .get('metacard')
              .get('properties')
              .unset(attribute)
          })
        }
      })
  },
}
