const CustomElements = require('js/CustomElements');
const Marionette = require('marionette');
import React from 'react';

module.exports = Marionette.ItemView.extend({
    tagName: CustomElements.register('dev-jsx-guide-example'),
    template() {
        return (
            <React.Fragment> {/* surround with multiple child roots with this to avoid wrapper divs */}
                {this.jsxTemplate()}
            </React.Fragment>
        )   
    },
    jsxTemplate() {
        const arrayToLoop = ['hi', 'this', 'is', 'an', 'array']
        return (
            <React.Fragment> {/* surround with multiple child roots with this to avoid wrapper divs */}
                <div className="class1"></div> {/* use className instead of class */}
                <div>
                    {
                        arrayToLoop.map((value) => {
                             // key is recommended, but not necessary since react doesn't control our rendering yet
                            return (
                                <React.Fragment key={value}> {/* surround with multiple child roots with this to avoid wrapper divs */}
                                    <div>Value:</div>
                                    <div>{value}</div>
                                </React.Fragment>
                            )
                        })
                    }
                </div>
                <div>
                    {
                        arrayToLoop.map((value) => {
                            // key is recommended, but not necessary since react doesn't control our rendering yet
                            return (
                                <div key={value}>{value}</div>
                            )
                        })
                    }
                </div>
            </React.Fragment>
        )
    }
});