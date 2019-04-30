const $ = require('jquery')
const api = require('./index')
const oldGet = $.get
const oldPost = $.post
const oldAjax = $.ajax

const mock = () => {
  const httpRequest = ({ url }) => {
    return Promise.resolve(api(url))
  }
  $.get = url => httpRequest({ url })
  $.post = httpRequest
  $.ajax = httpRequest
}

const unmock = () => {
  $.get = oldGet
  $.post = oldPost
  $.ajax = oldAjax
}

export { mock, unmock }
