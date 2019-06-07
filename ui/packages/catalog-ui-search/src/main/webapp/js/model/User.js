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
  defaults() {
    return {
      alpha: 0.5,
      show: true,
      id: Common.generateUUID(),
    }
  },
  blacklist: ['warning'],
  toJSON(options) {
    return _.omit(this.attributes, this.blacklist)
  },
  shouldShowLayer() {
    return this.get('show') && this.get('alpha') > 0
  },
  parse(resp) {
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
  defaults() {
    return _.map(
      _.values(properties.imageryProviders),
      layerConfig => new User.MapLayer(layerConfig, { parse: true })
    )
  },
  initialize(models) {
    if (!models || models.length === 0) {
      this.set(this.defaults())
    }
  },
  comparator(model) {
    return model.get('order')
  },
  getMapLayerConfig(url) {
    return this.findWhere({ url })
  },
  savePreferences() {
    this.parents[0].savePreferences()
  },
})

User.Preferences = Backbone.AssociatedModel.extend({
  useAjaxSync: true,
  url: './internal/user/preferences',
  defaults() {
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
  initialize() {
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
  handleRemove() {
    this.savePreferences()
  },
  addUpload(upload) {
    this.get('uploads').add(upload)
    this.savePreferences()
  },
  addAlert(alertDetails) {
    this.get('alerts').add(alertDetails)
    this.savePreferences()
  },
  savePreferences() {
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
        success: () => {
          this.lastSaved = currentPrefs
        },
        error: () => {
          announcement.announce({
            title: 'Issue Authorizing Request',
            message:
              'You appear to have been logged out.  Please sign in again.',
            type: 'error',
          })
        },
      })
    }
  },
  resetBlacklist() {
    this.set('resultBlacklist', [])
  },
  handleResultCount() {
    this.set(
      'resultCount',
      Math.min(properties.resultCount, this.get('resultCount'))
    )
  },
  handleAlertPersistence() {
    if (!this.get('alertPersistence')) {
      this.get('alerts').reset()
      this.get('uploads').reset()
    } else {
      const expiration = this.get('alertExpiration')
      this.removeExpiredAlerts(expiration)
      this.removeExpiredUploads(expiration)
    }
  },
  removeExpiredAlerts(expiration) {
    const expiredAlerts = this.get('alerts').filter(alert => {
      const recievedAt = alert.getTimeComparator()
      return Date.now() - recievedAt > expiration
    })
    this.get('alerts').remove(expiredAlerts)
  },
  removeExpiredUploads(expiration) {
    const expiredUploads = this.get('uploads').filter(upload => {
      const recievedAt = upload.getTimeComparator()
      return Date.now() - recievedAt > expiration
    })
    this.get('uploads').remove(expiredUploads)
  },
  getSummaryShown() {
    return this.get('inspector-summaryShown')
  },
  getHoverPreview() {
    return this.get('hoverPreview')
  },
  getQuerySettings() {
    return this.get('querySettings')
  },
  parse(data, options) {
    if (options && options.drop) {
      return {}
    }
    return data
  },
})

User.Model = Backbone.AssociatedModel.extend({
  defaults() {
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
  isGuestUser() {
    return this.get('isGuest')
  },
  getSummaryShown() {
    return this.get('preferences').getSummaryShown()
  },
  getHoverPreview() {
    return this.get('preferences').getHoverPreview()
  },
  getPreferences() {
    return this.get('preferences')
  },
  savePreferences() {
    this.get('preferences').savePreferences()
  },
  getQuerySettings() {
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
  initialize() {
    this.listenTo(this, 'sync', this.handleSync)
    this.set('user', new User.Model())
    this.fetch()
  },
  handleSync() {
    this.fetched = true
    this.get('user')
      .get('preferences')
      .handleAlertPersistence()
    this.get('user')
      .get('preferences')
      .handleResultCount()
  },
  getGuestPreferences() {
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
  getPreferences() {
    return this.get('user').getPreferences()
  },
  savePreferences() {
    this.get('user').savePreferences()
  },
  getQuerySettings() {
    return this.get('user').getQuerySettings()
  },
  getSummaryShown() {
    return this.get('user').getSummaryShown()
  },
  getUserReadableDateTime(date) {
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
  getHoverPreview() {
    return this.get('user').getHoverPreview()
  },
  isGuest() {
    return this.get('user').isGuestUser()
  },
  parse(body) {
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
  canRead(metacard) {
    return new Security(Restrictions.from(metacard)).canRead(this)
  },
  canWrite(metacard) {
    return new Security(Restrictions.from(metacard)).canWrite(this)
  },
  canShare(metacard) {
    return new Security(Restrictions.from(metacard)).canShare(this)
  },
})

module.exports = User
