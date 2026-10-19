import hashlib
from pathlib import Path
import tempfile
import unittest
import zipfile
from package_internal_apk import inspect_payload
from permission_scope import require_permission_scope


class PayloadTests(unittest.TestCase):
    def test_permission_contract(self):
        allowed={'android.permission.INTERNET','android.permission.USE_BIOMETRIC'}
        require_permission_scope(allowed)
        for extra in ['android.permission.CAMERA','android.permission.RECORD_AUDIO','android.permission.READ_EXTERNAL_STORAGE']:
            with self.assertRaises(ValueError):require_permission_scope(allowed|{extra})
        with self.assertRaises(ValueError):require_permission_scope({'android.permission.USE_BIOMETRIC'})

    def test_binary_requirements(self):
        with tempfile.TemporaryDirectory() as folder:
            apk = Path(folder) / 'synthetic.apk'
            model = {'sha256': hashlib.sha256(b'model').hexdigest(), 'bytes': 5}
            for abi, payload, valid in [('x86_64', b'model', False),
                                        ('arm64-v8a', b'wrong', False),
                                        ('arm64-v8a', b'model', True)]:
                with zipfile.ZipFile(apk, 'w') as archive:
                    archive.writestr('lib/' + abi + '/libaicenter.so', b'synthetic')
                    archive.writestr('assets/base.gguf', payload)
                if valid:
                    self.assertEqual(inspect_payload(apk, model), ['lib/arm64-v8a/libaicenter.so'])
                else:
                    with self.assertRaises(ValueError):
                        inspect_payload(apk, model)


if __name__ == '__main__':
    unittest.main()
