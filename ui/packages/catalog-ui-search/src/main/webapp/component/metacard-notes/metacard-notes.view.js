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
const template = require('./metacard-notes.hbs')
const store = require('../../js/store.js')
const CustomElements = require('../../js/CustomElements.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const NoteCollection = require('component/note/note.collection')
const NoteCollectionView = require('component/note/note.collection.view')
const LoadingCompanionView = require('../loading-companion/loading-companion.view.js')
const announcement = require('../announcement/index.jsx')

module.exports = Marionette.LayoutView.extend({
  setDefaultModel() {
    this.model = this.selectionInterface.getSelectedResults().first()
    this._metacardId = this.model.get('metacard').get('id')
  },
  regions: {
    addNoteField: '.add-note-field',
    notesList: '.notes-list',
    notesFeedback: '.notes-feedback',
  },
  template,
  tagName: CustomElements.register('metacard-notes'),
  selectionInterface: store,
  _notesCollection: undefined,
  _notes: undefined,
  _metacardId: undefined,
  _duration: undefined,
  events: {
    'click .add-note-btn ': 'handleCreate',
    'click .refresh-btn ': 'handleRefresh',
  },
  childEvents: {
    'note:add': 'handleChildDelete',
  },
  initialize(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    if (!options.model) {
      this.setDefaultModel()
    }
  },
  showNotesListView() {
    this.notesList.show(
      new NoteCollectionView({
        collection: this._notesCollection,
        selectionInterface: this.selectionInterface,
        parent: this.model,
      })
    )
    this.notesList.currentView.$el.addClass('is-list')
  },
  onBeforeShow() {
    this.clearNotes()
    this.showAddNoteView()
    this.getNotesForMetacard()
    this.showNotesListView()
    this.turnOnEditing()
    this.setupListeners()
  },
  setupListeners() {
    this.listenTo(this._notesCollection, 'remove', function() {
      this.checkHasNotes()
    })
  },
  showAddNoteView() {
    this.addNoteField.show(
      new PropertyView({
        model: new Property({
          value: [''],
          placeholder: 'enter note',
          type: 'TEXTAREA',
          label: '',
        }),
      })
    )
  },
  getNotesForMetacard() {
    LoadingCompanionView.beginLoading(this)
    $.get('./internal/notes/' + this._metacardId).then(response => {
      const resp = response.response
      if (response.responseType === 'success') {
        if (this.isValidResponse(resp)) {
          this._notes = JSON.parse(resp)
          this.parseNotes()
        } else {
          announcement.announce({
            title: 'Error!',
            message: 'There was an error retrieving the notes for this item!',
            type: 'error',
          })
        }
      }
      LoadingCompanionView.endLoading(this)
      this.checkHasNotes()
    })
  },
  handleRefresh() {
    this.getNotesForMetacard()
    announcement.announce({
      title: 'Success!',
      message: 'Updated the notes list!',
      type: 'success',
    })
  },
  checkHasNotes() {
    if (this._notesCollection.length > 0) {
      this.$el.toggleClass('has-no-notes', false)
    } else {
      this.$el.toggleClass('has-no-notes', true)
    }
  },
  parseNotes() {
    this.clearNotes()
    this._notes.forEach(note => {
      this._notesCollection.add({
        id: note.id,
        parent: note.parent.id,
        created: note.created,
        modified: note.modified,
        note: note.note,
        owner: note.owner,
      })
    })
  },
  clearNotes() {
    if (!this._notesCollection) {
      this._notesCollection = new NoteCollection()
    }
    this._notesCollection.reset()
  },
  turnOnEditing() {
    this.addNoteField.currentView.turnOnEditing()
  },
  turnOffEditing() {
    this.$el.toggleClass('is-editing', false)
  },
  handleCreate() {
    const note = this.addNoteField.currentView.model.get('value')[0]
    const noteObj = {}
    noteObj.parent = this._metacardId
    noteObj.note = note

    if (note !== '') {
      LoadingCompanionView.beginLoading(this)
      $.ajax({
        url: './internal/notes',
        data: JSON.stringify(noteObj),
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
                message: 'New note has been created.',
                type: 'success',
              })
              if (!this.isDestroyed) {
                this.addNoteField.currentView.revert()
              }
            }
          } else {
            announcement.announce({
              title: 'Error!',
              message: resp,
              type: 'error',
            })
          }
          LoadingCompanionView.endLoading(this)
          this.checkHasNotes()
        }, 1000)
      })
    } else {
      announcement.announce({
        title: 'Error!',
        message: 'Note was empty. Can not create!',
        type: 'error',
      })
    }
  },
  isValidResponse(response) {
    return response !== ''
  },
  handlePostResponse(response) {
    const note = JSON.parse(response)

    this._notesCollection.add({
      id: note.id,
      parent: note.parent.id,
      created: note.created,
      modified: note.modified,
      note: note.note,
      owner: note.owner,
    })
  },
})
