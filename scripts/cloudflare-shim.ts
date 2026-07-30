// Shim for cloudflare:workers so command modules can be imported in Node.js.
// Only the static `definition` properties are accessed — no runtime calls are made.
import { register } from "node:module";

register("data:text/javascript," + encodeURIComponent(`
export function resolve(specifier, context, next) {
  if (specifier === "cloudflare:workers") {
    return { url: "data:text/javascript," + encodeURIComponent("export const env = {}; export function waitUntil() {}"), shortCircuit: true };
  }
  return next(specifier, context);
}
`), import.meta.url);
