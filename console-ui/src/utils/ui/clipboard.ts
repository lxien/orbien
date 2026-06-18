let hiddenInput: HTMLTextAreaElement | null = null

function getHiddenInput(): HTMLTextAreaElement {
  if (!hiddenInput || !document.body.contains(hiddenInput)) {
    hiddenInput = document.createElement('textarea')
    hiddenInput.style.cssText =
      'position:fixed;top:-999px;left:-999px;opacity:0;pointer-events:none;z-index:-1;'
    hiddenInput.setAttribute('readonly', '')
    hiddenInput.setAttribute('tabindex', '-1')
    document.body.appendChild(hiddenInput)
  }
  return hiddenInput
}

function execCopy(text: string): boolean {
  const input = getHiddenInput()
  input.value = text
  input.select()
  input.setSelectionRange(0, 999999)

  let success = false
  try {
    success = document.execCommand('copy')
  } catch (e) {
    console.warn('execCommand copy failed:', e)
  }

  if (
    document.activeElement &&
    typeof (document.activeElement as HTMLElement).blur === 'function'
  ) {
    ;(document.activeElement as HTMLElement).blur()
  }

  return success
}

function isSecureContext(): boolean {
  if (window.isSecureContext !== undefined) {
    return window.isSecureContext
  }
  return location.protocol === 'https:' || /^(127\.0\.0\.1|localhost)$/.test(location.hostname)
}

export const ClipboardUtils = {
  async copy(text: string): Promise<boolean> {
    if (!text) {
      return false
    }

    const stringText = String(text)

    if (
      isSecureContext() &&
      navigator.clipboard &&
      typeof navigator.clipboard.writeText === 'function'
    ) {
      try {
        await navigator.clipboard.writeText(stringText)
        return true
      } catch {
        return execCopy(stringText)
      }
    }

    const execSuccess = execCopy(stringText)

    if (execSuccess && navigator.clipboard && typeof navigator.clipboard.readText === 'function') {
      try {
        const clipboardText = await navigator.clipboard.readText()
        if (clipboardText === stringText) {
          return true
        }
      } catch {
        return execSuccess
      }
    }

    return execSuccess
  },

  escapeHtml(text: string): string {
    const div = document.createElement('div')
    div.textContent = text
    return div.innerHTML
  }
}
