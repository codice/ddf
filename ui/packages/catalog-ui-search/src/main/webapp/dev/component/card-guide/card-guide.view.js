const template = require('./card-guide.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const BaseGuideView = require('../base-guide/base-guide.view.js')

const ResultItemView = require('../../../component/result-item/result-item.view.js')
const SelectionInterfaceModel = require('../../../component/selection-interface/selection-interface.model.js')
const QueryResultModel = require('../../../js/model/QueryResult.js')

const QueryItemView = require('../../../component/query-item/query-item.view.js')
const QueryModel = require('../../../js/model/Query.js')

module.exports = BaseGuideView.extend({
  template,
  tagName: CustomElements.register('dev-card-guide'),
  regions: {
    resultExample: '.example > .result',
    result2Example: '.example > .result2',
    queryExample: '.example > .query',
  },
  showComponents() {
    this.showResultExample()
    this.showResult2Example()
    this.showQueryExample()
  },
  showQueryExample() {
    this.queryExample.show(
      new QueryItemView({
        model: new QueryModel.Model(),
      })
    )
  },
  showResult2Example() {
    this.result2Example.show(
      new ResultItemView({
        model: new QueryResultModel({
          actions: [
            {
              description: 'example',
              id: 'example',
              title: 'example',
              url: 'https://example.com',
              displayName: 'example',
            },
          ],
          distance: null,
          hasThumbnail: false,
          isResourceLocal: true,
          metacard: {
            id: 'blah blah blah',
            cached: '2018-06-28T01:51:32.800+0000',
            properties: {
              title: 'Example Result',
              id: 'example',
              'metacard-tags': ['resource', 'VALID'],
              'validation-warnings': ['this isonly sort of wrong'],
              'source-id': 'banana land',
              'resource-download-url': 'https://example.com',
            },
          },
          relevance: 11,
        }),
        selectionInterface: new SelectionInterfaceModel(),
      })
    )
  },
  showResultExample() {
    this.resultExample.show(
      new ResultItemView({
        model: new QueryResultModel({
          actions: [
            {
              description: 'example',
              id: 'example',
              title: 'example',
              url: 'https://www.google.com',
              displayName: 'example',
            },
          ],
          distance: null,
          hasThumbnail: false,
          isResourceLocal: true,
          metacard: {
            id: 'blah blah blah',
            cached: '2018-06-28T01:51:32.800+0000',
            properties: {
              title: 'Example Result',
              id: 'example',
              'metacard-tags': ['deleted', 'VALID'],
              'validation-errors': ['wow this is way wrong'],
              'validation-warnings': ['this isonly sort of wrong'],
              'source-id': 'banana land',
            },
          },
          relevance: 11,
        }),
        selectionInterface: new SelectionInterfaceModel(),
      })
    )
  },
})
