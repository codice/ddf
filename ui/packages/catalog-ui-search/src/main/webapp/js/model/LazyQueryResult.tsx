// /**
//  * Copyright (c) Codice Foundation
//  *
//  * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
//  * General Public License as published by the Free Software Foundation, either version 3 of the
//  * License, or any later version.
//  *
//  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//  * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//  * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
//  * is distributed along with this program and can be found at
//  * <http://www.gnu.org/licenses/lgpl.html>.
//  *
//  **/
// import * as React from 'react'
// import { ResultType } from './Types'
// import { generateCompareFunction } from './LazyQueryResult/sort'

// const _ = require('underscore')
// const Sources = require('../../component/singletons/sources-instance.js')
// const metacardDefinitions = require('../../component/singletons/metacard-definitions.js')
// const properties = require('../properties.js')
// const TurfMeta = require('@turf/meta')
// const wkx = require('wkx')
// const Common = require('../Common.js')
// const filter = require('../filter.js')
// require('backbone-associations')
// const user = require('../../component/singletons/user-instance.js')
// const Backbone = require('backbone')

// function cacheBustUrl(url: string): string {
//   if (url && url.indexOf('_=') === -1) {
//     let newUrl = url
//     if (url.indexOf('?') >= 0) {
//       newUrl += '&'
//     } else {
//       newUrl += '?'
//     }
//     newUrl += '_=' + Date.now()
//     return newUrl
//   }
//   return url
// }

// function cacheBustThumbnail(plain: ResultType) {
//   let url = plain.metacard.properties.thumbnail
//   if (url) {
//     plain.metacard.properties.thumbnail = cacheBustUrl(url)
//   }
// }

// function humanizeResourceSize(plain: ResultType) {
//   if (plain.metacard.properties['resource-size']) {
//     plain.metacard.properties['resource-size'] = Common.getFileSize(
//       plain.metacard.properties['resource-size']
//     )
//   }
// }

// /**
//  * Add defaults, etc.  We need to make sure everything has a tag at the very least
//  */
// const transformPlain = ({
//   plain,
// }: {
//   plain: LazyQueryResult['plain']
// }): LazyQueryResult['plain'] => {
//   if (!plain.metacard.properties['metacard-tags']) {
//     plain.metacard.properties['metacard-tags'] = ['resource']
//   }
//   return plain
// }

// export type QuerySortType = {
//   attribute: string
//   direction: string
// }

// /**
//  * I think we need a container for this
//  */
// export class LazyQueryResults {
//   compareFunction: (a: LazyQueryResult, b: LazyQueryResult) => number
//   results: {
//     [key: string]: LazyQueryResult
//   }
//   sortedAndFilteredResults: LazyQueryResult[]
//   backboneModel: Backbone.Model
//   /**
//    * Can contain distance / best text match
//    */
//   persistantSorts: QuerySortType[]
//   // on the fly sorts (user prefs), so no distance or best text match
//   ephemeralSorts: QuerySortType[]
//   sort() {
//     /**
//      * How do we do this
//      *
//      * We could instead generate a new LazyQueryResults?
//      * I think that makes the most sense.  One thing at the top
//      * says sort, then all the rest are passed a sorted version.
//      *
//      * Although, only the table / list view care about order.
//      * With that in mind, maybe they should sort the list themselves
//      * on the fly?  No that might be expensive.  Also need filtering
//      * which affects all visuals.  So we need a better way.
//      *
//      * I guess filtering could be done by hiding?  That seems like a
//      * bad idea though, when it comes to multiselect.
//      *
//      *
//      */
//   }
//   filter() {
//     //
//   }
//   /**
//    *  Should really only be set at constructor time (moment a query is done)
//    * @param sorts
//    */
//   _updatePersistantSorts(sorts: QuerySortType[]) {
//     this.persistantSorts = sorts
//   }
//   /**
//    *  Should be updated based on user prefs at the current moment,
//    *  And respond to updates to those prefs on the fly.
//    * @param sorts
//    */
//   _updateEphemeralSorts() {
//     this.ephemeralSorts = user.getPreferences().get('resultSort') || []
//   }
//   _getSortedResults(results: LazyQueryResult[]) {
//     return results.sort(
//       generateCompareFunction(this.ephemeralSorts || this.persistantSorts)
//     )
//   }
//   /**
//    * The map of results will ultimately be the source of truth here
//    * Maps guarantee chronological order for Object.keys operations,
//    * so we turn it into an array to sort then feed it back into a map.
//    */
//   _resort() {
//     this.results = this._getSortedResults(Object.values(this.results)).reduce(
//       (blob, result) => {
//         blob[result['metacard.id']] = result
//         return blob
//       },
//       {} as { [key: string]: LazyQueryResult }
//     )
//     // now notify subscriptions?
//   }
//   constructor({
//     results = [],
//     sorts = [],
//   }: { results?: ResultType[]; sorts?: QuerySortType[] } = {}) {
//     this.results = {}
//     this._updatePersistantSorts(sorts)
//     this.add({ results })

//     this.backboneModel = new Backbone.Model({
//       id: Math.random().toString(),
//     })
//     this.backboneModel.listenTo(
//       user,
//       'change:user>preferences>resultSort',
//       () => {
//         this._updateEphemeralSorts()
//       }
//     )
//   }
//   destroy() {
//     this.backboneModel.stopListening()
//   }
//   isEmpty() {
//     return Object.keys(this.results).length === 0
//   }
//   /**
//    * If it's empty sort ahead of time for perf,
//    * otherwise toss into the map then sort.
//    */
//   add({ results = [] }: { results?: ResultType[] } = {}) {
//     // if (this.isEmpty()) {
//     //   this.results = this._getSortedResults(results.map(result => {
//     //     const lazyResult = new LazyQueryResult(result)
//     //     lazyResult.parent = this
//     //     return lazyResult
//     //   })).reduce((blob, result) => {
//     //     blob[result["metacard.id"]] = result
//     //     return blob
//     //   }, {} as {[key:string]:LazyQueryResult})
//     // } else {
//     results.forEach(result => {
//       const lazyResult = new LazyQueryResult(result)
//       this.results[lazyResult['metacard.id']] = lazyResult
//       lazyResult.parent = this
//     })
//     this._resort()
//     // }

//     // sort and filter?
//   }
// }
