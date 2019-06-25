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
const QueryAdhoc = require('../../component/query-adhoc/query-adhoc.view.js')
const QueryBasic = require('../../component/query-basic/query-basic.view.js')
const QueryAdvanced = require('../../component/query-advanced/query-advanced.view.js')

export default [
  { id: 'text', title: 'Text Search', view: QueryAdhoc },
  { id: 'basic', title: 'Basic Search', view: QueryBasic },
  {
    id: 'advanced',
    title: 'Advanced Search',
    view: QueryAdvanced,
    options: {
      isForm: false,
      isFormBuilder: false,
      isAdd: true,
    },
  },
]
