#!/usr/bin/env python3
"""Verify the built application binary and stage it separately from diagnostics.

Never uploads anything. Distribution access must be authorized separately.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def digest(stream):
    value = hashlib.sha256()
    for block in iter(lambda: stream.read(1024 * 1024), b''):
        value.update(block)
    return value.hexdigest()


def inspect_payload(apk, model):
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        libraries = sorted(n for n in names if n.startswith('lib/arm64-v8a/') and n.endswith('.so'))
        if 'lib/arm64-v8a/libaicenter.so' not in libraries:
            raise ValueError('ARM64 native libraries missing')
        with archive.open('assets/base.gguf') as stream:
            model_hash = digest(stream)
        if model_hash != model['sha256'] or archive.getinfo('assets/base.gguf').file_size != model['bytes']:
            raise ValueError('Bundled model differs from pinned model')
        return libraries


def main():
    build = json.loads((ROOT / 'Tests/android-build.json').read_text())
    if build['status'] != 'PASS' or not build['apk']['signed']:
        raise ValueError('Successful signed application build required')
    apk = ROOT / build['apk']['path']
    with apk.open('rb') as stream:
        sha = digest(stream)
    if sha != build['apk']['sha256']:
        raise ValueError('APK changed since build verification')
    lock = json.loads((ROOT / 'ci/native-lock.json').read_text())
    model = lock['model']
    libraries = inspect_payload(apk, model)
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
    badging = subprocess.check_output([str(sdk / 'build-tools/35.0.0/aapt'), 'dump', 'badging', str(apk)], text=True)
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if not package or package[1] != 'local.aicenter.app.dev':
        raise ValueError('Unexpected application package')
    if 'android.permission.INTERNET' in badging:
        raise ValueError('Offline application unexpectedly requests INTERNET')
    cert = subprocess.check_output([str(sdk / 'build-tools/35.0.0/apksigner'), 'verify', '--print-certs', str(apk)], text=True)
    source = os.environ.get('GITHUB_SHA') or subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    output = ROOT / 'build/internal-delivery'
    output.mkdir(parents=True, exist_ok=True)
    filename = 'AI-Center-' + source[:12] + '-internal.apk'
    shutil.copyfile(apk, output / filename)
    metadata = dict(application_apk=filename, source_sha=source, package=package[1],
        version_code=package[2], version_name=package[3], bytes=apk.stat().st_size,
        sha256=sha, arm64_libraries=libraries, model=model, signature=cert,
        recommended_free_bytes=3 * apk.stat().st_size + model['bytes'] + 512 * 1024**2,
        storage_basis='download + installed APK + installation staging + extracted model + 512 MiB reserve; vendor overhead may vary',
        known_issues=['Internal debug build; not Phase 1 acceptance',
            'Q013/Q073 repetition unresolved', '53 manual reviews pending',
            'Physical vivo X300 Pro and Lenovo Y900 NOT_RUN',
            'Runner debug signing certificate may change; never uninstall existing data without backup'],
        instrumentation_apk_included=False)
    (output / 'manifest.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n')
    (ROOT / 'Tests/internal-delivery-manifest.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n')
    shutil.copyfile(ROOT / 'Documentation/REAL_DEVICE_TEST.md', output / 'READ-ME.md')
    print('Verified internal application staged; distribution requires an authorized download channel.')


if __name__ == '__main__':
    main()
