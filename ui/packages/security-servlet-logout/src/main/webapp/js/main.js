;(function() {
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
    logout(actions).catch(error => {
      console.log(error.message)
      $('#error-msg').show()
    })
  })
})()

async function logout(actions) {
  let localLogoutAction
  let externalLogoutActions = false
  for (const action of actions) {
    if (action.title === 'Local Logout') {
      localLogoutAction = action
    } else {
      externalLogoutActions = true
      // Certain protocols, e.g. SAML with the POST binding, use redirects that can't be followed
      // with an AJAX request. For logout to work, we need to load the url in an iframe.
      const iframe = document.createElement('iframe')
      iframe.style.display = 'none'
      iframe.src = action.url
      document.body.appendChild(iframe)
    }
  }

  // Wait 3 seconds for iframe logout
  if (externalLogoutActions) {
    await new Promise(r => setTimeout(r, 3000))
  }

  if (localLogoutAction) {
    const response = await $.ajax(localLogoutAction.url)
    if (response.mustCloseBrowser === true) {
      $('#close-browser-msg').show()
      return
    }
  }

  const queryParams = parseQueryParams()
  if (queryParams && queryParams['service']) {
    $('#link').attr('href', queryParams['service'])
  } else {
    $('#link').attr('href', window.location.href.replace(/logout\/.*/, ''))
  }
  $('#standard-msg').show()
}

function parseQueryParams() {
  const searchString = (window.location.search + '').split('?')

  if (searchString[1]) {
    const searchParams = searchString[1].split('&')

    return searchParams.reduce((map, param) => {
      const paramKvPair = param.split('=')

      if (paramKvPair.length === 2) {
        map[paramKvPair[0]] = decodeURIComponent(
          paramKvPair[1].split('+').join(' ')
        )
      }

      return map
    }, {})
  }
}
