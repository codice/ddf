define([
  'marionette',
  'text!./query-editor.hbs',
  'component/tabs/simple/simple',
  './basic-editor/basic-editor.view',
  'js/store'
], function (Marionette, queryEditor, SimpleTabs, BasicEditorView, store) {

  var selected = store.get('selected')

  var tabs = {
    Basic: BasicEditorView,
    Advanced: BasicEditorView,
    Preview: BasicEditorView,
    Updates: BasicEditorView,
    Status: BasicEditorView
  }

  var QueryEditorView = Marionette.LayoutView.extend({
    template : queryEditor,
    className: 'panel-content query-editor',

    regions : {
      queryTabsRegion: '#query-tabs'
    },

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click #close-query-editor': 'closeQuery'
    },

    closeQuery: function () {
      selected.reset()
    },

    serializeData: function () {
      return {
        title: 'New Query'
      }
    },

    initialize: function () {
    },

    onBeforeShow: function () {
      var m = this.model
      this.queryTabsRegion.show(new SimpleTabs({
        tabs: Object.keys(tabs),
        getContent: function (tab) {
          return new tabs[tab]({ model: m })
        }
      }))

      //this.queryTabsRegion.show(new BasicEditorView())
    }
  })

  return QueryEditorView
})
