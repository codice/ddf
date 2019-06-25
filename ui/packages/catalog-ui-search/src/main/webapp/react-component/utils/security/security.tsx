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
import { Item } from '../../container/sharing'

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
const _ = require('underscore')

export type Entry = {
  value: string
  access: Access
}

export enum Access {
  None = 0,
  Read = 1,
  Write = 2,
  Share = 3,
}

export class Restrictions {
  owner: string
  accessGroups: string[]
  accessGroupsRead: string[]
  accessIndividuals: string[]
  accessIndividualsRead: string[]
  accessAdministrators: string[]

  static readonly GroupsRead = 'security.access-groups-read'
  static readonly GroupsWrite = 'security.access-groups'
  static readonly IndividualsRead = 'security.access-individuals-read'
  static readonly IndividualsWrite = 'security.access-individuals'
  static readonly AccessAdministrators = 'security.access-administrators'

  // remove this ugly function when everything is typescript
  static from(obj: any): Restrictions {
    if (typeof obj.get !== 'function')
      return {
        owner: obj.owner || obj['metacard.owner'],
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
      owner: obj.get('owner') || obj.get('metacard.owner'),
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
}

export class Security {
  private readonly res: Restrictions

  constructor(res: Restrictions) {
    this.res = res
  }

  private canAccess(user: any, accessLevel: Access) {
    return (
      this.res.owner === undefined ||
      this.res.owner === user.getEmail() ||
      this.getAccess(user) >= accessLevel
    )
  }

  canRead(user: any): boolean {
    return this.canAccess(user, Access.Read)
  }

  canWrite(user: any) {
    return this.canAccess(user, Access.Write)
  }

  canShare(user: any) {
    return this.canAccess(user, Access.Share)
  }

  isShared(): boolean {
    return !(
      this.res.accessGroups.length == 0 &&
      this.res.accessGroupsRead.length == 0 &&
      this.res.accessIndividuals.length == 0 &&
      this.res.accessIndividualsRead.length == 0 &&
      (this.res.accessAdministrators.length == 0 ||
        (this.res.accessAdministrators.length == 1 &&
          this.res.accessAdministrators[0] === this.res.owner))
    )
  }

  private getGroupAccess(group: string) {
    if (this.res.accessGroups.indexOf(group) > -1) {
      return Access.Write
    }

    if (this.res.accessGroupsRead.indexOf(group) > -1) {
      return Access.Read
    }

    return Access.None
  }

  private getIndividualAccess(email: string) {
    if (this.res.accessAdministrators.indexOf(email) > -1) {
      return Access.Share
    }

    if (this.res.accessIndividuals.indexOf(email) > -1) {
      return Access.Write
    }

    if (this.res.accessIndividualsRead.indexOf(email) > -1) {
      return Access.Read
    }

    return Access.None
  }

  private getAccess(user: any): Access {
    return Math.max(
      this.getIndividualAccess(user.getEmail()),
      ...user.getRoles().map((group: string) => this.getGroupAccess(group))
    )
  }

  getGroups(forceIncludeGroups: string[]): Entry[] {
    return _.union(
      forceIncludeGroups,
      this.res.accessGroups,
      this.res.accessGroupsRead
    )
      .map((group: string) => {
        return {
          value: group,
          access: this.getGroupAccess(group),
        } as Entry
      })
      .sort(Security.compareFn)
  }

  getIndividuals(): Entry[] {
    return _.union(
      this.res.accessIndividuals,
      this.res.accessIndividualsRead,
      this.res.accessAdministrators
    )
      .map((username: string) => {
        return {
          value: username,
          access: this.getIndividualAccess(username),
        } as Entry
      })
      .sort(Security.compareFn)
  }

  private static compareFn = (a: Item, b: Item): number => {
    return a.value < b.value ? -1 : a.value > b.value ? 1 : 0
  }
}
