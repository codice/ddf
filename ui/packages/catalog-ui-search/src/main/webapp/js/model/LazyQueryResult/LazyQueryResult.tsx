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
import type { LazyQueryResults } from './LazyQueryResults'

const _ = require('underscore')
const Sources = require('../../../component/singletons/sources-instance.js')
const metacardDefinitions = require('../../../component/singletons/metacard-definitions.js')
const properties = require('../../properties.js')
const TurfMeta = require('@turf/meta')
const wkx = require('wkx')
const Common = require('../../Common.js')
import {matchesCql, matchesFilters} from './filter'
import { FilterType } from './types'

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
  return plain
}

export class LazyQueryResult {
  index: number
  prev?: LazyQueryResult
  next?: LazyQueryResult
  parent?: LazyQueryResults
  plain: ResultType
  backbone?: any
  isResourceLocal: boolean
  type: 'query-result'
  subscriptions: { [key: string]: () => void }
  selectionSubscriptions: { [key: string]: () => void };
  filterSubscriptions:  { [key: string]: () => void };
  ['metacard.id']: string
  isSelected: boolean
  isFiltered: boolean
  constructor(plain: ResultType) {
    this.type = 'query-result'
    this.plain = transformPlain({ plain })
    this.isResourceLocal = false || plain.isResourceLocal
    this.subscriptions = {}
    this.selectionSubscriptions = {}
    this.filterSubscriptions = {}
    this['metacard.id'] = plain.metacard.properties.id
    this.isSelected = false
    this.isFiltered = false
    humanizeResourceSize(plain)
    cacheBustThumbnail(plain)
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
      this.setBackbone(new QueryResult(this.plain))
    }
    return this.backbone
  }
  /**
   *  Should really only be called in query response the moment the backbone model is available
   */
  setBackbone(backboneModel: Backbone.Model) {
    this.backbone = backboneModel
    this._notifySubscriptions()
  }
  _notifySubscriptions() {
    Object.values(this.subscriptions).forEach(sub => sub())
  }
  _notifySelectionSubscriptions() {
    Object.values(this.selectionSubscriptions).forEach(sub => sub())
  }
  _notifyFilterSubscriptions() {
    Object.values(this.filterSubscriptions).forEach(sub => sub())
  }
  subscribe(callback: () => void) {
    const id = Math.random().toString()
    this.subscriptions[id] = callback
    return id
  }
  unsubscribe(id?: string) {
    if (id === undefined) return
    delete this.subscriptions[id]
  }
  subscribeToSelection(callback: () => void) {
    const id = Math.random().toString()
    this.selectionSubscriptions[id] = callback
    return () => {
      this.unsubscribeFromSelection(id)
    }
  }
  unsubscribeFromSelection(id?: string) {
    if (id === undefined) return
    delete this.selectionSubscriptions[id]
  }
  setSelected(isSelected: boolean) {
    if (this.isSelected !== isSelected) {
      this.isSelected = isSelected
      this._notifySelectionSubscriptions()
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
  subscribeToFiltered(callback: () => void) {
    const id = Math.random().toString()
    this.filterSubscriptions[id] = callback
    return () => {
      delete this.filterSubscriptions[id]
    }
  }
  setFiltered(isFiltered: boolean) {
    if (this.isFiltered !== isFiltered) {
      this.isFiltered = isFiltered
      this._notifyFilterSubscriptions()
      return true
    } else {
      return false
    }
  }
}
