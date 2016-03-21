define([
  'marionette',
  'text!./queries-results-layout.hbs',
  './queries.view',
  './results.view',
], function (Marionette, layout, QueriesView, ResultsView) {

  var QueryResultsView = Marionette.LayoutView.extend({
    template: '<div id="queries"></div><div id="results"></div>',
    className: 'height-full queries-results',
    regions: {
      queries: '#queries',
      results: '#results',
    },
    onRender: function () {
      this.queries.show(new QueriesView({ collection: this.model.get('searches') }))
      //this.results.show(new ResultsView({ collection: this.searches.getResults() }))
    }
  })

  return QueryResultsView
})
