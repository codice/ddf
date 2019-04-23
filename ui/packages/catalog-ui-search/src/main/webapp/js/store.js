const Backbone = require('backbone')
const WorkspaceCollection = require('./model/Workspace.collection.js')
const Content = require('../component/content/content.js')
const router = require('../component/router/router.js')

module.exports = new (Backbone.Model.extend({
  initialize: function() {
    this.set('content', new Content())
    this.set('workspaces', new WorkspaceCollection())

    window.onbeforeunload = function() {
      const unsaved = this.get('workspaces')
        .chain()
        .map(function(workspace) {
          if (!workspace.isSaved()) {
            return workspace.get('title')
          }
        })
        .filter(function(title) {
          return title !== undefined
        })
        .value();

      if (unsaved.length > 0) {
        return (
          'Do you really want to close? Unsaved workspaces: ' +
          unsaved.join(', ')
        )
      }
    }.bind(this)

    this.listenTo(this.get('workspaces'), 'remove', function() {
      const currentWorkspace = this.getCurrentWorkspace();
      if (currentWorkspace && !this.get('workspaces').get(currentWorkspace)) {
        this.get('content').set('currentWorkspace', undefined)
      }
    })
    this.listenTo(
      this.get('content'),
      'change:currentWorkspace',
      this.handleWorkspaceChange
    )
    this.listenTo(router, 'change', this.handleRoute)
    this.handleRoute()
  },
  handleRoute() {
    if (router.toJSON().name === 'openWorkspace') {
      const workspaceId = router.get('args')[0]
      if (this.get('workspaces').get(workspaceId) !== undefined) {
        this.setCurrentWorkspaceById(workspaceId)
      } else {
        router.notFound()
      }
    }
  },
  clearOtherWorkspaces: function(workspaceId) {
    this.get('workspaces').forEach(function(workspaceModel) {
      if (workspaceId !== workspaceModel.id) {
        workspaceModel.clearResults()
      }
    })
  },
  handleWorkspaceChange: function() {
    if (this.get('content').changedAttributes().currentWorkspace) {
      const previousWorkspace = this.get('content').previousAttributes()
        .currentWorkspace;
      if (
        previousWorkspace &&
        previousWorkspace.id !== this.get('content').get('currentWorkspace').id
      ) {
        previousWorkspace.clearResults()
      }
    }
  },
  getWorkspaceById: function(workspaceId) {
    return this.get('workspaces').get(workspaceId)
  },
  setCurrentWorkspaceById: function(workspaceId) {
    this.get('content').set(
      'currentWorkspace',
      this.get('workspaces').get(workspaceId)
    )
  },
  getCurrentWorkspace: function() {
    return this.get('content').get('currentWorkspace')
  },
  getCurrentQueries: function() {
    return this.getCurrentWorkspace().get('queries')
  },
  setQueryById: function(queryId) {
    const queryRef = this.getCurrentQueries().get(queryId);
    this.setQueryByReference(queryRef.clone())
  },
  setQueryByReference: function(queryRef) {
    this.get('content').set('query', queryRef)
  },
  getQuery: function() {
    return this.get('content').get('query')
  },
  getQueryById: function(queryId) {
    return this.getCurrentQueries().get(queryId)
  },
  getSelectedResults: function() {
    return this.get('content').get('selectedResults')
  },
  clearSelectedResults: function() {
    this.getSelectedResults().reset()
  },
  addSelectedResult: function(metacard) {
    this.getSelectedResults().add(metacard)
  },
  removeSelectedResult: function(metacard) {
    this.getSelectedResults().remove(metacard)
  },
  getActiveSearchResultsAttributes: function() {
    return this.get('content').getActiveSearchResultsAttributes()
  },
  getActiveSearchResults: function() {
    return this.get('content').getActiveSearchResults()
  },
  setActiveSearchResults: function(results) {
    this.get('content').setActiveSearchResults(results)
  },
  addToActiveSearchResults: function(results) {
    this.get('content').addToActiveSearchResults(results)
  },
  saveCurrentWorkspace: function() {
    this.getCurrentWorkspace().save()
  },
  setCurrentQuery: function(query) {
    this.get('content').setCurrentQuery(query)
  },
  getCurrentQuery: function() {
    return this.get('content').getCurrentQuery()
  },
  setWorkspaceRestrictions: function(workspaceId, restrictions) {
    const metacard = this.getWorkspaceById(workspaceId)
    restrictions.forEach(function(restriction) {
      metacard.attributes[restriction.attribute] = restriction.values
    })
  },
}))()
