const PBKDF2_ITERATIONS = 100_000
const SALT_BYTES = 16
const HASH_BYTES = 32
const STORED_PREFIX = 'pbkdf2-sha256'

function toBase64(bytes: Uint8Array): string {
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }
  return btoa(binary)
}

function fromBase64(value: string): Uint8Array | null {
  try {
    const binary = atob(value)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    return bytes
  } catch {
    return null
  }
}

function timingSafeEqual(left: Uint8Array, right: Uint8Array): boolean {
  if (left.length !== right.length) {
    return false
  }
  let diff = 0
  for (let i = 0; i < left.length; i++) {
    diff |= left[i] ^ right[i]
  }
  return diff === 0
}

async function derivePbkdf2(
  password: string,
  salt: Uint8Array,
  iterations: number
): Promise<Uint8Array> {
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(password),
    'PBKDF2',
    false,
    ['deriveBits']
  )
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt,
      iterations,
      hash: 'SHA-256'
    },
    keyMaterial,
    HASH_BYTES * 8
  )
  return new Uint8Array(derivedBits)
}

function parseStoredPassword(storedPassword: string) {
  const parts = storedPassword.split(':')
  if (parts.length !== 4 || parts[0] !== STORED_PREFIX) {
    return null
  }

  const iterations = Number(parts[1])
  const salt = fromBase64(parts[2])
  const hash = fromBase64(parts[3])
  if (!Number.isInteger(iterations) || iterations <= 0 || !salt || !hash) {
    return null
  }

  return { iterations, salt, hash }
}

export async function hashLockPassword(password: string): Promise<string> {
  const salt = crypto.getRandomValues(new Uint8Array(SALT_BYTES))
  const hash = await derivePbkdf2(password, salt, PBKDF2_ITERATIONS)
  return `${STORED_PREFIX}:${PBKDF2_ITERATIONS}:${toBase64(salt)}:${toBase64(hash)}`
}

export async function verifyLockPassword(
  inputPassword: string,
  storedPassword: string
): Promise<boolean> {
  const parsed = parseStoredPassword(storedPassword)
  if (!parsed) {
    return false
  }

  try {
    const derived = await derivePbkdf2(inputPassword, parsed.salt, parsed.iterations)
    return timingSafeEqual(derived, parsed.hash)
  } catch {
    return false
  }
}
