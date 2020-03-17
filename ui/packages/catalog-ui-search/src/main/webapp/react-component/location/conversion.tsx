import * as React from 'react'
import { hot } from 'react-hot-loader'
import Dropdown from '../presentation/dropdown'
import { buttonTypeEnum, Button } from '../presentation/button'
const usngs = require('usng.js')
const dmsUtils = require('../../component/location-new/utils/dms-utils.js')
const converter = new usngs.Converter()
const usngPrecision = 6

type ConvertedValueType = {
  lat: string
  lon: string
}[]

type USNGValueType = string[]

type DMSValueType = {
  lat: {
    coordinate: string
    direction: 'S' | 'N'
  }
  lon: {
    coordinate: string
    direction: 'E' | 'W'
  }
}[]

type Props = {
  value: any
  isValid: boolean
}

// can't key off isValid yet because the flag isn't cleared until the user presses X on the validation popup
export const Conversion = ({ value, isValid }: Props) => {
  const [deserializedValue, setDeserializedValue] = React.useState(undefined as
    | undefined
    | ConvertedValueType)
  const [usngValue, setUsngValue] = React.useState(undefined as
    | undefined
    | USNGValueType)
  const [dmsValue, setDmsValue] = React.useState(undefined as
    | undefined
    | DMSValueType)
  React.useEffect(
    () => {
      if (value) {
        try {
          setDeserializedValue(
            value
              .split('],')
              .map((pair: string) => pair.split(','))
              .map((pair: string[]) => {
                return {
                  lon: pair[0].replace(/\[/g, '').replace(/\]/g, ''),
                  lat: pair[1].replace(/\[/g, '').replace(/\]/g, ''),
                }
              })
          )
        } catch (err) {
          console.warn(err)
          setDeserializedValue(undefined)
        }
      } else {
        setDeserializedValue(undefined)
      }
    },
    [value, isValid]
  )

  React.useEffect(
    () => {
      if (deserializedValue !== undefined) {
        setUsngValue(
          deserializedValue.map(pair => {
            return converter.LLtoUSNG(pair.lat, pair.lon, usngPrecision)
          })
        )
      } else {
        setUsngValue(undefined)
      }
    },
    [deserializedValue]
  )

  React.useEffect(
    () => {
      if (deserializedValue !== undefined) {
        setDmsValue(
          deserializedValue.map(pair => {
            return {
              lat: dmsUtils.ddToDmsCoordinateLat(pair.lat),
              lon: dmsUtils.ddToDmsCoordinateLon(pair.lon),
            }
          })
        )
      } else {
        setDmsValue(undefined)
      }
    },
    [deserializedValue]
  )
  return (
    <div>
      <Dropdown
        content={() => {
          return (
            <div style={{ padding: '10px' }}>
              <div>USNG / MGRS translation:</div>
              {usngValue === undefined ? (
                <div style={{ whiteSpace: 'nowrap', padding: '10px' }}>
                  Current value is not valid, so it can't be translated to USNG
                </div>
              ) : (
                usngValue.map((coord, i) => {
                  return (
                    <div style={{ whiteSpace: 'nowrap', padding: '10px' }}>
                      Coordinate {i + 1} : {coord}
                    </div>
                  )
                })
              )}
            </div>
          )
        }}
      >
        <Button
          buttonType={buttonTypeEnum.neutral}
          fadeUntilHover
          text="USNG / MGRS"
          style={{ padding: '0px 10px' }}
        />
      </Dropdown>
      <Dropdown
        content={() => {
          return (
            <div style={{ padding: '10px' }}>
              <div>DMS translation:</div>
              {dmsValue === undefined ? (
                <div style={{ whiteSpace: 'nowrap', padding: '10px' }}>
                  Current value is not valid, so it can't be translated to DMS
                </div>
              ) : (
                dmsValue.map((coord, i) => {
                  return (
                    <div style={{ whiteSpace: 'nowrap', padding: '10px' }}>
                      Coordinate {i + 1} :
                      <div>
                        Lat: {coord.lat.coordinate} {coord.lat.direction}
                      </div>
                      <div>
                        Lon: {coord.lon.coordinate} {coord.lon.direction}
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          )
        }}
      >
        <Button
          buttonType={buttonTypeEnum.neutral}
          fadeUntilHover
          text="DMS"
          style={{ padding: '0px 10px' }}
        />
      </Dropdown>
    </div>
  )
}

export default hot(module)(Conversion)
