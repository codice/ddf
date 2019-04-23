const Backbone = require('backbone')
module.exports = Backbone.Model.extend({
  defaults: {
    options: [
      {
        label: 'No',
        value: 'no',
      },
      {
        label: 'Yes',
        value: 'yes',
      },
    ],
    value: undefined,
    isEditing: true,
  },
})
