import { env } from "cloudflare:workers";

/**
 * Verifies Discord's Ed25519 interaction signature directly against Workers'
 * native crypto.subtle (Cloudflare added native Ed25519 support specifically
 * for this use case), rather than going through `discord-interactions`'
 * verifyKey helper.
 *
 * The meaningful win over that helper isn't the crypto itself — it already
 * delegates to the same crypto.subtle under the hood — it's that it re-runs
 * `importKey` on our public key from scratch on every single request. The
 * key never changes, so we import it once per isolate and reuse the
 * resulting CryptoKey for every verify() call after that.
 */

const PUBLIC_KEY_HEX = env.DISCORD_PUBLIC_KEY;

type VerificationResult =
  | {
      valid: true;
    }
  | {
      valid: false;
      error: string;
      code: number;
    };

function hexToBytes(hex: string): Uint8Array {
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = Number.parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

let publicKeyPromise: Promise<CryptoKey> | undefined;
function getPublicKey(): Promise<CryptoKey> {
  publicKeyPromise ??= crypto.subtle.importKey(
    "raw",
    hexToBytes(PUBLIC_KEY_HEX),
    "Ed25519",
    false,
    ["verify"],
  );
  return publicKeyPromise;
}

export async function verifyInteraction(
  body: ArrayBuffer,
  headers: Request["headers"],
): Promise<VerificationResult> {
  const signature = headers.get("x-signature-ed25519");
  const timestamp = headers.get("x-signature-timestamp");

  if (signature == null || timestamp == null) {
    return { valid: false, error: "Missing required headers", code: 400 };
  }

  const message = new Uint8Array(timestamp.length + body.byteLength);
  message.set(new TextEncoder().encode(timestamp));
  message.set(new Uint8Array(body), timestamp.length);

  const key = await getPublicKey();
  const isValid = await crypto.subtle
    .verify("Ed25519", key, hexToBytes(signature), message)
    .catch(() => false);

  if (isValid === false) {
    return { valid: false, error: "Invalid request signature", code: 401 };
  }

  return { valid: true };
}
