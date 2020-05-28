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
import { ResultType } from '../Types'
const QueryResult = require('../QueryResult.js')
import { LazyQueryResults } from './LazyQueryResults'

const _ = require('underscore')
const Sources = require('../../../component/singletons/sources-instance.js')
const metacardDefinitions = require('../../../component/singletons/metacard-definitions.js')
const properties = require('../../properties.js')
const TurfMeta = require('@turf/meta')
const wkx = require('wkx')
const Common = require('../../Common.js')
import { matchesCql, matchesFilters } from './filter'
import { FilterType } from './types'

function getThumbnailAction(result: ResultType) {
  return result.actions.find(
    action => action.id === 'catalog.data.metacard.thumbnail'
  )
}

function cacheBustUrl(url: string): string {
  if (url && url.indexOf('_=') === -1) {
    let newUrl = url
    if (url.indexOf('?') >= 0) {
      newUrl += '&'
    } else {
      newUrl += '?'
    }
    newUrl += '_=' + Date.now()
    return newUrl
  }
  return url
}

function cacheBustThumbnail(plain: ResultType) {
  let url = plain.metacard.properties.thumbnail
  if (url) {
    plain.metacard.properties.thumbnail = cacheBustUrl(url)
  }
}

function humanizeResourceSize(plain: ResultType) {
  if (plain.metacard.properties['resource-size']) {
    plain.metacard.properties['resource-size'] = Common.getFileSize(
      plain.metacard.properties['resource-size']
    )
  }
}

/**
 * Add defaults, etc.  We need to make sure everything has a tag at the very least
 */
const transformPlain = ({
  plain,
}: {
  plain: LazyQueryResult['plain']
}): LazyQueryResult['plain'] => {
  if (!plain.metacard.properties['metacard-tags']) {
    plain.metacard.properties['metacard-tags'] = ['resource']
  }
  const thumbnailAction = getThumbnailAction(plain)
  if (thumbnailAction) {
    plain.metacard.properties.thumbnail = thumbnailAction.url
  }
  plain.metacardType = plain.metacard.properties['metacard-type']
  plain.metacard.id = plain.metacard.properties.id
  plain.id = plain.metacard.properties.id
  return plain
}

