/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

class Restrictions {
  owner: string
  accessGroups: string[]
  accessGroupsRead: string[]
  accessIndividuals: string[]
  accessIndividualsRead: string[]
  accessAdministrators: string[]
}

export class Security {
  static extractRestrictions(metacard: any): Restrictions {
    return {
      owner:
        metacard.owner ||
        metacard.get('metacard.owner') ||
        metacard.get('owner'),
      accessGroups:
        metacard.accessGroups ||
        metacard.get('security.access-groups') ||
        metacard.get('accessGroups') ||
        [],
      accessGroupsRead:
        metacard.accessGroupsRead ||
        metacard.get('security.access-groups-read') ||
        metacard.get('accessGroupsRead') ||
        [],
      accessIndividuals:
        metacard.accessIndividuals ||
        metacard.get('security.access-individuals') ||
        metacard.get('accessIndividuals') ||
        [],
      accessIndividualsRead:
        metacard.accessIndividualsRead ||
        metacard.get('security.access-individuals-read') ||
        metacard.get('accessIndividualsRead') ||
        [],
      accessAdministrators:
        metacard.accessAdministrators ||
        metacard.get('security.access-administrators') ||
        metacard.get('accessAdministrators') ||
        [],
    } as Restrictions
  }

  static canRead(user: any, restrictions: Restrictions): boolean {
    return (
      restrictions.owner === undefined ||
      restrictions.owner === user.getEmail() ||
      restrictions.accessIndividuals.indexOf(user.getEmail()) > -1 ||
      restrictions.accessIndividualsRead.indexOf(user.getEmail()) > -1 ||
      restrictions.accessGroups.some(
        group => user.getRoles().indexOf(group) > -1
      ) ||
      restrictions.accessGroupsRead.some(
        group => user.getRoles().indexOf(group) > -1
      ) ||
      restrictions.accessAdministrators.indexOf(user.getEmail()) > -1
    )
  }

  static canWrite(user: any, restrictions: Restrictions) {
    return (
      restrictions.owner === undefined ||
      restrictions.owner === user.getEmail() ||
      restrictions.accessIndividuals.indexOf(user.getEmail()) > -1 ||
      restrictions.accessGroups.some(
        group => user.getRoles().indexOf(group) > -1
      ) ||
      restrictions.accessAdministrators.indexOf(user.getEmail()) > -1
    )
  }

  static canShare(user: any, restrictions: Restrictions) {
    return (
      restrictions.owner === undefined ||
      restrictions.owner === user.getEmail() ||
      restrictions.accessAdministrators.indexOf(user.getEmail()) > -1
    )
  }
}
