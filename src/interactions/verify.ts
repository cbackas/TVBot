import { env } from "cloudflare:workers";
import { verifyKey } from "discord-interactions";

const PUBLIC_KEY = env.DISCORD_PUBLIC_KEY;

type VerificationResult =
  | {
      valid: true;
    }
  | {
      valid: false;
      error: string;
      code: number;
    };

export async function verifyInteraction(
  body: ArrayBuffer,
  headers: Request["headers"],
): Promise<VerificationResult> {
  const signature = headers.get("x-signature-ed25519");
  const timestamp = headers.get("x-signature-timestamp");

  if (signature == null || timestamp == null) {
    return { valid: false, error: "Missing required headers", code: 400 };
  }

  const isValid: boolean = await verifyKey(
    body,
    signature,
    timestamp,
    PUBLIC_KEY,
  );
  if (isValid === false) {
    return { valid: false, error: "Invalid request signature", code: 401 };
  }

  return { valid: true };
}
