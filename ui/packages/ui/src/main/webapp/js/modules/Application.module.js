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

define(['js/application', 'require'], function(Application, require) {
  Application.App.module('Applications', function(ApplicationModule, App) {
    this.startWithParent = false
    require([
      'js/controllers/App.controller',
      'js/controllers/AppDetail.controller',
    ], function(AppController, AppDetailController) {
      // Define a controller to run this module
      // --------------------------------------

      // Initialize this module when the app starts
      // ------------------------------------------

      ApplicationModule.addInitializer(function() {
        ApplicationModule.controllers = {}
        ApplicationModule.controllers.appController = new AppController({
          regions: {
            applications: App.applications,
          },
        })
        ApplicationModule.controllers.appDetailController = new AppDetailController(
          {
            regions: {
              applications: App.applications,
            },
          }
        )

        // display main app home.
        ApplicationModule.controllers.appController.show()
      })
    })
  })
})
