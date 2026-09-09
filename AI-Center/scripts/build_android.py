#!/usr/bin/env python3
"""Build with the verified official wrapper; preserve failure logs and real APK evidence."""
from datetime import datetime, timezone
from pathlib import Path
import hashlib
import json
import os
import re
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'Tests'


def main():
    OUT.mkdir(exist_ok=True)
    report = {'started_utc': datetime.now(timezone.utc).isoformat(), 'status': 'BLOCKED',
              'blockers': [], 'apk': None, 'instrumentation_apk': None,
              'scope': 'Foundation APK build only; startup, real LLM and device acceptance are separate gates.'}
    lock = json.loads((ROOT / 'ci/toolchain-lock.json').read_text())
    wrapper = ROOT / 'gradle/wrapper/gradle-wrapper.jar'
    if not wrapper.is_file() or hashlib.sha256(wrapper.read_bytes()).hexdigest() != lock['gradle_wrapper_sha256']:
        report['blockers'].append('Official Gradle wrapper is missing or fails SHA-256 verification')
    sdk_value = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    sdk = Path(sdk_value) if sdk_value else None
    aapt = 'aapt2.exe' if os.name == 'nt' else 'aapt2'
    signer = 'apksigner.bat' if os.name == 'nt' else 'apksigner'
    for relative in ('platforms/android-36/android.jar', 'build-tools/35.0.0/' + aapt, 'build-tools/35.0.0/' + signer):
        if not sdk or not (sdk / relative).is_file():
            report['blockers'].append('Android SDK component unavailable: ' + relative)
    report['sdk_root'] = str(sdk) if sdk else None
    command = ['cmd', '/c', str(ROOT / 'gradlew.bat')] if os.name == 'nt' else ['sh', str(ROOT / 'gradlew')]
    with (OUT / 'android-build.log').open('w') as log:
        if not report['blockers']:
            try:
                version = subprocess.run(command + ['--version'], cwd=ROOT, capture_output=True, text=True, timeout=120)
                log.write(version.stdout + version.stderr)
                if version.returncode or not re.search(r'^Gradle 8\.13\s*$', version.stdout, re.M):
                    report['blockers'].append('Wrapper could not start pinned Gradle 8.13; see build log')
                else:
                    report['gradle_version'] = '8.13'
                    log.flush()
                    result = subprocess.run(command + ['--no-daemon', '--console=plain', '--stacktrace',
                        ':app:assembleDebug', ':app:assembleDebugAndroidTest', ':app:lintDebug'],
                        cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, timeout=1800)
                    report['exit_code'] = result.returncode
                    report['status'] = 'PASS' if result.returncode == 0 else 'FAIL'
                    # UP-TO-DATE outputs are valid only after build success and signature checks.
                    for key, relative in [('apk', 'app/build/outputs/apk/debug/app-debug.apk'),
                        ('instrumentation_apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')]:
                        apk = ROOT / relative
                        if not apk.is_file():
                            report['status'] = 'FAIL'
                            report['blockers'].append('Missing build output: ' + relative)
                            continue
                        with zipfile.ZipFile(apk) as archive:
                            if archive.testzip() or 'AndroidManifest.xml' not in archive.namelist() or 'classes.dex' not in archive.namelist():
                                raise ValueError('Invalid APK structure: ' + relative)
                        verify = [str(sdk / 'build-tools/35.0.0' / signer), 'verify', '--verbose', '--print-certs', str(apk)]
                        if os.name == 'nt':
                            verify = ['cmd', '/c'] + verify
                        signed = subprocess.run(verify, stdout=log, stderr=subprocess.STDOUT, timeout=60)
                        if signed.returncode:
                            report['status'] = 'FAIL'
                            report['blockers'].append('APK signature verification failed: ' + relative)
                        elif result.returncode == 0:
                            report[key] = {'path': relative, 'bytes': apk.stat().st_size,
                                           'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(), 'signed': True}
            except subprocess.TimeoutExpired:
                report['status'] = 'FAIL'
                report['blockers'].append('Build command timed out; inspect preserved log')
            except (OSError, ValueError, zipfile.BadZipFile) as error:
                report['status'] = 'FAIL'
                report['blockers'].append(type(error).__name__ + ': ' + str(error))
        for blocker in report['blockers']:
            log.write('BLOCKER: ' + blocker + '\n')
    report['finished_utc'] = datetime.now(timezone.utc).isoformat()
    (OUT / 'android-build.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report['status'] == 'PASS' else 2


if __name__ == '__main__':
    sys.exit(main())
