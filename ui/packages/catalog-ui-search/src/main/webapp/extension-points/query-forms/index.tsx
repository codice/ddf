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
