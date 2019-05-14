/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import {
  Restrictions,
  Security,
} from '../../react-component/utils/security/security'

const _ = require('underscore')
const _get = require('lodash.get')
const wreqr = require('../wreqr.js')
const Backbone = require('backbone')
const properties = require('../properties.js')
const Alert = require('./Alert')
const Common = require('../Common.js')
const UploadBatch = require('./UploadBatch.js')
const announcement = require('../../component/announcement/index.jsx')
const BlackListItem = require('./BlacklistItem.js')
const moment = require('moment-timezone')
const Theme = require('./Theme.js')
const ThemeUtils = require('../ThemeUtils.js')
const QuerySettings = require('./QuerySettings.js')
require('backbone-associations')

const User = {}

User.updateMapLayers = function(layers) {
  const providers = properties.imageryProviders
  const exclude = ['id', 'label', 'alpha', 'show', 'order']
  const equal = (a, b) => _.isEqual(_.omit(a, exclude), _.omit(b, exclude))

  const layersToRemove = layers.filter(model => {
    const found = providers.some(provider => equal(provider, model.toJSON()))
    return !found && !model.get('userRemovable')
  })

  layers.remove(layersToRemove)

  providers.forEach(provider => {
    const found = layers
      .toArray()
      .some(model => equal(provider, model.toJSON()))

    if (!found) {
      layers.add(new User.MapLayer(provider, { parse: true }))
    }
  })
}

User.MapLayer = Backbone.AssociatedModel.extend({
  defaults: function() {
    return {
      alpha: 0.5,
      show: true,
      id: Common.generateUUID(),
    }
  },
  blacklist: ['warning'],
  toJSON: function(options) {
    return _.omit(this.attributes, this.blacklist)
  },
  shouldShowLayer: function() {
    return this.get('show') && this.get('alpha') > 0
  },
  parse: function(resp) {
    const layer = _.clone(resp)
    layer.label = 'Type: ' + layer.type
    if (layer.layer) {
      layer.label += ' Layer: ' + layer.layer
    }
    if (layer.layers) {
      layer.label += ' Layers: ' + layer.layers.join(', ')
    }
    return layer
  },
})

User.MapLayers = Backbone.Collection.extend({
  model: User.MapLayer,
  defaults: function() {
    return _.map(_.values(properties.imageryProviders), function(layerConfig) {
      return new User.MapLayer(layerConfig, { parse: true })
    })
  },
  initialize: function(models) {
    if (!models || models.length === 0) {
      this.set(this.defaults())
    }
  },
  comparator: function(model) {
    return model.get('order')
  },
  getMapLayerConfig: function(url) {
    return this.findWhere({ url: url })
  },
  savePreferences: function() {
    this.parents[0].savePreferences()
  },
})

