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

const Marionette = require('marionette')
const wreqr = require('../../js/wreqr.js')
const _ = require('underscore')
const template = require('./associations-graph.hbs')
const CustomElements = require('../../js/CustomElements.js')
const AssociationView = require('../association/association.view.js')
const Association = require('../association/association.js')
const Vis = require('vis')

function determineNodes(view) {
  const currentMetacard = view.options.currentMetacard
  let nodes = view.options.knownMetacards
    .map(metacard => ({
      id: metacard.id,
      label: metacard.get('title'),
    }))
    .concat(
      view.options.selectionInterface.getActiveSearchResults().map(result => ({
        id: result.get('metacard').id,

        label: result
          .get('metacard')
          .get('properties')
          .get('title'),
      }))
    )
  nodes = _.uniq(nodes, false, node => node.id)
  return nodes.map(node => ({
    id: node.id,

    label:
      node.id === currentMetacard.get('metacard').id
        ? 'Current Metacard'
        : node.label,
  }))
}

// Visjs throws an error if the edge can't be find.  We don't care.
function selectEdges(network, ids) {
  try {
    network.selectEdges(ids)
  } catch (e) {
    // I don't care if the edge didn't exist!
  }
}

// Visjs throws an error sometimes, we don't care
function fitGraph(network) {
  try {
    if (network) {
      network.fit()
    }
  } catch (e) {
    // I don't care!
  }
}

function handleSelection(network, ids) {
  if (network) {
    network.unselectAll()
    if (ids) {
      selectEdges(network, ids)
    }
  }
}

const fontStyles = {
  color: 'black',
  strokeWidth: 10,
  strokeColor: 'white',
}

