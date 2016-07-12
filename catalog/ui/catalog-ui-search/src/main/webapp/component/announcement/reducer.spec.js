var reducer = require('./reducer');
var actions = require('./actions');

var mock = function (type, message) {
    return {
        title: 'Title',
        type: type || 'error',
        message: message || 'Unknown message.'
    }
};

var apply = function (fns) {
    return function () {
        fns.shift().apply(null, arguments);
    };
};

describe('Announcement reducer', function () {
    it('should start empty', function () {
        expect(reducer()).to.deep.equal([]);
    });

    it('should add a new announcement', function (done) {
        var state = reducer();
        var thunk = actions.announce(mock());
        thunk(function (action) {
            state = reducer(state, action);
            expect(state).to.have.lengthOf(1);
            done();
        });
    });

    it('should dissmiss if not error', function (done) {
        var state = reducer();
        var m = mock('warn');
        var thunk = actions.announce(m, 1);

        thunk(apply([function (action) {
            state = reducer(state, action);
            expect(state).to.have.lengthOf(1);
        }, function (action) {
            state = reducer(state, action);
            expect(state).to.have.lengthOf(0);
            done();
        }]));
    });

    it('should remove an announcement', function () {
        var state = reducer();
        var m = mock();
        var thunk = actions.announce(m);
    });
});
