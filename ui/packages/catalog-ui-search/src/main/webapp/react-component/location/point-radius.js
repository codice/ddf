const React = require('react');

const { Radio, RadioItem } = require('../radio');
const TextField = require('../text-field');

const { Units, Zone, Hemisphere } = require('./common');

const PointRadiusLatLon = (props) => {
    const { lat, lon, radius, radiusUnits, cursor } = props;
    return (
        <div>
            <TextField label="Latitude" value={lat} onChange={cursor('lat')} addon="°" />
            <TextField label="Longitude" value={lon} onChange={cursor('lon')} addon="°" />
            <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
                <TextField label="Radius" value={radius} onChange={cursor('radius')} />
            </Units>
        </div>
    );
};

const PointRadiusUsngMgrs = (props) => {
    const { usng, radius, radiusUnits, cursor } = props;
    return (
        <div>
            <TextField label="USNG / MGRS" value={usng} onChange={cursor('usng')} />
            <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
                <TextField label="Radius" value={radius} onChange={cursor('radius')} />
            </Units>
        </div>
    );
};

const PointRadiusUtm = (props) => {
    const { utmEasting, utmNorthing, utmZone, utmHemisphere, radius, radiusUnits, cursor } = props;
    return (
        <div>
            <TextField
                label="Easting"
                value={utmEasting}
                onChange={cursor('utmEasting')}
                addon="m"
            />
            <TextField
                label="Northing"
                value={utmNorthing}
                onChange={cursor('utmNorthing')}
                addon="m"
            />
            <Zone value={utmZone} onChange={cursor('utmZone')} />
            <Hemisphere value={utmHemisphere} onChange={cursor('utmHemisphere')} />
            <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
                <TextField label="Radius" value={radius} onChange={cursor('radius')} />
            </Units>
        </div>
    );
};

const PointRadius = (props) => {
    const { cursor, locationType } = props;

    const inputs = {
        latlon: PointRadiusLatLon,
        usng: PointRadiusUsngMgrs,
        utm: PointRadiusUtm
    };

    const Component = inputs[locationType] || null;

    return (
        <div>
            <Radio value={locationType} onChange={cursor('locationType')}>
                <RadioItem value="latlon">Lat / Lon</RadioItem>
                <RadioItem value="usng">USNG / MGRS</RadioItem>
                <RadioItem value="utm">UTM</RadioItem>
            </Radio>
            <div className="input-location">
                {Component !== null ? <Component {...props} /> : null}
            </div>
        </div>
    );
};

module.exports = PointRadius;
