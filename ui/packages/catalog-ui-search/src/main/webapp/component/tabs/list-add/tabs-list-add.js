const Tabs = require('../tabs')
const IngestView = require('../../ingest/ingest.view.js')
const BuilderView = require('../../builder/builder.view.js')

module.exports = Tabs.extend({
  defaults: {
    tabs: {
      Import: IngestView,
      Manual: BuilderView,
    },
  },
  initialize(options) {},
})
