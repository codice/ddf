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
import { generateCompareFunction } from './sort'
import { LazyQueryResult } from './LazyQueryResult'
import { QuerySortType, FilterType } from './types'

const user = require('../../../component/singletons/user-instance.js')
const Backbone = require('backbone')

/**
 * Constructed with performance in mind, taking advantage of maps whenever possible.
 * This is the heart of our app, so take care when updating / adding things here to
 * do it with performance in mind.
 *
 */
export class LazyQueryResults {
  subscriptions: { [key: string]: () => void }
  subscribe(callback: () => void) {
    const id = Math.random().toString()
    this.subscriptions[id] = callback
    return () => {
      this._unsubscribe(id)
    }
  }
  _unsubscribe(id?: string) {
    if (id === undefined) return
    delete this.subscriptions[id]
  }
  _notifySubscriptions() {
    Object.values(this.subscriptions).forEach(sub => sub())
  }
  compareFunction: (a: LazyQueryResult, b: LazyQueryResult) => number
  results: {
    [key: string]: LazyQueryResult
  }
  /**
   * This is fairly common ask between visuals that use these results, so we keep a copy for repeat access
   */
  filteredResults: {
    [key: string]: LazyQueryResult
  }
  selectedResults: {
    [key: string]: LazyQueryResult
  }
  _getMaxIndexOfSelectedResults() {
    return Object.values(this.selectedResults).reduce((max, result) => {
      return Math.max(max, result.index)
    }, -1)
  }
  _getMinIndexOfSelectedResults() {
    return Object.values(this.selectedResults).reduce((min, result) => {
      return Math.min(min, result.index)
    }, Object.keys(this.results).length)
  }
  shiftSelect(target: LazyQueryResult) {
    const firstIndex = this._getMinIndexOfSelectedResults()
    const lastIndex = this._getMaxIndexOfSelectedResults()
    const indexClicked = target.index
    if (Object.keys(this.selectedResults).length === 0) {
      target.setSelected(target.isSelected)
    } else if (indexClicked <= firstIndex) {
      // traverse from target to next until firstIndex
      let currentItem = target as LazyQueryResult | undefined
      while (currentItem && currentItem.index <= firstIndex) {
        currentItem.setSelected(true)
        currentItem = currentItem.next
      }
    } else if (indexClicked >= lastIndex) {
      // traverse from target to prev until lastIndex
      let currentItem = target as LazyQueryResult | undefined
      while (currentItem && currentItem.index >= lastIndex) {
        currentItem.setSelected(true)
        currentItem = currentItem.prev
      }
    } else {
      // traverse from target to prev until something doesn't change
      let currentItem = target as LazyQueryResult | undefined
      let changed = true
      while (currentItem && changed) {
        changed = currentItem.setSelected(true) && changed
        currentItem = currentItem.prev
      }
    }
  }
  controlSelect(target: LazyQueryResult) {
    target.setSelected(!target.isSelected)
  }
  select(target: LazyQueryResult) {
    const isSelected = !target.isSelected
    this.deselect()
    target.setSelected(isSelected)
  }
  deselect() {
    Object.values(this.selectedResults).forEach(result => {
      result.setSelected(false)
    })
  }
  /**
   * Array of the callbacks to unsubscribe
   */
  selectionSubscriptions: (() => void)[]
  _updateFilteredResults() {
    this.filteredResults = Object.values(this.results)
      .filter(result => {
        return result.isFiltered === false
      })
      .reduce(
        (blob, result) => {
          blob[result['metacard.id']] = result
          return blob
        },
        {} as { [key: string]: LazyQueryResult }
      )
    console.log(this.filteredResults)
  }
  backboneModel: Backbone.Model
  /**
   * Can contain distance / best text match
   * (this matches what the query requested)
   */
  persistantSorts: QuerySortType[]
  /**
   * on the fly sorts (user prefs), so no distance or best text match
   * (this is a user pref aka client side only)
   */
  ephemeralSorts: QuerySortType[]
  /**
   * on the fly filtering (user prefs aka client side only)
   */
  ephemeralFilter?: FilterType
  _updateEphemeralFilter() {
    this.ephemeralFilter = user.getPreferences().get('resultFilter')
  }
  /**
   * Go through and set isFiltered on results
   *
   * This keeps us from needing to resort, and will allow multiselection
   * to still work.
   */
  _refilter() {
    if (this.ephemeralFilter !== undefined) {
      let updated = false
      Object.values(this.results).forEach(result => {
        updated =
          result.setFiltered(
            result.matchesFilters(this.ephemeralFilter as FilterType) === false
          ) || updated
      })
      this._updateFilteredResults()
    } else {
      Object.values(this.results).forEach(result => {
        result.setFiltered(false)
      })
      this.filteredResults = this.results
    }
  }
  /**
   *  Should really only be set at constructor time (moment a query is done)
   */
  _updatePersistantSorts(sorts: QuerySortType[]) {
    this.persistantSorts = sorts
  }
  /**
   *  Should be updated based on user prefs at the current moment,
   *  And respond to updates to those prefs on the fly.
   */
  _updateEphemeralSorts() {
    this.ephemeralSorts = user.getPreferences().get('resultSort') || []
  }
  _getSortedResults(results: LazyQueryResult[]) {
    return results.sort(
      generateCompareFunction(
        this.ephemeralSorts.length > 0
          ? this.ephemeralSorts
          : this.persistantSorts
      )
    )
  }
  /**
   * The map of results will ultimately be the source of truth here
   * Maps guarantee chronological order for Object.keys operations,
   * so we turn it into an array to sort then feed it back into a map.
   *
   * On resort we have to update the links between results (used for selecting performantly),
   * as well as the indexes which are used similarly
   *
   */
  _resort() {
    this.results = this._getSortedResults(Object.values(this.results)).reduce(
      (blob, result, index, results) => {
        result.index = index
        result.prev = results[index - 1]
        result.next = results[index + 1]
        blob[result['metacard.id']] = result
        return blob
      },
      {} as { [key: string]: LazyQueryResult }
    )
  }
  constructor({
    results = [],
    sorts = [],
  }: { results?: ResultType[]; sorts?: QuerySortType[] } = {}) {
    this.init()
    this._updatePersistantSorts(sorts)
    this._updateEphemeralSorts()
    this._updateEphemeralFilter()
    this.add({ results })

    this.backboneModel = new Backbone.Model({
      id: Math.random().toString(),
    })
    this.backboneModel.listenTo(
      user,
      'change:user>preferences>resultSort',
      () => {
        this._updateEphemeralSorts()
        this._resort()
        this._refilter() // needs to be in sync in the sorted map
        this._notifySubscriptions()
      }
    )
    this.backboneModel.listenTo(
      user,
      'change:user>preferences>resultFilter',
      () => {
        this._updateEphemeralFilter()
        this._refilter()
        this._notifySubscriptions()
      }
    )
  }
  init() {
    if (this.selectionSubscriptions)
      this.selectionSubscriptions.forEach(unsubscribe => {
        unsubscribe()
      })
    this.selectionSubscriptions = []
    this.selectedResults = {}
    if (this.subscriptions === undefined) this.subscriptions = {}
    this.results = {}
  }
  reset() {
    this.init()
    this._resort()
    this._refilter()
    this._notifySubscriptions()
  }
  destroy() {
    this.backboneModel.stopListening()
  }
  isEmpty() {
    return Object.keys(this.results).length === 0
  }
  add({ results = [] }: { results?: ResultType[] } = {}) {
    results.forEach(result => {
      const lazyResult = new LazyQueryResult(result)
      this.results[lazyResult['metacard.id']] = lazyResult
      lazyResult.parent = this
      this.selectionSubscriptions.push(
        lazyResult.subscribeToSelection(() => {
          if (lazyResult.isSelected) {
            this.selectedResults[lazyResult['metacard.id']] = lazyResult
          } else {
            delete this.selectedResults[lazyResult['metacard.id']]
          }
        })
      )
    })
    this._resort()
    this._refilter()
    this._notifySubscriptions()
  }
}