User.Preferences = Backbone.AssociatedModel.extend({
  useAjaxSync: true,
  url: './internal/user/preferences',
  defaults: function() {
    return {
      id: 'preferences',
      mapLayers: new User.MapLayers(),
      resultDisplay: 'List',
      resultPreview: ['modified'],
      resultFilter: undefined,
      resultSort: undefined,
      'inspector-summaryShown': [],
      'inspector-summaryOrder': [],
      'inspector-detailsOrder': ['title', 'created', 'modified', 'thumbnail'],
      'inspector-detailsHidden': [],
      homeFilter: 'Owned by anyone',
      homeSort: 'Last modified',
      homeDisplay: 'Grid',
      alerts: [],
      alertPersistence: true, // persist across sessions by default
      alertExpiration: 2592000000, // 1 month in milliseconds
      resultBlacklist: [],
      visualization: '3dmap',
      columnHide: [],
      columnOrder: ['title', 'created', 'modified', 'thumbnail'],
      uploads: [],
      fontSize: ThemeUtils.getFontSize(_get(properties, 'zoomPercentage', 100)),
      resultCount: properties.resultCount,
      dateTimeFormat: Common.getDateTimeFormats()['ISO'],
      timeZone: Common.getTimeZones()['UTC'],
      coordinateFormat: 'degrees',
      goldenLayout: undefined,
      goldenLayoutUpload: undefined,
      goldenLayoutMetacard: undefined,
      goldenLayoutAlert: undefined,
      theme: new Theme(),
      animation: true,
      hoverPreview: true,
      querySettings: new QuerySettings(),
      mapHome: undefined,
    }
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'mapLayers',
      relatedModel: User.MapLayer,
      collectionType: User.MapLayers,
    },
    {
      type: Backbone.Many,
      key: 'alerts',
      relatedModel: Alert,
    },
    {
      type: Backbone.Many,
      key: 'uploads',
      relatedModel: UploadBatch,
    },
    {
      type: Backbone.Many,
      key: 'resultBlacklist',
      relatedModel: BlackListItem,
    },
    {
      type: Backbone.One,
      key: 'theme',
      relatedModel: Theme,
    },
    {
      type: Backbone.One,
      key: 'querySettings',
      relatedModel: QuerySettings,
    },
  ],
  initialize: function() {
    this.handleAlertPersistence()
    this.handleResultCount()
    this.listenTo(wreqr.vent, 'alerts:add', this.addAlert)
    this.listenTo(wreqr.vent, 'uploads:add', this.addUpload)
    this.listenTo(wreqr.vent, 'preferences:save', this.savePreferences)
    this.listenTo(this.get('alerts'), 'remove', this.savePreferences)
    this.listenTo(this.get('uploads'), 'remove', this.savePreferences)
    this.listenTo(this, 'change:visualization', this.savePreferences)
    this.listenTo(this, 'change:fontSize', this.savePreferences)
    this.listenTo(this, 'change:goldenLayout', this.savePreferences)
    this.listenTo(this, 'change:goldenLayoutUpload', this.savePreferences)
    this.listenTo(this, 'change:goldenLayoutMetacard', this.savePreferences)
    this.listenTo(this, 'change:goldenLayoutAlert', this.savePreferences)
    this.listenTo(this, 'change:mapHome', this.savePreferences)
  },
  handleRemove: function() {
    this.savePreferences()
  },
  addUpload: function(upload) {
    this.get('uploads').add(upload)
    this.savePreferences()
  },
  addAlert: function(alertDetails) {
    this.get('alerts').add(alertDetails)
    this.savePreferences()
  },
  savePreferences: function() {
    const currentPrefs = this.toJSON()
    if (_.isEqual(currentPrefs, this.lastSaved)) {
      return
    }
    if (this.parents[0].isGuestUser()) {
      window.localStorage.setItem('preferences', JSON.stringify(currentPrefs))
    } else {
      this.save(currentPrefs, {
        drop: true,
        withoutSet: true,
        customErrorHandling: true,
        success: function() {
          this.lastSaved = currentPrefs
        }.bind(this),
        error: function() {
          announcement.announce({
            title: 'Issue Authorizing Request',
            message:
              'You appear to have been logged out.  Please sign in again.',
            type: 'error',
          })
        }.bind(this),
      })
    }
  },
  resetBlacklist: function() {
    this.set('resultBlacklist', [])
  },
  handleResultCount: function() {
    this.set(
      'resultCount',
      Math.min(properties.resultCount, this.get('resultCount'))
    )
  },
  handleAlertPersistence: function() {
    if (!this.get('alertPersistence')) {
      this.get('alerts').reset()
      this.get('uploads').reset()
    } else {
      const expiration = this.get('alertExpiration')
      this.removeExpiredAlerts(expiration)
      this.removeExpiredUploads(expiration)
    }
  },
  removeExpiredAlerts: function(expiration) {
    const expiredAlerts = this.get('alerts').filter(function(alert) {
      const recievedAt = alert.getTimeComparator()
      return Date.now() - recievedAt > expiration
    })
    this.get('alerts').remove(expiredAlerts)
  },
  removeExpiredUploads: function(expiration) {
    const expiredUploads = this.get('uploads').filter(function(upload) {
      const recievedAt = upload.getTimeComparator()
      return Date.now() - recievedAt > expiration
    })
    this.get('uploads').remove(expiredUploads)
  },
  getSummaryShown: function() {
    return this.get('inspector-summaryShown')
  },
  getHoverPreview: function() {
    return this.get('hoverPreview')
  },
  getQuerySettings: function() {
    return this.get('querySettings')
  },
  parse: function(data, options) {
    if (options && options.drop) {
      return {}
    }
    return data
  },
})

