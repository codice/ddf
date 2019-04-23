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
;(function() {
  const prevUrl = $.url().param('prevurl')

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
    const actions = JSON.parse(data)

    const doLogout = function(action) {
      window.location.href = action.url
      $('#modal').removeClass('is-hidden')
    }

    if (actions.length !== 0) {
      $('#noActions').toggleClass('is-hidden', actions.length !== 0)
      $('#actions').toggleClass('is-hidden', actions.length === 0)
      actions.forEach(function(action) {
        if ($.url().param('noPrompt')) {
          doLogout(action)
        } else {
          const $row = $('<tr></tr>')
          const $realm = $('<td></td>')
          $realm.html(action.realm)
          const $realmDescription = $(
            '<a href="#" class="description" tabindex="-1"></a>'
          )
          $realmDescription.data('title', action.description)
          $realmDescription.data('placement', 'right')
          $realmDescription.append(
            $('<i class="glyphicon glyphicon-question-sign"></i>')
          )
          $realm.append($realmDescription)
          const $user = $('<td></td>')
          $user.html(action.auth)
          const $logout = $('<td></td>')
          const $button = $('<button>')
            .attr('id', 'logoutButton')
            .text('Logout')
            .addClass('btn btn-primary float-right')
            .click(function() {
              doLogout(action)
            })
          $logout.append($button)
          $row
            .append($realm)
            .append($user)
            .append($logout)
          $('#actions')
            .find('tbody')
            .append($row)
        }
      })

      $('.description').tooltip()
    }
  })

  $('#landinglink').click(function() {
    window.location.href = window.location.href.replace(/logout\/.*/, '')
  })
})()
