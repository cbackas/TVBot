import { env } from "cloudflare:workers";
import { verifyKey } from "discord-interactions";

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

// verifyKey() accepts either the raw hex string or a pre-imported CryptoKey.
// Passed a string, it re-imports the key via crypto.subtle on every call —
// wasted work since our public key never changes. Import it once and hand
// verifyKey the CryptoKey instead.
let publicKeyPromise: Promise<CryptoKey> | undefined;
function getPublicKey(): Promise<CryptoKey> {
  publicKeyPromise ??= crypto.subtle.importKey(
    "raw",
    hexToBytes(PUBLIC_KEY_HEX),
    { name: "ed25519", namedCurve: "ed25519" },
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

  const key = await getPublicKey();
  const isValid = await verifyKey(body, signature, timestamp, key);
  if (isValid === false) {
    return { valid: false, error: "Invalid request signature", code: 401 };
  }

  return { valid: true };
}
