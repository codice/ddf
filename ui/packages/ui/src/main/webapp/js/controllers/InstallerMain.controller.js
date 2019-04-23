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

define(['backbone.marionette', 'js/models/InstallProfile', 'q'], function(
  Marionette,
  InstallProfile,
  Q
) {
  const InstallMainController = Marionette.Controller.extend({
    installProfiles: null,

    fetchInstallProfiles: function() {
      const defer = Q.defer();

      if (this.installProfiles) {
        defer.resolve(this.installProfiles)
      } else {
        this.installProfiles = new InstallProfile.Collection()

        this.installProfiles.fetch({
          success: function(collection) {
            defer.resolve(collection)
          },
          failure: function() {
            defer.reject(
              new Error('There was an error fetching the installation items.')
            )
          },
        })
      }

      return defer.promise
    },
  });

  return InstallMainController
})
