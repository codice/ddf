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

function getRandomColor() {
  const letters = '789ABCD'
  let color = '#'
  for (let i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 6)]
  }
  return color
}

module.exports = {
  getNewGenerator() {
    const colors = [
      // 10 best taken from here: https://ux.stackexchange.com/questions/94696/color-palette-for-all-types-of-color-blindness
      // http://mkweb.bcgsc.ca/colorblind/
      '#004949', //dark turquoise
      '#009292', //turquoise
      '#ff6db6', //pink
      //  '#ffb677',  //lightpink
      '#490092', //darkpurple
      '#006ddb', //darkblue
      '#b66dff', //purple
      '#6db6ff', //blue
      '#b6dbff', //lightblue
      '#924900', //brown
      // '#dbd100',  //orange
      '#24ff24', //green
      // '#ffff6d'  //yellow
    ]
    const idToColor = {}

    return {
      getColor(id) {
        if (idToColor[id] === undefined) {
          if (colors.length === 0) {
            idToColor[id] = undefined
            //throw "Generator is out of colors to assign.";
          }
          idToColor[id] = colors.pop()
        }
        return idToColor[id]
      },
      removeColor(id) {
        const color = idToColor[id]
        if (color !== undefined) {
          colors.push(color)
          delete idToColor[id]
        }
      },
    }
  },
}
