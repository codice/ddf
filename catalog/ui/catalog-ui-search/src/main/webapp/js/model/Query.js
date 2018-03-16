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
/*global define*/

define([
        'backbone',
        'underscore',
        'properties',
        'js/cql',
        'js/model/QueryResponse',
        'component/singletons/sources-instance',
        'js/Common',
        'js/CacheSourceSelector',
        'component/announcement',
        'js/CQLUtils',
        'component/singletons/user-instance',
        'lodash/merge',
        'backboneassociations',
    ],
    function (Backbone, _, properties, cql, QueryResponse, Sources, Common, CacheSourceSelector, announcement,
        CQLUtils, user, _merge) {
        "use strict";
        var Query = {};

        function limitToDeleted(cqlString) {
            return CQLUtils.transformFilterToCQL({
                type: 'AND',
                filters: [
                    CQLUtils.transformCQLToFilter(cqlString),
                    {
                        property: '"metacard-tags"',
                        type: "ILIKE",
                        value: 'deleted'
                    }
                ]
            });
        }

        function limitToHistoric(cqlString) {
            return CQLUtils.transformFilterToCQL({
                type: 'AND',
                filters: [
                    CQLUtils.transformCQLToFilter(cqlString),
                    {
                        property: '"metacard-tags"',
                        type: "ILIKE",
                        value: 'revision'
                    }
                ]
            });
        }

        Query.Model = Backbone.AssociatedModel.extend({
            relations: [{
                type: Backbone.One,
                key: 'result',
                relatedModel: QueryResponse,
                isTransient: true
            }],
            //in the search we are checking for whether or not the model
            //only contains 5 items to know if we can search or not
            //as soon as the model contains more than 5 items, we assume
            //that we have enough values to search
            defaults: function () {
                return _merge({
                    cql: "anyText ILIKE ''",
                    title: 'Search Name',
                    excludeUnnecessaryAttributes: true,
                    count: properties.resultCount,
                    start: 1,
                    federation: 'enterprise',
                    sortField: 'modified',
                    sortOrder: 'descending',
                    result: undefined,
                    serverPageIndex: 0,
                    type: 'text',
                    isLocal: false,
                    isOutdated: false
                }, user.getQuerySettings().toJSON());
            },
            resetToDefaults: function() {
                this.set(_.omit(this.defaults(), ['type', 'isLocal', 'serverPageIndex', 'result']));
                this.trigger('resetToDefaults');
            },
            applyDefaults: function() {
                this.set(_.pick(this.defaults(), ['sortField', 'sortOrder', 'federation', 'src']));
            },
            revert: function() {
                this.trigger('revert');
            },
            isLocal: function() {
                return this.get('isLocal');
            },
            initialize: function () {
                this.currentIndexForSource = {};

                _.bindAll.apply(_, [this].concat(_.functions(this))); // underscore bindAll does not take array arg
                this.set('id', this.getId());
                this.listenTo(user.get('user>preferences'), 'change:resultCount', this.handleChangeResultCount);
                this.listenTo(this, 'change:cql', () => this.set('isOutdated', true));
            },
            buildSearchData: function () {
                var data = this.toJSON();

                switch (data.federation) {
                    case 'local':
                        if (!properties.isDisableLocalCatalog()) {
                            data.src = [Sources.localCatalog];
                        }
                        break;
                    case 'enterprise':
                        data.src = _.pluck(Sources.toJSON(), 'id');
                        break;
                    case 'selected':
                        // already in correct format
                        break;
                }

                data.count = user.get('user').get('preferences').get('resultCount');

                data.sort = this.get('sortField') + ':' + this.get('sortOrder');

                return _.pick(data, 'src', 'start', 'count', 'timeout', 'cql', 'sort', 'id');
            },
            isOutdated() {
               return this.get('isOutdated');
            },
            startSearchIfOutdated() {
                if (this.isOutdated()) {
                  this.startSearch();
                }
            },
            startSearch: function (options) {
                this.set('isOutdated', false);
                if (this.get('cql') === '') {
                    return;
                }
                options = _.extend({
                    limitToDeleted: false,
                    limitToHistoric: false
                }, options);
                this.cancelCurrentSearches();

                var data = Common.duplicate(this.buildSearchData());
                if (options.resultCountOnly) {
                    data.count = 0;
                }
                var sources = data.src;
                var initialStatus = sources.map(function (src) {
                    return {
                        id: src
                    };
                });
                var result;
                if (this.get('result') && this.get('result').get('results')) {
                    result = this.get('result');
                    result.setColor(this.getColor());
                    result.setQueryId(this.getId());
                    result.set('merged', true);
                    result.get('queuedResults').fullCollection.reset();
                    result.get('queuedResults').reset();
                    result.get('results').fullCollection.reset();
                    result.get('results').reset();
                    result.get('status').reset(initialStatus);
                } else {
                    result = new QueryResponse({
                        queryId: this.getId(),
                        color: this.getColor(),
                        status: initialStatus
                    });
                    this.set({
                        result: result
                    });
                }

                var sortField = this.get('sortField');
                var sortOrder = this.get('sortOrder') === 'descending' ? -1 : 1;

                switch (sortField) {
                    case 'RELEVANCE':
                        result.get('results').fullCollection.comparator = function (a, b) {
                            return sortOrder * (a.get('relevance') - b.get('relevance'));
                        };
                        break;
                    case 'DISTANCE':
                        result.get('results').fullCollection.comparator = function (a, b) {
                            return sortOrder * (a.get('distance') - b.get('distance'));
                        };
                        break;
                    default:
                        result.get('results').fullCollection.comparator = function (a, b) {
                            var aVal = a.get('metacard>properties>' + sortField);
                            var bVal = b.get('metacard>properties>' + sortField);
                            if (aVal < bVal) {
                                return sortOrder * -1;
                            }
                            if (aVal > bVal) {
                                return sortOrder;
                            }
                            return 0;
                        };
                }

                result.set('initiated', Date.now());
                result.set('resultCountOnly', options.resultCountOnly);
                result.get('results').fullCollection.sort();

                if (sources.length === 0) {
                    announcement.announce({
                        title: 'Search "' + this.get('title') + '" cannot be run.',
                        message: 'No sources are currently selected.  Edit the search and select at least one source.',
                        type: 'warn'
                    });
                    return [];
                }

                if (!properties.isCacheDisabled) {
                    sources.unshift("cache");
                }

                var cqlString = data.cql;
                if (options.limitToDeleted) {
                    cqlString = limitToDeleted(cqlString);
                } else if (options.limitToHistoric) {
                    cqlString = limitToHistoric(cqlString);
                }
                var query = this;
                this.currentSearches = sources.map(function (src) {
                    data.src = src;
                    data.start = query.getStartIndexForSource(src);

                    // since the "cache" source will return all cached results, need to
                    // limit the cached results to only those from a selected source
                    data.cql = (src === 'cache') ?
                        CacheSourceSelector.trimCacheSources(cqlString, sources) :
                        cqlString;
                    var payload = JSON.stringify(data);

                    return result.fetch({
                        customErrorHandling: true,
                        data: payload,
                        remove: false,
                        dataType: "json",
                        contentType: "application/json",
                        method: "POST",
                        processData: false,
                        timeout: properties.timeout,
                        success: function (model, response, options) {
                            response.options = options;
                            if (options.resort === true) {
                                model.get('results').fullCollection.sort();
                            }
                        },
                        error: function (model, response, options) {
                            var srcStatus = result.get('status').get(src);
                            if (srcStatus) {
                                srcStatus.set({
                                    successful: false,
                                    pending: false
                                });
                            }
                            response.options = options;
                        }
                    });
                });
                return this.currentSearches;
            },
            currentSearches: [],
            cancelCurrentSearches: function () {
                this.currentSearches.forEach(function (request) {
                    request.abort('Canceled');
                });
                this.currentSearches = [];
            },
            clearResults: function () {
                this.cancelCurrentSearches();
                this.set({
                    result: undefined
                });
            },
            setSources: function (sources) {
                var sourceArr = [];
                sources.each(function (src) {
                    if (src.get('available') === true) {
                        sourceArr.push(src.get('id'));
                    }
                });
                if (sourceArr.length > 0) {
                    this.set('src', sourceArr.join(','));
                } else {
                    this.set('src', '');
                }
            },
            getId: function () {
                if (this.get('id')) {
                    return this.get('id');
                } else {
                    var id = this._cloneOf || this.id || Common.generateUUID();
                    this.set('id');
                    return id;
                }
            },
            setColor: function (color) {
                this.set('color', color);
            },
            getColor: function () {
                return this.get('color');
            },
            color: function () {
                return this.get('color');
            },
            hasPreviousServerPage: function () {
                return Boolean(_.find(this.currentIndexForSource, function (index) {
                    return index > 1;
                }));
            },
            hasNextServerPage: function () {
                var pageSize = user.get('user').get('preferences').get('resultCount');
                return Boolean(this.get('result').get('status').find(function (status) {
                    var startingIndex = this.getStartIndexForSource(status.id);
                    var total = status.get('hits');
                    return (total - startingIndex) >= pageSize;
                }.bind(this)));
            },
            getPreviousServerPage: function () {
                this.get('result').getSourceList().forEach(function (src) {
                    var increment = this.get('result').getLastResultCountForSource(src);
                    this.currentIndexForSource[src] = Math.max(this.getStartIndexForSource(src) - increment, 1);
                }.bind(this));
                this.set('serverPageIndex', Math.max(0, this.get('serverPageIndex') - 1));
                this.startSearch();
            },
            getNextServerPage: function () {
                this.get('result').getSourceList().forEach(function (src) {
                    var increment = this.get('result').getLastResultCountForSource(src);
                    this.currentIndexForSource[src] = this.getStartIndexForSource(src) + increment;
                }.bind(this));
                this.set('serverPageIndex', this.get('serverPageIndex') + 1);
                this.startSearch();
            },
            // get the starting offset (beginning of the server page) for the given source
            getStartIndexForSource: function (src) {
                return this.currentIndexForSource[src] || 1;
            },
            // if the server page size changes, reset our indices and let them get
            // recalculated on the next fetch
            handleChangeResultCount: function () {
                this.currentIndexForSource = {};
                this.set('serverPageIndex', 0);
                if (this.get('result')) {
                    this.get('result').resetResultCountsBySource();
                }
            },
            getResultsRangeLabel: function (resultsCollection) {
                var results = resultsCollection.fullCollection.length;
                var hits = _.filter(this.get('result').get('status').toJSON(), function (status) {
                    return status.id !== 'cache';
                }).reduce(function (hits, status) {
                    return status.hits ? hits + status.hits : hits;
                }, 0);

                if (hits === 0) {
                    return '0 results';
                } else if (results > hits) {
                    return hits + ' results';
                }

                var clientState = resultsCollection.state || this.get('result').get('results').state;
                var serverPageSize = user.get('user>preferences>resultCount');
                var startingIndex = this.get('serverPageIndex') * serverPageSize +
                    (clientState.currentPage - 1) * clientState.pageSize;
                var endingIndex = startingIndex + resultsCollection.length;

                return (startingIndex + 1) + '-' + endingIndex + ' of ' + hits;
            }
        });
        return Query;
    });