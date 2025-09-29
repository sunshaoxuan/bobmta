import type { ApiError } from '../api/client';
import type { LocalizationState } from '../i18n/useLocalization';

export function formatApiErrorMessage(
  error: ApiError | null,
  translate: LocalizationState['translate']
): string | null {
  if (!error) {
    return null;
  }
  const codeSuffix = extractCodeSuffix(error);
  if (error.message && error.message.trim()) {
    return `${error.message.trim()}${codeSuffix}`;
  }
  if (error.type === 'status') {
    const statusLabel = `${error.status}${codeSuffix}`;
    return translate('backendErrorStatus', { status: statusLabel });
  }
  return translate('backendErrorNetwork');
}

export function extractApiErrorCode(error: ApiError | null): string | number | null {
  if (!error) {
    return null;
  }
  const code = (error as { code?: string | number | null }).code;
  if (typeof code === 'number' && Number.isFinite(code)) {
    return code;
  }
  if (typeof code === 'string' && code.trim()) {
    return code.trim();
  }
  return null;
}

function extractCodeSuffix(error: ApiError): string {
  const code = extractApiErrorCode(error);
  if (code === null) {
    return '';
  }
  return ` (${code})`;
}
