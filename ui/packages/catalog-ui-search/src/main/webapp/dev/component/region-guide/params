behaviors: {
    region: {
        regions: [
            {
                selector: '> div.test', // string (REQUIRED) should be unique as well
                view: MarionetteView, // can be a Marionette view or a function that returns one (or undefined),
                viewOptions: {}, // options to pass the view or a function that returns options
                destroyIfMissing: false, // defaults to false if missing, set to true to destroy if a render results in the selector disappearing
                shouldRegionUpdate: false, // defaults to false if missing, set to function if you want to be notified on renders (function will recieve currentView of region if it exists and should return true or false) 
            }
        ]
    }
}

or 

behaviors() {
    return {
        region: {
            regions: [
                {
                    selector: '> div.test', // string (REQUIRED) should be unique as well
                    view: MarionetteView, // can be a Marionette view or a function that returns one (or undefined),
                    viewOptions: {}, // options to pass the view or a function that returns options
                    destroyIfMissing: false, // defaults to false if missing, set to true to destroy if a render results in the selector disappearing
                    shouldRegionUpdate: false, // defaults to false if missing, set to function if you want to be notified on renders (function will recieve currentView of region if it exists and should return true or false) 
                }
            ]
        }
    }
}

or 

behaviors() {
    return {
        region: {
            regions: [
                {
                    selector: '> div.test', // string (REQUIRED) should be unique as well
                    view() {
                        if (blah) {
                            return MarionetteView1
                        } else if (blah2) {
                            return MarionetteView2
                        } else {
                            return undefined;
                        }
                    }, // can be a Marionette view or a function that returns one (or undefined),
                    viewOptions() {
                        return {
                            model: new Model(),
                            selectionInterface: this.options.selectionInterface
                        }
                    }, // options to pass the view or a function that returns options
                    destroyIfMissing: true, // defaults to false if missing, set to true to destroy if a render results in the selector disappearing
                    shouldRegionUpdate(currentView) {
                        if (currentView.model !== otherModel) {
                            return true;
                        } else {
                            return false;
                        }
                    }, // defaults to false if missing, set to function if you want to be notified on renders (function will recieve currentView of region if it exists and should return true or false) 
                }
            ]
        }
    }
}

or 

behaviors() {
    return {
        region: {
            regions: [
                {
                    selector: '> div.test', // string (REQUIRED) should be unique as well
                    view() {
                        if (blah) {
                            return MarionetteView1
                        } else if (blah2) {
                            return MarionetteView2
                        } else {
                            return undefined;
                        }
                    }, // can be a Marionette view or a function that returns one (or undefined),
                    viewOptions() {
                        return {
                            model: new Model(),
                            selectionInterface: this.options.selectionInterface
                        }
                    }, // options to pass the view or a function that returns options
                    /* leaving off destroyIfMissing and shouldComponentUpdate is okay!  
                        Most times you'll want a static view with static options.  For those advanced cases though, you can utilize 
                        passing functions.  shouldComponentUpdate will allow you to reinitialize a view if you think it needs some new information,
                        it gets called whenever the view with the regions behavior renders.
                    */
                }
            ]
        }
    }
}