const React = require('react');

const { Button, Radio, RadioItem } = require('./inputs');

const { WKT, LatLongDD, LatLongDMS, USNG } = require('./geo-components');
const plugin = require('plugins/location');
const produce = require('immer').default;

const inputs = plugin({
    wkt: {
        label: 'WKT',
        Component: WKT
    },
    dd: {
        label: 'Lat/Lon (DD)',
        Component: LatLongDD
    },
    dms: {
        label: 'Lat/Lon (DMS)',
        Component: LatLongDMS
    },
    usng: {
        label: 'USNG/MGRS',
        Component: USNG
    }
});

const drawTypes = ['wkt', 'dd', 'dms', 'usng'];

const Form = ({ children }) => <div className="form-group clearfix">{children}</div>;

const LocationInput = (props) => {
    const { mode, valid, error, showErrors, setState } = props;
    const input = inputs[mode] || {};
    const { Component = null } = input;

    return (
        <div>
            <Radio value={mode} onChange={setState((draft, value) => draft.mode = value)}>
                {Object.keys(inputs).map((key) => (
                    <RadioItem key={key} value={key}>
                        {inputs[key].label}
                    </RadioItem>
                ))}
            </Radio>
            <Form>
                {Component !== null ? <Component {...props} /> : null}
                {!valid && showErrors ? (
                    <div className='for-error'>
                        <span className='fa fa-exclamation-triangle' /> {error}
                    </div>
                ) : null }
            </Form>

        </div>
    );
};

module.exports = ({ state, setState, options }) => (
    <LocationInput
        {...state}
        onDraw={options.onDraw}
        setState={(producer) => (value) => {
            const nextState = produce(state, draft => { producer(draft, value) });
            setState(nextState);
        }}
    />
);
