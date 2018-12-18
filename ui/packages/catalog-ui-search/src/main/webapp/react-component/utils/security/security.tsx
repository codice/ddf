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
  None = 0,
  Read = 1,
  Write = 2,
  Share = 3,
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
        accessGroups: obj.accessGroups || obj[this.GroupsWrite] || [],
        accessGroupsRead: obj.accessGroupsRead || obj[this.GroupsRead] || [],
        accessIndividuals:
          obj.accessIndividuals || obj[this.IndividualsWrite] || [],
        accessIndividualsRead:
          obj.accessIndividualsRead || obj[this.IndividualsRead] || [],
        accessAdministrators:
          obj.accessAdministrators || obj[this.AccessAdministrators] || [],
      } as Restrictions

    return {
      owner: obj.get('metacard.owner') || obj.get('owner'),
      accessGroups: obj.get(this.GroupsWrite) || obj.get('accessGroups') || [],
      accessGroupsRead:
        obj.get(this.GroupsRead) || obj.get('accessGroupsRead') || [],
      accessIndividuals:
        obj.get(this.IndividualsWrite) || obj.get('accessIndividuals') || [],
      accessIndividualsRead:
        obj.get(this.IndividualsRead) || obj.get('accessIndividualsRead') || [],
      accessAdministrators:
        obj.get(this.AccessAdministrators) ||
        obj.get('accessAdministrators') ||
        [],
    } as Restrictions
  }

  private static canAccess(user: any, res: Restrictions, accessLevel: Access) {
    return (
      res.owner === undefined ||
      res.owner === user.getEmail() ||
      this.getAccess(user, res) > accessLevel
    )
  }

  static canRead(user: any, res: Restrictions): boolean {
    return this.canAccess(user, res, Access.Read)
  }

  static canWrite(user: any, res: Restrictions) {
    return this.canAccess(user, res, Access.Write)
  }

  static canShare(user: any, res: Restrictions) {
    return this.canAccess(user, res, Access.Share)
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

  static getAccess(user: any, res: Restrictions) {
    return this.highestAccess(
      user
        .getRoles()
        .map((role: string) => this.getRoleAccess(res, role))
        .append(this.getIndividualAccess(res, user.getEmail()))
    )
  }

  private static highestAccess(accesses: Access[]): Access {
    let max = Access.None
    accesses.forEach(a => {
      if (a > max) max = a
    })
    return max
  }
}
