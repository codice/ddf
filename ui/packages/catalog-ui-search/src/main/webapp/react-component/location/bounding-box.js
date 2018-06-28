const React = require('react');

const Group = require('../group');
const Label = require('./label');
const TextField = require('../text-field');
const { Radio, RadioItem } = require('../radio');

const { Zone, Hemisphere } = require('./common');

const minimumDifference = 0.0001;

const BoundingBoxLatLon = (props) => {
    const { north, east, south, west, cursor } = props;

    const { mapEast, mapWest, mapSouth, mapNorth } = props;

    const westMax = parseFloat(mapEast) - minimumDifference;
    const eastMin = parseFloat(mapWest) + minimumDifference;
    const northMin = parseFloat(mapSouth) + minimumDifference;
    const southMax = parseFloat(mapNorth) - minimumDifference;

    return (
        <div className="input-location">
            <TextField
                label="West"
                value={west}
                onChange={cursor('west')}
                type="number"
                step="any"
                min={-180}
                max={westMax || 180}
                addon="°"
            />
            <TextField
                label="South"
                value={south}
                onChange={cursor('south')}
                type="number"
                step="any"
                min={-90}
                max={southMax || 90}
                addon="°"
            />
            <TextField
                label="East"
                value={east}
                onChange={cursor('east')}
                type="number"
                step="any"
                min={eastMin || -180}
                max={180}
                addon="°"
            />
            <TextField
                label="North"
                value={north}
                onChange={cursor('north')}
                type="number"
                step="any"
                min={northMin || -90}
                max={90}
                addon="°"
            />
        </div>
    );
};

const BoundingBoxUsngMgrs = (props) => {
    const { usngbb, cursor } = props;
    return (
        <div className="input-location">
            <TextField label="USNG / MGRS" value={usngbb} onChange={cursor('usngbb')} />
        </div>
    );
};

const BoundingBoxUtm = (props) => {
    const {
        utmUpperLeftEasting,
        utmUpperLeftNorthing,
        utmUpperLeftZone,
        utmUpperLeftHemisphere,

        utmLowerRightEasting,
        utmLowerRightNorthing,
        utmLowerRightZone,
        utmLowerRightHemisphere,

        cursor
    } = props;

    return (
        <div>
            <div className="input-location">
                <Group>
                    <Label>Upper-Left</Label>
                    <div>
                        <TextField
                            label="Easting"
                            value={utmUpperLeftEasting}
                            onChange={cursor('utmUpperLeftEasting')}
                            addon="m"
                        />
                        <TextField
                            label="Northing"
                            value={utmUpperLeftNorthing}
                            onChange={cursor('utmUpperLeftNorthing')}
                            addon="m"
                        />
                        <Zone value={utmUpperLeftZone} onChange={cursor('utmUpperLeftZone')} />
                        <Hemisphere
                            value={utmUpperLeftHemisphere}
                            onChange={cursor('utmUpperLeftHemisphere')}
                        />
                    </div>
                </Group>
            </div>
            <div className="input-location">
                <Group>
                    <Label>Lower-Right</Label>
                    <div>
                        <TextField
                            label="Easting"
                            value={utmLowerRightEasting}
                            onChange={cursor('utmLowerRightEasting')}
                            addon="m"
                        />
                        <TextField
                            label="Northing"
                            value={utmLowerRightNorthing}
                            onChange={cursor('utmLowerRightNorthing')}
                            addon="m"
                        />
                        <Zone value={utmLowerRightZone} onChange={cursor('utmLowerRightZone')} />
                        <Hemisphere
                            value={utmLowerRightHemisphere}
                            onChange={cursor('utmLowerRightHemisphere')}
                        />
                    </div>
                </Group>
            </div>
        </div>
    );
};

const BoundingBox = (props) => {
    const { cursor, locationType } = props;

    const inputs = {
        latlon: BoundingBoxLatLon,
        usng: BoundingBoxUsngMgrs,
        utm: BoundingBoxUtm
    };

    const Component = inputs[locationType] || null;

    return (
        <div>
            <Radio value={locationType} onChange={cursor('locationType')}>
                <RadioItem value="latlon">Lat / Lon</RadioItem>
                <RadioItem value="usng">USNG / MGRS</RadioItem>
                <RadioItem value="utm">UTM</RadioItem>
            </Radio>
            {Component !== null ? <Component {...props} /> : null}
        </div>
    );
};

module.exports = BoundingBox;
