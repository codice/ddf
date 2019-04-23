const _ = require('underscore');
const Backbone = require('backbone');
const Query = require('./Query.js');
const cql = require('../cql.js');
const user = require('../../component/singletons/user-instance.js');
const moment = require('moment');
require('backbone-associations')
const WorkspaceModel = require('./Workspace.js');

module.exports = Backbone.Collection.extend({
  model: WorkspaceModel,
  url: './internal/workspaces',
  useAjaxSync: true,
  fetched: false,
  handleSync: function() {
    this.fetched = true
  },
  initialize: function() {
    this.listenTo(this, 'sync', this.handleSync)
    this.handleUserChange()
    this.listenTo(user, 'change', this.handleUserChange)
    const collection = this;
    collection.on('add', function(workspace) {
      workspace.on('change:lastModifiedDate', function() {
        collection.sort()
      })
    })
    this.listenTo(this, 'add', this.tagGuestWorkspace)
  },
  handleUserChange: function() {
    this.fetch({
      remove: false,
    })
  },
  tagGuestWorkspace: function(model) {
    if (this.isGuestUser() && model.isNew()) {
      model.set({
        localStorage: true,
      })
    }
  },
  isGuestUser: function() {
    return user.get('user').isGuestUser()
  },
  comparator: function(workspace) {
    return -moment(workspace.get('lastModifiedDate')).unix()
  },
  createWorkspace: function(title) {
    this.create({
      title: title || 'New Workspace',
    })
  },
  createWorkspaceWithQuery: function(queryModel) {
    this.create({
      title: 'New Workspace',
      queries: [queryModel],
    })
      .get('queries')
      .first()
      .startSearch()
  },
  createAdhocWorkspace: function(text) {
    let cqlQuery;
    let title = text;
    if (text.length === 0) {
      cqlQuery = "anyText ILIKE '%'"
      title = '*'
    } else {
      cqlQuery = cql.write({
        type: 'ILIKE',
        property: 'anyText',
        value: text,
      })
    }
    const queryForWorkspace = new Query.Model({
      title: title,
      cql: cqlQuery,
      type: 'text',
    });
    this.create({
      title: 'New Workspace',
      queries: [queryForWorkspace.toJSON()],
    })
      .get('queries')
      .first()
      .startSearch()
  },
  createLocalWorkspace: function() {
    const queryForWorkspace = new Query.Model({
      title: 'Example Local',
      federation: 'local',
      excludeUnnecessaryAttributes: false,
      cql: "anyText ILIKE '%'",
      type: 'basic',
    });
    this.create({
      title: 'Template Local',
      queries: [queryForWorkspace.toJSON()],
    })
      .get('queries')
      .first()
      .startSearch()
  },
  createAllWorkspace: function() {
    const queryForWorkspace = new Query.Model({
      title: 'Example Federated',
      federation: 'enterprise',
      excludeUnnecessaryAttributes: false,
      cql: "anyText ILIKE '%'",
      type: 'basic',
    });
    this.create({
      title: 'Template Federated',
      queries: [queryForWorkspace.toJSON()],
    })
      .get('queries')
      .first()
      .startSearch()
  },
  createGeoWorkspace: function() {
    const queryForWorkspace = new Query.Model({
      title: 'Example Location',
      excludeUnnecessaryAttributes: false,
      cql:
        "anyText ILIKE '%' AND INTERSECTS(anyGeo, POLYGON((-130.7514 20.6825, -130.7514 44.5780, -65.1230 44.5780, -65.1230 20.6825, -130.7514 20.6825)))",
      type: 'basic',
    });
    this.create({
      title: 'Template Location',
      queries: [queryForWorkspace.toJSON()],
    })
      .get('queries')
      .first()
      .startSearch()
  },
  createLatestWorkspace: function() {
    const queryForWorkspace = new Query.Model({
      title: 'Example Temporal',
      excludeUnnecessaryAttributes: false,
      cql:
        'anyText ILIKE \'%\' AND ("created" AFTER ' +
        moment()
          .subtract(1, 'days')
          .toISOString() +
        ')',
      type: 'basic',
    });
    this.create({
      title: 'Template Temporal',
      queries: [queryForWorkspace.toJSON()],
    })
      .get('queries')
      .first()
      .startSearch()
  },
  duplicateWorkspace: function(workspace) {
    let duplicateWorkspace = _.pick(workspace.toJSON(), 'title', 'queries')
    duplicateWorkspace.queries = duplicateWorkspace.queries.map(query =>
      _.omit(query, 'isLocal', 'id')
    )
    this.create(duplicateWorkspace)
  },
  saveAll: function() {
    this.forEach(function(workspace) {
      if (!workspace.isSaved()) {
        workspace.save()
      }
    })
  },
  saveLocalWorkspaces: function() {
    const localWorkspaces = this.chain()
      .filter(function(workspace) {
        return workspace.get('localStorage')
      })
      .reduce(function(blob, workspace) {
        blob[workspace.get('id')] = workspace.toJSON()
        return blob
      }, {})
      .value();

    window.localStorage.setItem('workspaces', JSON.stringify(localWorkspaces))
  },
  convert2_10Format: function(localWorkspaceJSON) {
    if (localWorkspaceJSON.constructor === Array) {
      return localWorkspaceJSON.reduce(function(blob, workspace) {
        blob[workspace.id] = workspace
        return blob
      }, {})
    } else {
      return localWorkspaceJSON
    }
  },
  getLocalWorkspaces: function() {
    const localWorkspaces = window.localStorage.getItem('workspaces') || '{}';
    try {
      return this.convert2_10Format(JSON.parse(localWorkspaces))
    } catch (e) {
      console.error('Failed to parse local workspaces.', localWorkspaces)
    }
    return {}
  },
  // override parse to merge server response with local storage
  parse: function(resp) {
    const localWorkspaces = _.map(this.getLocalWorkspaces());
    return resp.concat(localWorkspaces)
  },
})
