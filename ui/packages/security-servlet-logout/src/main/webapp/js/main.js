;(function() {
  const processActions = require('@connexta/atlas/atoms/logout')
  var prevUrl = $.url().param('prevurl')

  $.get('../services/platform/config/ui', function(data) {
    $('.nav img').attr('src', 'data:image/png;base64,' + data.productImage)
    $('.nav label').attr('title', data.version)
    $('.nav label:first-of-type').append(data.version)
    $('.nav label button').click(function() {
      window.location.href =
        window.location.origin +
        (prevUrl !== undefined && prevUrl !== 'undefined'
          ? decodeURI(prevUrl)
          : '')
    })
  })

  $.get('../services/logout/actions', function(data) {
    var actions = JSON.parse(data)
    processActions({ actions: actions })
  })

  $('#landinglink').click(function() {
    window.location.href = window.location.href.replace(/logout\/.*/, '')
  })
})()
