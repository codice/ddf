define([
    'jquery',
    'backbone',
    'marionette',
    'underscore',
    'properties',
    'wreqr',
    './metacard-notes.hbs',
    'maptype',
    'js/store',
    'js/CustomElements',
    'component/property/property.view',
    'component/property/property',
    'component/note/note.collection',
    'component/note/note.collection.view',
    'component/input/textarea/input-textarea.view',
    'component/loading-companion/loading-companion.view',
    'component/announcement'
], function ($, Backbone, Marionette, _, properties, wreqr, template, maptype,
             store, CustomElements, PropertyView, Property, NoteCollection, NoteCollectionView,
             TextAreaView, LoadingCompanionView, announcement) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function () {
            this.model = this.selectionInterface.getSelectedResults().first();
            this._metacardId = this.model.get('metacard').get('id');
        },
        regions: {
            addNoteField: '.add-note-field',
            notesList: '.notes-list',
            notesFeedback: '.notes-feedback'
        },
        template: template,
        tagName: CustomElements.register('metacard-notes'),
        selectionInterface: store,
        _notesCollection: undefined,
        _notes: undefined,
        _metacardId: undefined,
        _duration: undefined,
        events: {
            'click .add-note-btn ': 'handleCreate',
            'click .refresh-btn ': 'handleRefresh'
        },
        childEvents: {
            "note:add": "handleChildDelete"
        },
        initialize: function (options) {
            this.selectionInterface = options.selectionInterface || this.selectionInterface;
            if (!options.model) {
                this.setDefaultModel();
            }
        },
        showNotesListView: function () {
            this.notesList.show(new NoteCollectionView({
                collection: this._notesCollection,
                selectionInterface: this.selectionInterface,
                parent: this.model
            }));
            this.notesList.currentView.$el.addClass("is-list");
        },
        onBeforeShow: function () {
            this.clearNotes();
            this.showAddNoteView();
            this.getNotesForMetacard();
            this.showNotesListView();
            this.turnOnEditing();
            this.setupListeners();
        },
        setupListeners: function () {
            this.listenTo(this._notesCollection, "remove", function () {
                this.checkHasNotes();
            });
        },
        showAddNoteView: function () {
            this.addNoteField.show(new PropertyView({
                model: new Property({
                    value: [''],
                    placeholder: 'enter note',
                    type: 'TEXTAREA',
                    label: ''
                })
            }));
        },
        getNotesForMetacard: function () {
            LoadingCompanionView.beginLoading(this);
            $.get('/search/catalog/internal/notes/' + this._metacardId)
                .then(function (response) {
                    var resp = response.response;
                    if (response.responseType === "success") {
                        if (this.isValidResponse(resp)) {
                            this._notes = JSON.parse(resp);
                            this.parseNotes();
                        } else {
                            announcement.announce({
                                title: 'Error!',
                                message: "There was an error retrieving the notes for this item!",
                                type: 'error'
                            });
                        }
                    }
                    LoadingCompanionView.endLoading(this);
                    this.checkHasNotes();
                }.bind(this));
        },
        handleRefresh: function () {
            this.getNotesForMetacard();
            announcement.announce({
                title: 'Success!',
                message: 'Updated the notes list!',
                type: 'success'
            });
        },
        checkHasNotes: function () {
            if (this._notesCollection.length > 0) {
                this.$el.toggleClass("has-no-notes", false);
            } else {
                this.$el.toggleClass("has-no-notes", true);
            }
        },
        parseNotes: function () {
            this.clearNotes();
            this._notes.forEach(function (note) {
                this._notesCollection.add({
                    id: note.id,
                    parent: note.parent.id,
                    created: note.created,
                    modified: note.modified,
                    note: note.note,
                    owner: note.owner
                })
            }.bind(this));
        },
        clearNotes: function () {
            if (!this._notesCollection) {
                this._notesCollection = new NoteCollection();
            }
            this._notesCollection.reset();
        },
        turnOnEditing: function () {
            this.addNoteField.currentView.turnOnEditing();
        },
        turnOffEditing: function () {
            this.$el.toggleClass('is-editing', false);
        },
        handleCreate: function () {
            var note = this.addNoteField.currentView.model.get('value')[0];
            var noteObj = {};
            noteObj.parent = this._metacardId;
            noteObj.note = note;

            if (note !== "") {
                LoadingCompanionView.beginLoading(this);
                $.ajax({
                    url: '/search/catalog/internal/notes',
                    data: JSON.stringify(noteObj),
                    method: 'POST',
                    contentType: 'application/json'
                }).always(function (response) {
                    var resp = response.response;
                    setTimeout(function () {
                        if (response.responseType === "success") {
                            if (this.isValidResponse(resp)) {
                                this.handlePostResponse(resp);
                                announcement.announce({
                                    title: 'Created!',
                                    message: 'New note has been created.',
                                    type: 'success'
                                });
                                if (!this.isDestroyed) {
                                    this.addNoteField.currentView.revert();
                                }
                            }
                        } else {
                            announcement.announce({
                                title: 'Error!',
                                message: resp,
                                type: 'error'
                            });
                        }
                        LoadingCompanionView.endLoading(this);
                        this.checkHasNotes();
                    }.bind(this), 1000);
                }.bind(this));
            } else {
                announcement.announce({
                    title: 'Error!',
                    message: 'Note was empty. Can not create!',
                    type: 'error'
                });
            }
        },
        isValidResponse: function(response) {
            return response !== "";
        },
        handlePostResponse: function (response) {
            var note = JSON.parse(response);
            
            this._notesCollection.add({
                id: note.id,
                parent: note.parent.id,
                created: note.created,
                modified: note.modified,
                note: note.note,
                owner: note.owner
            });
        }
    });
});
