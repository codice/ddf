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
/*global define*/
var Backbone = require('backbone');
var Marionette = require('marionette');
var wreqr = require('wreqr');
var _ = require('underscore');
var $ = require('jquery');
var template = require('./associations-graph.hbs');
var CustomElements = require('js/CustomElements');
var store = require('js/store');
var LoadingCompanionView = require('component/loading-companion/loading-companion.view');
var AssociationView = require('component/association/association.view');
var Association = require('component/association/association');
var Vis = require('vis');

function determineNodes(view) {
    var currentMetacard = view.options.currentMetacard;
    var nodes = view.options.knownMetacards.map(function(metacard) {
        return {
            id: metacard.id,
            label: metacard.get('title')
        };
    }).concat(view.options.selectionInterface.getActiveSearchResults().map(function(result) {
        return {
            id: result.get('metacard').id,
            label: result.get('metacard').get('properties').get('title')
        }
    }));
    nodes = _.uniq(nodes, false, function(node) {
        return node.id;
    });
    return nodes.map(function(node) {
        return {
            id: node.id,
            label: node.id === currentMetacard.get('metacard').id ? 'Current Metacard' : node.label
        };
    });
}

// Visjs throws an error if the edge can't be find.  We don't care.
function selectEdges(network, ids) {
    try {
        network.selectEdges(ids);
    } catch (e) {
        // I don't care if the edge didn't exist!
    }
}

// Visjs throws an error sometimes, we don't care
function fitGraph(network) {
    try {
        if (network) {
            network.fit();
        }
    } catch (e) {
        // I don't care!
    }
}

function handleSelection(network, ids) {
    if (network) {
        network.unselectAll();
        if (ids) {
            selectEdges(network, ids);
        }
    }
}

