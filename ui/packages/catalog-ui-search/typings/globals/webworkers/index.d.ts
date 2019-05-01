declare namespace NodeJS {
  export interface Global {
    window: any
  }
}
declare function postMessage(message: any, transfer?: any[]): void //https://www.antonmata.me/2017/04/04/web-workers-vscode-intellisense.html
