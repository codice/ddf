;(function() {
  var searchString = (window.location.search + '').split('?')

  if (searchString[1] !== undefined) {
    var searchParams = searchString[1].split('&')

    searchParams.forEach(function(paramString) {
      var param = paramString.split('=')

      if (param[0] === 'msg') {
        $('#extramessage')
          .text(decodeURIComponent(param[1].split('+').join(' ')))
          .html()
      }
      //add additional params here if needed
    })
  }

  $('#landinglink').click(function() {
    window.location.href = window.location.href.replace(/logout\/.*/, '')
  })
})()