module.exports = Marionette.LayoutView.extend({
    regions: {
        graphInspector: '> .graph-inspector > .inspector-association'
    },
    template: template,
    tagName: CustomElements.register('associations-graph'),
    events: {
        'click .inspector-action > .action-add': 'addEdge',
        'click .inspector-action > .action-remove': 'removeSelectedEdge',
        'mouseup > .graph-network': 'handleMouseup',
        'keyup > .graph-network': 'handleKeyup'
    },
    handleKeyup: function(e) {
        if (this.$el.hasClass('is-editing') && this.$el.hasClass('has-association-selected')) {
            if ([8, 46].indexOf(e.keyCode) >= 0) {
                this.removeSelectedEdge();
            } else if ([27].indexOf(e.keyCode) >= 0){
                this.handleMouseup();
            }
        }
    },
    handleMouseup: function() {
        this.$el.toggleClass('has-association-selected', false);
        handleSelection(this._childNetwork);
        handleSelection(this._parentNetwork);
        this.$el.find('> .graph-network').focus();
    },
    _parentNetwork: undefined,
    _childNetwork: undefined,
    initialize: function(options) {
        this.setupListeners();
    },
    setupListeners: function() {
        this.listenTo(this.collection, 'reset add remove update change', this.showGraphView);
        this.listenTo(this.collection, 'add', function() {
            setTimeout(function() {
                this.fitGraph();
            }.bind(this), 1000);
        }.bind(this));
    },
    onBeforeShow: function() {
        this.showGraphView();
        this.showGraphInspector();
    },
    showGraphView: function() {
        this.showParentGraph();
        this.showChildGraph();
        this.handleSelection();
        this.$el.find('canvas').attr('tab-index', -1);
    },
    handleFilter: function(filter) {
        this.$el.toggleClass('filter-by-parent', filter === 'parent');
        this.$el.toggleClass('filter-by-child', filter === 'child');
        this.showGraphInspector();
        setTimeout(function() {
            this.fitGraph();
        }.bind(this), 1000);
    },
    handleSelection: function() {
        var graphInspector = this.graphInspector.currentView;
        if (graphInspector) {
            handleSelection(this._parentNetwork, [graphInspector.model.cid]);
            handleSelection(this._childNetwork, [graphInspector.model.cid]);
        }
    },
    showParentGraph: function() {
        var currentMetacard = this.options.currentMetacard;
        var nodes = determineNodes(this);

        // create an array with edges
        var edges = this.collection.map(function(association) {
            return {
                arrows: {
                    to: {
                        enabled: true
                    }
                },
                id: association.cid,
                from: association.get('parent'),
                to: association.get('child'),
                label: association.get('relationship') === 'related' ? 'related' : '\n\n\nderived',
                font: {
                    color: 'white',
                    strokeWidth: 0
                },
                smooth: {
                    type: 'cubicBezier',
                    forceDirection: 'vertical'
                }
            };
        }).filter(function(edge) {
            return edge.to === currentMetacard.get('metacard').id;
        });

        nodes = nodes.filter(function(node) {
            return edges.some(function(edge) {
                return edge.from === node.id || edge.to === node.id;
            });
        });
        this.$el.toggleClass('has-no-parent', nodes.length === 0);
        if (nodes.length === 0) {
            return;
        }

        var data = {
            nodes: new Vis.DataSet(nodes),
            edges: new Vis.DataSet(edges)
        };
        var options = {
            layout: {
                hierarchical: {
                    sortMethod: 'directed'
                }
            },
            interaction: {
                selectConnectedEdges: false,
                hover: true
            }
        };
        if (!this._parentNetwork) {
            this._parentNetwork = new Vis.Network(this.$el.find('> .graph-network .network-parent')[0], data, options);
            this.listenToNetwork(this._parentNetwork);
        } else {
            this._parentNetwork.setData(data);
        }
    },
    showChildGraph: function() {
        var currentMetacard = this.options.currentMetacard;
        var nodes = determineNodes(this);

        // create an array with edges
        var edges = this.collection.map(function(association) {
            return {
                arrows: {
                    to: {
                        enabled: true
                    }
                },
                id: association.cid,
                from: association.get('parent'),
                to: association.get('child'),
                label: association.get('relationship') === 'related' ? 'related' : '\n\n\nderived',
                font: {
                    color: 'white',
                    strokeWidth: 0
                },
                smooth: {
                    type: 'cubicBezier',
                    forceDirection: 'vertical'
                }
            };
        }).filter(function(edge) {
            return edge.from === currentMetacard.get('metacard').id;
        });

        nodes = nodes.filter(function(node) {
            return edges.some(function(edge) {
                return edge.from === node.id || edge.to === node.id;
            });
        });
        this.$el.toggleClass('has-no-child', nodes.length === 0);
        if (nodes.length === 0) {
            return;
        }

        var data = {
            nodes: new Vis.DataSet(nodes),
            edges: new Vis.DataSet(edges)
        };
        var options = {
            layout: {
                hierarchical: {
                    sortMethod: 'directed'
                }
            },
            interaction: {
                selectConnectedEdges: false,
                hover: true
            }
        };
        if (!this._childNetwork) {
            this._childNetwork = new Vis.Network(this.$el.find('> .graph-network .network-child')[0], data, options);
            this.listenToNetwork(this._childNetwork);
        } else {
            this._childNetwork.setData(data);
        }
    },
    listenToNetwork: function(network) {
        network.on('selectEdge', this.handleEdgeSelection.bind(this));
        network.on('deselectEdge', this.showGraphInspector.bind(this));
        network.on('hoverEdge', this.handleHover.bind(this));
        network.on('blurEdge', this.handleUnhover.bind(this));
        network.on('hoverNode', this.handleHover.bind(this));
        network.on('blurNode', this.handleUnhover.bind(this));
        network.on('selectNode', this.handleNodeSelection.bind(this));
    },
    handleNodeSelection: function(params) {
        if (!this.$el.hasClass('is-editing')) {
            wreqr.vent.trigger('router:navigate', {
                fragment: 'metacards/' + params.nodes[0],
                options: {
                    trigger: true
                }
            });
        }
    },
    handleHover: function() {
        this.$el.find('> .graph-network').addClass('is-hovering');
    },
    handleUnhover: function() {
        this.$el.find('> .graph-network').removeClass('is-hovering');
    },
    handleEdgeSelection: function(params) {
        if (params.edges[0]) {
            this.$el.toggleClass('has-association-selected', true);
            this.graphInspector.show(new AssociationView({
                model: this.collection.get(params.edges[0]),
                selectionInterface: this.options.selectionInterface,
                knownMetacards: this.options.knownMetacards,
                currentMetacard: this.options.currentMetacard
            }));
            this.handleSelection();
        }
    },
    fitGraph: function() {
        fitGraph(this._childNetwork);
        fitGraph(this._parentNetwork);
    },
    showGraphInspector: function() {
        this.$el.toggleClass('has-association-selected', false);
        this.graphInspector.show(new AssociationView({
            model: new Association(),
            selectionInterface: this.options.selectionInterface,
            knownMetacards: this.options.knownMetacards,
            currentMetacard: this.options.currentMetacard
        }));
        this.handleSelection();
    },
    addEdge: function() {
        this.collection.add(this.graphInspector.currentView.model);
        this.$el.toggleClass('has-association-selected', true);
    },
    removeSelectedEdge: function() {
        this.collection.remove(this.graphInspector.currentView.model);
        this.showGraphInspector();
    },
    turnOnEditing: function() {
        this.$el.toggleClass('is-editing', true);
    },
    turnOffEditing: function() {
        this.$el.toggleClass('is-editing', false);
    },
    onDestroy: function() {
        if (this._parentNetwork) {
            this._parentNetwork.destroy();
        }
        if (this._childNetwork) {
            this._childNetwork.destroy();
        }
    }
});