module.exports = Marionette.LayoutView.extend({
  regions: {
    graphInspector: '> .graph-inspector > .inspector-association',
  },
  template,
  tagName: CustomElements.register('associations-graph'),
  events: {
    'click .inspector-action > .action-add': 'addEdge',
    'click .inspector-action > .action-remove': 'removeSelectedEdge',
    'mouseup > .graph-network': 'handleMouseup',
    'keyup > .graph-network': 'handleKeyup',
  },
  handleKeyup(e) {
    if (
      this.$el.hasClass('is-editing') &&
      this.$el.hasClass('has-association-selected')
    ) {
      if ([8, 46].indexOf(e.keyCode) >= 0) {
        this.removeSelectedEdge()
      } else if ([27].indexOf(e.keyCode) >= 0) {
        this.handleMouseup()
      }
    }
  },
  handleMouseup() {
    this.$el.toggleClass('has-association-selected', false)
    handleSelection(this._childNetwork)
    handleSelection(this._parentNetwork)
    this.$el.find('> .graph-network').focus()
  },
  _parentNetwork: undefined,
  _childNetwork: undefined,
  initialize(options) {
    this.setupListeners()
  },
  setupListeners() {
    this.listenTo(
      this.collection,
      'reset add remove update change',
      this.showGraphView
    )
    this.listenTo(this.collection, 'add', () => {
      setTimeout(() => {
        this.fitGraph()
      }, 1000)
    })
  },
  onBeforeShow() {
    this.showGraphView()
    this.showGraphInspector()
  },
  showGraphView() {
    this.showParentGraph()
    this.showChildGraph()
    this.handleSelection()
    this.$el.find('canvas').attr('tab-index', -1)
  },
  handleFilter(filter) {
    this.$el.toggleClass('filter-by-parent', filter === 'parent')
    this.$el.toggleClass('filter-by-child', filter === 'child')
    this.showGraphInspector()
    setTimeout(() => {
      this.fitGraph()
    }, 1000)
  },
  handleSelection() {
    const graphInspector = this.graphInspector.currentView
    if (graphInspector) {
      handleSelection(this._parentNetwork, [graphInspector.model.cid])
      handleSelection(this._childNetwork, [graphInspector.model.cid])
    }
  },
  showParentGraph() {
    const currentMetacard = this.options.currentMetacard
    let nodes = determineNodes(this)

    // create an array with edges
    const edges = this.collection
      .map(association => ({
        arrows: {
          to: {
            enabled: true,
          },
        },

        id: association.cid,
        from: association.get('parent'),
        to: association.get('child'),

        label:
          association.get('relationship') === 'related'
            ? 'related'
            : '\n\n\nderived',

        font: fontStyles,

        smooth: {
          type: 'cubicBezier',
          forceDirection: 'vertical',
        },
      }))
      .filter(edge => edge.to === currentMetacard.get('metacard').id)

    nodes = nodes.filter(node =>
      edges.some(edge => edge.from === node.id || edge.to === node.id)
    )
    this.$el.toggleClass('has-no-parent', nodes.length === 0)
    if (nodes.length === 0) {
      return
    }

    const data = {
      nodes: new Vis.DataSet(nodes),
      edges: new Vis.DataSet(edges),
    }
    var options = {
      layout: {
        hierarchical: {
          sortMethod: 'directed',
        },
      },
      interaction: {
        selectConnectedEdges: false,
        hover: true,
      },
    }
    if (!this._parentNetwork) {
      this._parentNetwork = new Vis.Network(
        this.$el.find('> .graph-network .network-parent')[0],
        data,
        options
      )
      this.listenToNetwork(this._parentNetwork)
    } else {
      this._parentNetwork.setData(data)
    }
  },
  showChildGraph() {
    const currentMetacard = this.options.currentMetacard
    let nodes = determineNodes(this)

    // create an array with edges
    const edges = this.collection
      .map(association => ({
        arrows: {
          to: {
            enabled: true,
          },
        },

        id: association.cid,
        from: association.get('parent'),
        to: association.get('child'),

        label:
          association.get('relationship') === 'related'
            ? 'related'
            : '\n\n\nderived',

        font: fontStyles,

        smooth: {
          type: 'cubicBezier',
          forceDirection: 'vertical',
        },
      }))
      .filter(edge => edge.from === currentMetacard.get('metacard').id)

    nodes = nodes.filter(node =>
      edges.some(edge => edge.from === node.id || edge.to === node.id)
    )
    this.$el.toggleClass('has-no-child', nodes.length === 0)
    if (nodes.length === 0) {
      return
    }

    const data = {
      nodes: new Vis.DataSet(nodes),
      edges: new Vis.DataSet(edges),
    }
    var options = {
      layout: {
        hierarchical: {
          sortMethod: 'directed',
        },
      },
      interaction: {
        selectConnectedEdges: false,
        hover: true,
      },
    }
    if (!this._childNetwork) {
      this._childNetwork = new Vis.Network(
        this.$el.find('> .graph-network .network-child')[0],
        data,
        options
      )
      this.listenToNetwork(this._childNetwork)
    } else {
      this._childNetwork.setData(data)
    }
  },
  listenToNetwork(network) {
    network.on('selectEdge', this.handleEdgeSelection.bind(this))
    network.on('deselectEdge', this.showGraphInspector.bind(this))
    network.on('hoverEdge', this.handleHover.bind(this))
    network.on('blurEdge', this.handleUnhover.bind(this))
    network.on('hoverNode', this.handleHover.bind(this))
    network.on('blurNode', this.handleUnhover.bind(this))
    network.on('selectNode', this.handleNodeSelection.bind(this))
  },
  handleNodeSelection(params) {
    if (!this.$el.hasClass('is-editing')) {
      wreqr.vent.trigger('router:navigate', {
        fragment: 'metacards/' + params.nodes[0],
        options: {
          trigger: true,
        },
      })
    }
  },
  handleHover() {
    this.$el.find('> .graph-network').addClass('is-hovering')
  },
  handleUnhover() {
    this.$el.find('> .graph-network').removeClass('is-hovering')
  },
  handleEdgeSelection(params) {
    if (params.edges[0]) {
      this.$el.toggleClass('has-association-selected', true)
      this.graphInspector.show(
        new AssociationView({
          model: this.collection.get(params.edges[0]),
          selectionInterface: this.options.selectionInterface,
          knownMetacards: this.options.knownMetacards,
          currentMetacard: this.options.currentMetacard,
        })
      )
      this.handleSelection()
    }
  },
  fitGraph() {
    fitGraph(this._childNetwork)
    fitGraph(this._parentNetwork)
  },
  showGraphInspector() {
    this.$el.toggleClass('has-association-selected', false)
    this.graphInspector.show(
      new AssociationView({
        model: new Association(),
        selectionInterface: this.options.selectionInterface,
        knownMetacards: this.options.knownMetacards,
        currentMetacard: this.options.currentMetacard,
      })
    )
    this.handleSelection()
  },
  addEdge() {
    this.graphInspector.show(
      new AssociationView({
        model: new Association(),
        selectionInterface: this.options.selectionInterface,
        knownMetacards: this.options.knownMetacards,
        currentMetacard: this.options.currentMetacard,
      })
    )
    this.collection.add(this.graphInspector.currentView.model)
    this.$el.toggleClass('has-association-selected', true)
  },
  removeSelectedEdge() {
    this.collection.remove(this.graphInspector.currentView.model)
    this.showGraphInspector()
  },
  turnOnEditing() {
    this.$el.toggleClass('is-editing', true)
  },
  turnOffEditing() {
    this.$el.toggleClass('is-editing', false)
  },
  onDestroy() {
    if (this._parentNetwork) {
      this._parentNetwork.destroy()
    }
    if (this._childNetwork) {
      this._childNetwork.destroy()
    }
  },
})
