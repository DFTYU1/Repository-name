"""One exact Android permission contract for explicit API transport and biometrics."""
ALLOWED_PERMISSIONS = frozenset({'android.permission.INTERNET', 'android.permission.USE_BIOMETRIC'})

def require_permission_scope(requested):
    if set(requested) != ALLOWED_PERMISSIONS:
        raise ValueError('APK permission scope must be exactly API internet and biometrics')
