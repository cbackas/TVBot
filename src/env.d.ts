declare namespace Cloudflare {
  interface Env extends Partial<Record<import("./lib/env").EnvKey, string>> {}
}
