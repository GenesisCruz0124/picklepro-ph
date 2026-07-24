// 12-char activation codes (spec §6.2); same ambiguity-free alphabet the
// backend uses for PP- short codes (no 0/O/1/I).
const ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

export function generateActivationCode(): string {
  const bytes = new Uint8Array(12);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => ALPHABET[b % ALPHABET.length]).join("");
}
