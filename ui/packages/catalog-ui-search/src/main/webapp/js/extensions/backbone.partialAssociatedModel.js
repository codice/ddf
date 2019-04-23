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
import * as Backbone from 'backbone-associations'
import fetch from '../../react-component/utils/fetch'

const PARTIAL_KEY = '_isPartial'

const hasPartialRelations = model =>
  Array.isArray(model.relations) &&
  model.relations.some(relation => isRelationPartial(model, relation))

const isRelationPartial = (model, relation) => {
  const associatedModel = model.get(relation.key)
  return (
    associatedModel && associatedModel.isPartial && associatedModel.isPartial()
  )
}

const handleRelations = model => {
  if (!Array.isArray(model.relations)) {
    model.relations = []
  }
}

const handleOptions = (model, options) => {
  model.options = { fetch, ...options }
}

/**
 * For models inside other models that are initialized as partial representations of themselves.
 * One example is the queries inside workspaces, and soon lists inside workspaces.
 */
export default Backbone.AssociatedModel.extend({
  /**
   * Mixin the isPartial attribute on creation
   * If the only attribute provided is id, we know it's a partial representation of the full model so we set it to true
   * @param {*} attributes
   * @param {*} options
   */
  constructor(attributes, options) {
    handleOptions(this, options)
    handleRelations(this)
    if (attributes) {
      attributes[PARTIAL_KEY] =
        Object.keys(attributes).length === 1 && attributes.id !== undefined
    }
    return Backbone.AssociatedModel.prototype.constructor.apply(this, arguments)
  },
  /**
   * Return whether or not the model is a partial representation of the full model
   *
   * If given relations as arguments, will only check those relations
   */
  isPartial() {
    return this.get(PARTIAL_KEY) || hasPartialRelations(this)
  },
  isFetchingPartial: false,
  async fetchPartial() {
    if (this.isFetchingPartial) {
      return
    }
    this.isFetchingPartial = true
    this.listenToOnce(this, 'partialSync', () => {
      this.isFetchingPartial = false
    })
    const { fetch } = this.options
    await Promise.all(
      [
        fetch(this.url())
          .then(response => response.json())
          .then(data => {
            data[PARTIAL_KEY] = false
            this.set(data, { silent: true })
          }),
      ].concat(
        this.relations
          .filter(relation => isRelationPartial(this, relation))
          .map(relation => {
            if (relation.makeSomethingUp) {
            } else {
              return fetch(`${this.url()}/${relation.key}`)
                .then(response => response.json())
                .then(data => {
                  if (relation.type === Backbone.Many) {
                    data = data.map(model => {
                      model[PARTIAL_KEY] = false
                      return model
                    })
                  } else {
                    data[PARTIAL_KEY] = false
                  }
                  this.set(relation.key, data, { silent: true })
                })
                .then(() => {
                  this.get(relation.key).trigger('partialSync')
                })
            }
          })
      )
    )
    this.trigger('partialSync')
  },
  /**
   * Remove the isPartial field before rendering (if a view) or saving to the backend
   */
  toJSON() {
    const data = Backbone.AssociatedModel.prototype.toJSON.apply(
      this,
      arguments
    )
    delete data[PARTIAL_KEY]
    return data
  },
})

Backbone.Collection.prototype.isPartial = function() {
  return this.some(
    model => typeof model.isPartial === 'function' && model.isPartial()
  )
}
