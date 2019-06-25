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
const cql = require('./cql.js')
const CQLUtils = require('./CQLUtils.js')
function buildCacheSourcesCql(sources) {
  return {
    type: 'OR',
    filters: sources.filter(source => source !== 'cache').map(source => ({
      property: '"metacard_source"',
      type: '=',
      value: source,
    })),
  }
}

function limitCacheSources(cql, sources) {
  return {
    type: 'AND',
    filters: [cql, buildCacheSourcesCql(sources)],
  }
}

module.exports = {
  trimCacheSources(cqlString, sources) {
    return CQLUtils.sanitizeGeometryCql(
      '(' +
        cql.write(
          limitCacheSources(cql.simplify(cql.read(cqlString)), sources)
        ) +
        ')'
    )
  },
}
