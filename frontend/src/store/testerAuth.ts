/**
 * Auth state do mod tester — separado do admin.
 * Persiste em localStorage com chaves diferentes (liberthia.tester.*).
 */

const TOKEN_KEY = 'liberthia.tester.token'
const NAME_KEY = 'liberthia.tester.mcName'

export type TesterDto = {
  id: number
  mcName: string
  points: number
  createdAt?: string
  lastLoginAt?: string
}

export function getTesterToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getTesterMcName(): string | null {
  return localStorage.getItem(NAME_KEY)
}

export function setTesterAuth(token: string, mcName: string) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(NAME_KEY, mcName)
}

export function clearTesterAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(NAME_KEY)
}

export function isTesterLoggedIn(): boolean {
  return !!getTesterToken()
}
