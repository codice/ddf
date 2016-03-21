define([
  'marionette',
  'text!./basic-editor.hbs',
], function (Marionette, basicEditor) {

  var BasicEditorView = Marionette.ItemView.extend({
    template: basicEditor,
    className: 'basic-editor'
  })

  return BasicEditorView
})