User.Model = Backbone.AssociatedModel.extend({
  defaults: function() {
    return {
      id: 'user',
      preferences: new User.Preferences(),
      isGuest: true,
      username: 'guest',
      userid: 'guest',
      roles: ['guest'],
    }
  },
  relations: [
    {
      type: Backbone.One,
      key: 'preferences',
      relatedModel: User.Preferences,
    },
  ],
  getEmail() {
    return this.get('email')
  },
  getUserId() {
    return this.get('userid')
  },
  getUserName() {
    return this.get('username')
  },
  isGuestUser: function() {
    return this.get('isGuest')
  },
  getSummaryShown: function() {
    return this.get('preferences').getSummaryShown()
  },
  getHoverPreview: function() {
    return this.get('preferences').getHoverPreview()
  },
  getPreferences: function() {
    return this.get('preferences')
  },
  savePreferences: function() {
    this.get('preferences').savePreferences()
  },
  getQuerySettings: function() {
    return this.get('preferences').getQuerySettings()
  },
})

User.Response = Backbone.AssociatedModel.extend({
  useAjaxSync: true,
  url: './internal/user',
  relations: [
    {
      type: Backbone.One,
      key: 'user',
      relatedModel: User.Model,
    },
  ],
  fetched: false,
  initialize: function() {
    this.listenTo(this, 'sync', this.handleSync)
    this.set('user', new User.Model())
    this.fetch()
  },
  handleSync: function() {
    this.fetched = true
    this.get('user')
      .get('preferences')
      .handleAlertPersistence()
    this.get('user')
      .get('preferences')
      .handleResultCount()
  },
  getGuestPreferences: function() {
    try {
      return JSON.parse(window.localStorage.getItem('preferences')) || {}
    } catch (e) {
      return {}
    }
  },
  getEmail() {
    return this.get('user').getEmail()
  },
  getUserId() {
    return this.get('user').getUserId()
  },
  getRoles() {
    return this.get('user').get('roles')
  },
  getUserName() {
    return this.get('user').getUserName()
  },
  getPreferences: function() {
    return this.get('user').getPreferences()
  },
  savePreferences: function() {
    this.get('user').savePreferences()
  },
  getQuerySettings: function() {
    return this.get('user').getQuerySettings()
  },
  getSummaryShown: function() {
    return this.get('user').getSummaryShown()
  },
  getUserReadableDateTime: function(date) {
    return moment
      .tz(
        date,
        this.get('user')
          .get('preferences')
          .get('timeZone')
      )
      .format(
        this.get('user')
          .get('preferences')
          .get('dateTimeFormat')['datetimefmt']
      )
  },
  getHoverPreview: function() {
    return this.get('user').getHoverPreview()
  },
  isGuest: function() {
    return this.get('user').isGuestUser()
  },
  parse: function(body) {
    if (body.isGuest) {
      return {
        user: _.extend({ id: 'user' }, body, {
          preferences: _.extend(
            { id: 'preferences' },
            this.getGuestPreferences()
          ),
        }),
      }
    } else {
      _.extend(body.preferences, { id: 'preferences' })
      return {
        user: _.extend(
          {
            id: 'user',
          },
          body
        ),
      }
    }
  },
  canRead: function(metacard) {
    return new Security(Restrictions.from(metacard)).canRead(this)
  },
  canWrite: function(metacard) {
    return new Security(Restrictions.from(metacard)).canWrite(this)
  },
  canShare: function(metacard) {
    return new Security(Restrictions.from(metacard)).canShare(this)
  },
})

module.exports = User