type SubscribableType = 'backboneCreated' | 'selected' | 'filtered'
type SubscriptionType = { [key: string]: () => void }
export class LazyQueryResult {
  ['subscriptionsToMe.backboneCreated']: { [key: string]: () => void };
  ['subscriptionsToMe.selected']: { [key: string]: () => void };
  ['subscriptionsToMe.filtered']: { [key: string]: () => void }
  subscribeTo({
    subscribableThing,
    callback,
  }: {
    subscribableThing: SubscribableType
    callback: () => void
  }) {
    const id = Math.random().toString()
    // @ts-ignore
    this[`subscriptionsToMe.${subscribableThing}`][id] = callback
    return () => {
      // @ts-ignore
      delete this[`subscriptionsToMe.${subscribableThing}`][id]
    }
  }
  _notifySubscribers(subscribableThing: SubscribableType) {
    // @ts-ignore
    const subscribers = this[
      `subscriptionsToMe.${subscribableThing}`
    ] as SubscriptionType
    Object.values(subscribers).forEach(callback => callback())
  }
  ['_notifySubscribers.backboneCreated']() {
    this._notifySubscribers('backboneCreated')
  }
  ['_notifySubscribers.selected']() {
    this._notifySubscribers('selected')
  }
  ['_notifySubscribers.filtered']() {
    this._notifySubscribers('filtered')
  }
  _turnOnDebouncing() {
    this['_notifySubscribers.backboneCreated'] = _.debounce(
      this['_notifySubscribers.backboneCreated'],
      1000
    )
    this['_notifySubscribers.selected'] = _.debounce(
      this['_notifySubscribers.selected'],
      1000
    )
    this['_notifySubscribers.filtered'] = _.debounce(
      this['_notifySubscribers.filtered'],
      1000
    )
  }
  index: number
  prev?: LazyQueryResult
  next?: LazyQueryResult
  parent?: LazyQueryResults
  plain: ResultType
  backbone?: any
  isResourceLocal: boolean
  type: 'query-result';
  ['metacard.id']: string
  isSelected: boolean
  isFiltered: boolean
  constructor(plain: ResultType) {
    this.type = 'query-result'
    this.plain = transformPlain({ plain })
    this.isResourceLocal = false || plain.isResourceLocal
    this['subscriptionsToMe.backboneCreated'] = {}
    this['subscriptionsToMe.selected'] = {}
    this['subscriptionsToMe.filtered'] = {}
    this['metacard.id'] = plain.metacard.properties.id
    this.isSelected = false
    this.isFiltered = false
    humanizeResourceSize(plain)
    cacheBustThumbnail(plain)
  }
  syncWithBackbone() {
    if (this.backbone) {
      this.plain = transformPlain({ plain: this.backbone.toJSON() })
      humanizeResourceSize(this.plain)
      cacheBustThumbnail(this.plain)
    }
  }
  isDownloadable(): boolean {
    return this.plain.metacard.properties['resource-download-url'] !== undefined
  }
  getPreview(): string {
    return this.plain.actions.filter(
      action => action.id === 'catalog.data.metacard.html.preview'
    )[0].url
  }
  hasPreview(): boolean {
    return (
      this.plain.actions.filter(
        action => action.id === 'catalog.data.metacard.html.preview'
      ).length > 0
    )
  }
  matchesFilters(filters: FilterType): boolean {
    return matchesFilters(this.plain.metacard, filters)
  }
  matchesCql(cql: string): boolean {
    return matchesCql(this.plain.metacard, cql)
  }
  isWorkspace(): boolean {
    return (
      this.plain.metacard.properties['metacard-tags'].indexOf('workspace') >= 0
    )
  }
  isResource(): boolean {
    return (
      this.plain.metacard.properties['metacard-tags'].indexOf('resource') >= 0
    )
  }
  isRevision(): boolean {
    return (
      this.plain.metacard.properties['metacard-tags'].indexOf('revision') >= 0
    )
  }
  isDeleted(): boolean {
    return (
      this.plain.metacard.properties['metacard-tags'].indexOf('deleted') >= 0
    )
  }
  isRemote(): boolean {
    return this.plain.metacard.properties['source-id'] !== Sources.localCatalog
  }
  hasGeometry(attribute?: any): boolean {
    return (
      _.filter(
        this.plain.metacard.properties,
        (_value: any, key: string) =>
          (attribute === undefined || attribute === key) &&
          metacardDefinitions.metacardTypes[key] &&
          metacardDefinitions.metacardTypes[key].type === 'GEOMETRY'
      ).length > 0
    )
  }
  getGeometries(attribute?: any): any {
    return _.filter(
      this.plain.metacard.properties,
      (_value: any, key: string) =>
        !properties.isHidden(key) &&
        (attribute === undefined || attribute === key) &&
        metacardDefinitions.metacardTypes[key] &&
        metacardDefinitions.metacardTypes[key].type === 'GEOMETRY'
    )
  }
  getPoints(attribute?: any): any {
    return this.getGeometries(attribute).reduce(
      (pointArray: any, wkt: any) =>
        pointArray.concat(
          TurfMeta.coordAll(wkx.Geometry.parse(wkt).toGeoJSON())
        ),
      []
    )
  }
  getMapActions() {
    return this.plain.actions.filter(
      action => action.id.indexOf('catalog.data.metacard.map.') === 0
    )
  }
  hasMapActions(): boolean {
    return this.getMapActions().length > 0
  }
  getExportActions() {
    const otherActions = this.getMapActions()
    return this.plain.actions
      .filter(action => action.title.indexOf('Export') === 0)
      .filter(action => otherActions.indexOf(action) === -1)
  }
  hasExportActions(): boolean {
    return this.getExportActions().length > 0
  }
  getOtherActions() {
    const otherActions = this.getExportActions().concat(this.getMapActions())
    return this.plain.actions.filter(
      action => otherActions.indexOf(action) === -1
    )
  }
  hasRelevance() {
    return Boolean(this.plain.relevance)
  }
  getRoundedRelevance() {
    return this.plain.relevance.toPrecision(properties.relevancePrecision)
  }
  hasErrors() {
    return Boolean(this.getErrors())
  }
  getErrors() {
    return this.plain.metacard.properties['validation-errors']
  }
  hasWarnings() {
    return Boolean(this.getWarnings())
  }
  getWarnings() {
    return this.plain.metacard.properties['validation-warnings']
  }
  getColor() {
    return '#004949'
  }
  getBackbone() {
    if (this.backbone === undefined) {
      this._setBackbone(new QueryResult(this.plain))
    }
    return this.backbone
  }
  _setBackbone(backboneModel: Backbone.Model) {
    this.backbone = backboneModel
    this['_notifySubscribers.backboneCreated']()
  }
  setSelected(isSelected: boolean) {
    if (this.isSelected !== isSelected) {
      this.isSelected = isSelected
      this['_notifySubscribers.selected']()
      return true
    } else {
      return false
    }
  }
  shiftSelect() {
    if (this.parent) {
      this.parent.shiftSelect(this)
    }
  }
  controlSelect() {
    if (this.parent) {
      this.parent.controlSelect(this)
    }
  }
  select() {
    if (this.parent) {
      this.parent.select(this)
    }
  }
  setFiltered(isFiltered: boolean) {
    if (this.isFiltered !== isFiltered) {
      this.isFiltered = isFiltered
      this['_notifySubscribers.filtered']()
      return true
    } else {
      return false
    }
  }
}
