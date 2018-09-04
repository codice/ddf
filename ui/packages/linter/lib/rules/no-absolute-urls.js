/**
 * @fileoverview Requires all requests to be relative to their current context path
 * @author Brendan
 */
'use strict'

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

module.exports = {
  meta: {
    docs: {
      description: 'Requires all requests to be relative to their current context path',
      category: 'Fill me in',
      recommended: false
    },
    fixable: null, // or "code" or "whitespace"
    schema: [
      // fill in your schema
    ]
  },

  create: function (context) {
    // variables should be defined here

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    // any helper functions should go here or else delete this section

    // ----------------------------------------------------------------------
    // Public
    // ----------------------------------------------------------------------

    return {
      BinaryExpression (node) {
        while (node.type === 'BinaryExpression') {
          node = node.left
        }
        let cometd = context.getAncestors().filter(parent => parent.type === 'CallExpression' && parent.callee && parent.callee.object && parent.callee.object.object && parent.callee.object.object.name === 'Cometd')
        if (cometd.length === 0 && node.type === 'Literal' && typeof node.value === 'string' && node.value.startsWith('/')) {
          context.report(node, 'Do not concatenate absolute URLs')
        }
      },
      Literal (node) {
        let cometd = context.getAncestors().filter(parent => parent.type === 'CallExpression' && parent.callee && parent.callee.object && parent.callee.object.object && parent.callee.object.object.name === 'Cometd')
        if (cometd.length === 0 && node.parent.type !== 'BinaryExpression' && typeof node.value === 'string' && node.value.match('^/\\w') && !node.value.match('/service/\\w')) {
          context.report(node, 'Do not use absolute URLs')
        }
      }
    }
  }
}
