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

'use strict'
;(function($) {
  const cometd = $.cometd;
  cometd.websocketEnabled = false

  // CometD endpoint is assumed to be at localhost, change if otherwise
  const cometURL = 'https://localhost:8993/search/cometd';

  $(document).ready(function() {
    function _connectionEstablished() {
      $('#notifications').append('<div>CometD Connection Established</div>')
    }

    function _connectionBroken() {
      $('#notifications').append('<div>CometD Connection Broken</div>')
    }

    function _connectionClosed() {
      $('#notifications').append('<div>CometD Connection Closed</div>')
    }

    // Function that manages the connection status with the Bayeux server
    let _connected = false;
    function _metaConnect(message) {
      if (cometd.isDisconnected()) {
        _connected = false
        _connectionClosed()
        return
      }

      const wasConnected = _connected;
      _connected = message.successful === true
      if (!wasConnected && _connected) {
        _connectionEstablished()
      } else if (wasConnected && !_connected) {
        _connectionBroken()
      }
    }

    // Function invoked when first contacting the server and
    // when the server has lost the state of this client
    function _metaHandshake(handshake) {
      if (handshake.successful === true) {
        /* Since we are attempting to retrieve persisted notifications before
              other operations, use batch messages to ensure processing order. */
        cometd.batch(function() {
          // Add Notification Subscription
          cometd.subscribe('/ddf/notifications/**', function(message) {
            console.log('Got a message!')
            console.log('Message id: ' + message.data.id)

            // Add Notification
            getNotification(message)
          })
          const cid = cometd.getClientId();
          $('#notifications').append('<div>Cid: ' + cid + '</div>')
          // Publish an empty message to the notifications channel to retrieve any existing notifications.
          cometd.publish('/ddf/notifications', {})
          const a = $('a').attr('href');
          $('a').attr('href', a + cid)
        })
      }
    }

    // Displays new notifications to the user.
    function addNotification(message) {
      // Print out the raw JSON
      console.log('Adding Notification - Message JSON: ')
      console.log(JSON.stringify(message, null, 4))
      document.getElementById('json-responses').appendChild(renderjson(message))

      // Grab notification elements
      const notifyId = message.data.id, notifyTitle = message.data.title, notifyUser = message.data.user, notifyTime = message.data.timestamp, notifyMessage = message.data.message;

      // Add New Notification
      $('#notifications').append(
        '<div id="' + notifyId + '" class="notification"></div>'
      )
      $('#' + notifyId).append(
        '<h3 class="notify-title">' + notifyTitle + '</h3>'
      )
      $('#' + notifyId).append(
        '<p class="notify-user">User: ' + notifyUser + '</p>'
      )
      $('#' + notifyId).append(
        '<p class="notify-time">Time: ' + notifyTime + '</p>'
      )
      $('#' + notifyId).append(
        '<p class="notify-message">Message: ' + notifyMessage + '</p>'
      )
    }

    // Update notifications.
    function updateNotification(message) {
      // Print out the raw JSON
      console.log('Updating Notification - Message JSON: ')
      console.log(JSON.stringify(message, null, 4))
      document.getElementById('json-responses').appendChild(renderjson(message))

      const notifyId = message.data.id, notifyTitle = message.data.title, notifyUser = message.data.user, notifyTime = message.data.timestamp, notifyMessage = message.data.message;

      // Update existing notification
      $('#' + notifyId).replaceWith(
        '<div id="' + notifyId + '" class="notification"></div>'
      )
      $('#' + notifyId).append(
        '<h3 class="notify-title">' + notifyTitle + '</h3>'
      )
      $('#' + notifyId).append(
        '<p class="notify-user">User: ' + notifyUser + '</p>'
      )
      $('#' + notifyId).append(
        '<p class="notify-time">Time: ' + notifyTime + '</p>'
      )
      $('#' + notifyId).append(
        '<p class="notify-message">Message: ' + notifyMessage + '</p>'
      )
    }

    // When queries are executed a guid must be provided to be used as the response channel.
    // Create a "guid"
    function guid() {
      function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
          .toString(16)
          .substring(1)
      }
      return (
        s4() +
        s4() +
        '-' +
        s4() +
        '-' +
        s4() +
        '-' +
        s4() +
        '-' +
        s4() +
        s4() +
        s4()
      )
    }

    // Determines whether a notification is new or needs to be updated.
    function getNotification(message) {
      const notifyId = message.data.id;

      // If notification already exists, update it. Else display a new notification.
      if ($('#' + notifyId).length > 0) {
        updateNotification(message)
      } else {
        addNotification(message)
      }
    }

    // Add Query results to page
    function addResult(message) {
      console.log('Results: ')
      console.log(JSON.stringify(message, null, 4))
      document.getElementById('json-responses').appendChild(renderjson(message))

      const results = message.data.results.length;
      console.log('Result Length: ' + results)
      for (let i = 0; i < results; i++) {
        // get result elements
        const result = message.data.results[i], metacard = result.metacard, metaProps = metacard.properties, catalogId = metaProps.id, metaTitle = metaProps.title, cid = cometd.getClientId(), downloadUrl = metaProps['resource-download-url'] + '&session=' + cid;

        $('#results').append('<div class="result" id=' + catalogId + '></div>')
        $('#' + catalogId).append('<h3>' + metaTitle + '</h3>')
        $('#' + catalogId).append(
          '<a class="btn" target="_blank" href="' +
            downloadUrl +
            '">Download</a>'
        )
      }
    }

    // Execute query over cometd
    function executeQuery() {
      console.log('Executing Query')
      $('#results').empty()

      const keyword = $('#keyword').val(),
            uuid = guid(),
            guidChannel = '/' + uuid,
            request = {
              id: uuid,
              cql: "anyText LIKE '" + keyword + "'",
            };

      // Add query response subscription
      console.log('Query Request: ')
      console.log(JSON.stringify(request, null, 4))
      document.getElementById('json-responses').appendChild(renderjson(request))

      cometd.subscribe(guidChannel, function(message) {
        addResult(message)
      })

      // Publish Query Message
      cometd.publish('/service/query', request)
    }

    // Disconnect when the page unloads
    $(window).unload(function() {
      cometd.disconnect(true)
    })

    cometd.configure({
      url: cometURL,
      logLevel: 'debug',
    })

    cometd.addListener('/meta/handshake', _metaHandshake)
    cometd.addListener('/meta/connect', _metaConnect)

    cometd.handshake()

    //Add search event handler
    const searchBtn = document.getElementById('search-btn');
    searchBtn.addEventListener('click', executeQuery)
  })
})(jQuery)
