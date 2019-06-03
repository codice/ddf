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

const $ = require('jquery')
const Marionette = require('marionette')
const template = require('./query-annotations.hbs')
const store = require('../../js/store.js')
const CustomElements = require('../../js/CustomElements.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const AnnotationCollection = require('../annotation/annotation.collection.js')
const AnnotationCollectionView = require('../annotation/annotation.collection.view.js')
const LoadingCompanionView = require('../loading-companion/loading-companion.view.js')
const announcement = require('../announcement/index.jsx')

module.exports = Marionette.LayoutView.extend({
  setDefaultModel() {
    this.model = this.selectionInterface.getSelectedResults().first()
  },
  template,
  tagName: CustomElements.register('query-annotations'),
  events: {
    'click .add-annotation-btn ': 'handleCreate',
    'click .refresh-btn ': 'handleRefresh',
  },
  childEvents: {
    'annotation:add': 'handleChildDelete',
  },
  regions: {
    addAnnotationField: '.add-annotation-field',
    annotationsList: '.annotations-list',
  },
  _annotationsCollection: undefined,
  _annotations: undefined,
  initialize(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    if (!options.model) {
      this.setDefaultModel()
    }
  },
  onBeforeShow() {
    this.clearAnnotations()
    this.showAddAnnotationView()
    this.getAnnotationsForQuery()
    this.showAnnotationsListView()
    this.turnOnEditing()
    this.setupListeners()
  },
  clearAnnotations() {
    if (!this._annotationsCollection) {
      this._annotationsCollection = new AnnotationCollection()
    }
    this._annotationsCollection.reset()
  },
  showAddAnnotationView() {
    this.addAnnotationField.show(
      new PropertyView({
        model: new Property({
          value: [''],
          placeholder: 'enter annotation',
          type: 'TEXTAREA',
          label: '',
        }),
      })
    )
  },
  getAnnotationsForQuery() {
    LoadingCompanionView.beginLoading(this)
    $.get('./internal/annotations/' + this.model.get('id')).then(response => {
      const resp = response.response
      if (response.responseType === 'success') {
        if (this.isValidResponse(resp)) {
          this._annotations = JSON.parse(resp)
          this.parseAnnotations()
        } else {
          announcement.announce({
            title: 'Error!',
            message:
              'There was an error retrieving the annotations for this item!',
            type: 'error',
          })
        }
      }
      LoadingCompanionView.endLoading(this)
      this.checkHasAnnotations()
    })
  },
  showAnnotationsListView() {
    this.annotationsList.show(
      new AnnotationCollectionView({
        collection: this._annotationsCollection,
        selectionInterface: this.selectionInterface,
        parent: this.model,
      })
    )
    this.annotationsList.currentView.$el.addClass('is-list')
  },
  setupListeners() {
    this.listenTo(this._annotationsCollection, 'remove', function() {
      this.checkHasAnnotations()
    })
  },
  handleRefresh() {
    this.getAnnotationsForQuery()
    announcement.announce({
      title: 'Success!',
      message: 'Updated the annotations list!',
      type: 'success',
    })
  },
  checkHasAnnotations() {
    if (this._annotationsCollection.length > 0) {
      this.$el.toggleClass('has-no-annotations', false)
    } else {
      this.$el.toggleClass('has-no-annotations', true)
    }
  },
  isValidResponse(response) {
    return response !== ''
  },
  parseAnnotations() {
    this.clearAnnotations()
    this._annotations.forEach(annotation => {
      this._annotationsCollection.add({
        id: annotation.id,
        parent: annotation.parent,
        created: annotation.created,
        modified: annotation.modified,
        annotation: annotation.note,
        owner: annotation.owner,
      })
    })
  },
  clearAnnotations() {
    if (!this._annotationsCollection) {
      this._annotationsCollection = new AnnotationCollection()
    }
    this._annotationsCollection.reset()
  },
  handleCreate() {
    const annotation = this.addAnnotationField.currentView.model.get('value')[0]
    const annotationObj = {}
    annotationObj.parent = this.model.get('id')
    annotationObj.note = annotation
    annotationObj.workspace = store.getCurrentWorkspace().id

    if (annotation !== '') {
      LoadingCompanionView.beginLoading(this)
      $.ajax({
        url: './internal/annotations',
        data: JSON.stringify(annotationObj),
        method: 'POST',
        contentType: 'application/json',
      }).always(response => {
        const resp = response.response
        setTimeout(() => {
          if (response.responseType === 'success') {
            if (this.isValidResponse(resp)) {
              this.handlePostResponse(resp)
              announcement.announce({
                title: 'Created!',
                message: 'New annotation has been created.',
                type: 'success',
              })
              this.addAnnotationField.currentView.revert()
            }
          } else {
            announcement.announce({
              title: 'Error!',
              message: resp,
              type: 'error',
            })
          }
          LoadingCompanionView.endLoading(this)
          this.checkHasAnnotations()
        }, 1000)
      })
    } else {
      announcement.announce({
        title: 'Error!',
        message: 'annotation was empty. Can not create!',
        type: 'error',
      })
    }
  },
  handlePostResponse(response) {
    const annotation = JSON.parse(response)

    this._annotationsCollection.add({
      id: annotation.id,
      parent: annotation.parent,
      created: annotation.created,
      modified: annotation.modified,
      annotation: annotation.note,
      owner: annotation.owner,
    })
  },
  turnOnEditing() {
    this.addAnnotationField.currentView.turnOnEditing()
  },
  turnOffEditing() {
    this.$el.toggleClass('is-editing', false)
  },
})
