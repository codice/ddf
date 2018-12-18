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

export enum Access {
  None = 'none',
  Read = 'read',
  Write = 'write',
  Share = 'share',
}

export type Restrictions = {
  owner: string
  accessGroups: string[]
  accessGroupsRead: string[]
  accessIndividuals: string[]
  accessIndividualsRead: string[]
  accessAdministrators: string[]
}

export class Security {
  static readonly GroupsRead = 'security.access-groups-read'
  static readonly GroupsWrite = 'security.access-groups'
  static readonly IndividualsRead = 'security.access-individuals-read'
  static readonly IndividualsWrite = 'security.access-individuals'
  static readonly AccessAdministrators = 'security.access-administrators'

  // remove this ugly function when everything is typescript
  static extractRestrictions(obj: any): Restrictions {
    if (typeof obj.get !== 'function')
      return {
        owner: obj.owner,
        accessGroups: obj.accessGroups || [],
        accessGroupsRead: obj.accessGroupsRead || [],
        accessIndividuals: obj.accessIndividuals || [],
        accessIndividualsRead: obj.accessIndividualsRead || [],
        accessAdministrators: obj.accessAdministrators || [],
      } as Restrictions

    return {
      owner: obj.get('metacard.owner') || obj.get('owner'),
      accessGroups:
        obj.get('security.access-groups') || obj.get('accessGroups') || [],
      accessGroupsRead:
        obj.get('security.access-groups-read') ||
        obj.get('accessGroupsRead') ||
        [],
      accessIndividuals:
        obj.get('security.access-individuals') ||
        obj.get('accessIndividuals') ||
        [],
      accessIndividualsRead:
        obj.get('security.access-individuals-read') ||
        obj.get('accessIndividualsRead') ||
        [],
      accessAdministrators:
        obj.get('security.access-administrators') ||
        obj.get('accessAdministrators') ||
        [],
    } as Restrictions
  }

  static canRead(user: any, res: Restrictions): boolean {
    return (
      res.owner === undefined ||
      res.owner === user.getEmail() ||
      res.accessIndividuals.indexOf(user.getEmail()) > -1 ||
      res.accessIndividualsRead.indexOf(user.getEmail()) > -1 ||
      res.accessGroups.some(group => user.getRoles().indexOf(group) > -1) ||
      res.accessGroupsRead.some(group => user.getRoles().indexOf(group) > -1) ||
      res.accessAdministrators.indexOf(user.getEmail()) > -1
    )
  }

  static canWrite(user: any, res: Restrictions) {
    return (
      res.owner === undefined ||
      res.owner === user.getEmail() ||
      res.accessIndividuals.indexOf(user.getEmail()) > -1 ||
      res.accessGroups.some(group => user.getRoles().indexOf(group) > -1) ||
      res.accessAdministrators.indexOf(user.getEmail()) > -1
    )
  }

  static canShare(user: any, res: Restrictions) {
    return (
      res.owner === undefined ||
      res.owner === user.getEmail() ||
      res.accessAdministrators.indexOf(user.getEmail()) > -1
    )
  }

  static getRoleAccess(res: Restrictions, role: string) {
    return res.accessGroups.indexOf(role) > -1
      ? Access.Write
      : res.accessGroupsRead.indexOf(role) > -1
        ? Access.Read
        : Access.None
  }

  static getIndividualAccess(res: Restrictions, username: string) {
    return res.accessAdministrators.indexOf(username) > -1
      ? Access.Share
      : res.accessIndividuals.indexOf(username) > -1
        ? Access.Write
        : res.accessIndividualsRead.indexOf(username) > -1
          ? Access.Read
          : Access.None
  }
}
