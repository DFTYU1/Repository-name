#!/usr/bin/env python3
"""Official SDK provisioning and isolated Android startup checks on GitHub standard runners."""
import argparse
from datetime import datetime, timezone
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'Tests'
PACKAGE = 'local.aicenter.app.dev'
TEST_PACKAGE = PACKAGE + '.test'
LOCK = json.loads((ROOT / 'ci/toolchain-lock.json').read_text())


def run(command, timeout=60, check=True, input_text=None):
    result = subprocess.run([str(x) for x in command], input=input_text, text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
    if check and result.returncode:
        raise RuntimeError(Path(str(command[0])).name + ' exited ' + str(result.returncode) + '\n' + result.stdout[-5000:])
    return result.stdout


def sdk_root():
    value = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not value:
        raise RuntimeError('The official runner Android SDK is not configured')
    return Path(value)


def cli_tool(name):
    found = shutil.which(name)
    if found:
        return Path(found)
    candidate = sdk_root() / 'cmdline-tools/latest/bin' / name
    if candidate.is_file():
        return candidate
    raise RuntimeError('Official Android command-line tool unavailable: ' + name)


def verify_wrapper():
    sha = hashlib.sha256((ROOT / 'gradle/wrapper/gradle-wrapper.jar').read_bytes()).hexdigest()
    if sha != LOCK['gradle_wrapper_sha256']:
        raise RuntimeError('Gradle wrapper SHA-256 verification failed')
    props = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text()
    if 'distributionSha256Sum=' + LOCK['gradle_distribution_sha256'] not in props:
        raise RuntimeError('Gradle distribution checksum pin is missing')


def prepare():
    verify_wrapper()
    sdk = sdk_root()
    manager = cli_tool('sdkmanager')
    with (OUT / 'sdk-install.log').open('w') as log:
        log.write(run([manager, '--sdk_root=' + str(sdk), '--licenses'], timeout=120, input_text='y\n' * 150))
        log.flush()
        log.write(run([manager, '--sdk_root=' + str(sdk), '--install',
                       'platform-tools', 'platforms;android-36', 'build-tools;35.0.0',
                       'emulator', LOCK['emulator_image']], timeout=900, input_text='y\n' * 150))
    observed = {'time_utc': datetime.now(timezone.utc).isoformat(),
                'java': run(['java', '-version']), 'sdkmanager': run([manager, '--version']),
                'emulator': run([sdk / 'emulator/emulator', '-version'], check=False),
                'gradle_wrapper_sha256': LOCK['gradle_wrapper_sha256'],
                'source_properties': {}}
    for folder in ['platforms/android-36', 'build-tools/35.0.0', 'emulator',
                   'system-images/android-35/google_apis/x86_64']:
        properties = sdk / folder / 'source.properties'
        observed['source_properties'][folder] = properties.read_text() if properties.is_file() else 'NOT_FOUND'
    (OUT / 'toolchain-observed.json').write_text(json.dumps(observed, indent=2) + '\n')
    print('Official Android SDK packages installed; exact observed revisions recorded.')


def parse_instrumentation(output, profile):
    match = re.search(r'^INSTRUMENTATION_RESULT: aicenter_results=(.+)$', output, re.M)
    failed = re.search(r'^INSTRUMENTATION_RESULT: aicenter_failures=(\d+)$', output, re.M)
    if not match or not failed:
        raise RuntimeError('Instrumentation did not return a complete test report')
    tests = json.loads(match.group(1))
    expected = {'application_initialization', 'administrator_setup_via_real_ui', 'responsive_home_layout',
                'android_sqlite_and_keystore_message_persistence', 'sandbox_copy_and_traversal_denial',
                'interrupted_task_journal_recovery', 'disconnect_button_revokes_token_lease_and_close_hook'}
    if profile == 'phone':
        expected.add('lease_expires_after_real_five_minutes')
    names = {item['test'] for item in tests}
    passed = (int(failed.group(1)) == 0 and expected <= names and
              all(item['status'] == 'PASS' for item in tests))
    return {'status': 'PASS' if passed else 'FAIL', 'tests': tests,
            'missing_tests': sorted(expected - names)}


def test():
    sdk = sdk_root()
    adb_command = [sdk / 'platform-tools/adb', '-s', 'emulator-5554']
    def adb(*args, **kwargs):
        return run(adb_command + list(args), **kwargs)
    report = {'started_utc': datetime.now(timezone.utc).isoformat(), 'status': 'FAIL',
              'profiles': {}, 'errors': [], 'scope': 'Fresh API-35 emulator; not physical vivo/Y900 acceptance',
              'remaining_gates': {
                  'offline_llm_and_model_benchmarks': 'NOT_INTEGRATED',
                  'ai_long_term_memory_manager': 'NOT_IMPLEMENTED',
                  'learning_progress': 'NOT_IMPLEMENTED',
                  'real_external_SAF_grant_expiry': 'NOT_RUN',
                  'actual_network_and_voice_cancellation': 'NOT_IMPLEMENTED',
                  'physical_vivo_X300_Pro_and_Y900': 'NOT_RUN',
                  'visual_screenshot_review': 'NOT_RUN; app keeps FLAG_SECURE'
              }}
    process = None
    emulator_log = None
    try:
        build = json.loads((OUT / 'android-build.json').read_text())
        if build['status'] != 'PASS' or not build.get('apk') or not build.get('instrumentation_apk'):
            raise RuntimeError('A successful signed APK build is required before startup tests')
        for key in ('apk', 'instrumentation_apk'):
            item = build[key]
            if hashlib.sha256((ROOT / item['path']).read_bytes()).hexdigest() != item['sha256']:
                raise RuntimeError('APK changed after build verification')
        permissions = run([sdk / 'build-tools/35.0.0/aapt', 'dump', 'permissions', ROOT / build['apk']['path']])
        (OUT / 'apk-permissions.log').write_text(permissions)
        requested = set(re.findall(r"uses-permission: name='([^']+)'", permissions))
        if requested != {'android.permission.USE_BIOMETRIC'}:
            raise RuntimeError('APK permissions differ from this foundation milestone')
        if not Path('/dev/kvm').exists():
            raise RuntimeError('Standard runner has no KVM acceleration; no device tests were run')
        if not os.access('/dev/kvm', os.R_OK | os.W_OK):
            # Ephemeral Linux runner access only; never ROOT or change Android security.
            run(['sudo', 'setfacl', '-m', 'u:' + getpass.getuser() + ':rw', '/dev/kvm'])
        avd_name = 'aicenter_ci_' + str(os.getpid())
        manager = cli_tool('avdmanager')
        run([manager, 'create', 'avd', '--name', avd_name, '--package', LOCK['emulator_image'],
             '--device', 'pixel_6'], timeout=120, input_text='no\n')
        # Refuse to use any pre-existing device on the selected port.
        devices = run([sdk / 'platform-tools/adb', 'devices'])
        if 'emulator-5554' in devices:
            raise RuntimeError('Emulator port already in use; existing device will not be changed')
        emulator_log = (OUT / 'emulator.log').open('w')
        process = subprocess.Popen([str(sdk / 'emulator/emulator'), '-avd', avd_name,
            '-port', '5554', '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot',
            '-gpu', 'swiftshader_indirect', '-accel', 'on', '-memory', '3072', '-cores', '2'],
            stdout=emulator_log, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 300
        while True:
            if process.poll() is not None:
                raise RuntimeError('Emulator exited during startup; inspect emulator.log')
            if adb('shell', 'getprop', 'sys.boot_completed', timeout=15, check=False).strip() == '1':
                break
            if time.monotonic() > deadline:
                raise RuntimeError('Emulator boot timed out')
            time.sleep(2)
        if adb('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
            raise RuntimeError('Refusing to run CI data operations on a physical device')
        adb('shell', 'input', 'keyevent', '82')
        adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
        adb('shell', 'settings', 'put', 'system', 'user_rotation', '0')
        adb('shell', 'settings', 'put', 'system', 'screen_off_timeout', '1800000')
        report['device'] = {'model': adb('shell', 'getprop', 'ro.product.model').strip(),
                            'api': adb('shell', 'getprop', 'ro.build.version.sdk').strip(),
                            'abi': adb('shell', 'getprop', 'ro.product.cpu.abi').strip()}
        for profile, size, density in [('phone', '1080x2400', '420'), ('tablet', '2560x1600', '240')]:
            try:
                if profile == 'tablet':
                    # Only app/test data created in the preceding fresh CI run is removed.
                    adb('uninstall', TEST_PACKAGE)
                    adb('uninstall', PACKAGE)
                if adb('shell', 'pm', 'path', PACKAGE, check=False).strip():
                    raise RuntimeError('App already installed before CI fixture setup; existing data left intact')
                adb('shell', 'wm', 'size', size)
                adb('shell', 'wm', 'density', density)
                time.sleep(2)
                adb('install', '--no-streaming', ROOT / build['apk']['path'], timeout=90)
                adb('install', '--no-streaming', ROOT / build['instrumentation_apk']['path'], timeout=90)
                adb('logcat', '-c')
                started = adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/local.aicenter.app.MainActivity')
                (OUT / (profile + '-launch.log')).write_text(started)
                if not re.search(r'^Status: ok$', started, re.M):
                    raise RuntimeError('Activity launch did not report success')
                output = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'profile', profile,
                    '-e', 'long_lease', 'true' if profile == 'phone' else 'false',
                    TEST_PACKAGE + '/local.aicenter.app.FoundationInstrumentation', timeout=540)
                (OUT / (profile + '-instrumentation.log')).write_text(output)
                profile_result = parse_instrumentation(output, profile)
                crash = adb('logcat', '-d', '-b', 'crash', check=False)
                (OUT / (profile + '-crash.log')).write_text(crash)
                if 'Process: ' + PACKAGE in crash:
                    profile_result['status'] = 'FAIL'
                    profile_result['crash'] = True
                report['profiles'][profile] = profile_result
                print(profile + ': ' + profile_result['status'], flush=True)
            except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired) as error:
                report['profiles'][profile] = {'status': 'FAIL', 'error': str(error)}
                print(profile + ': FAIL', flush=True)
        if len(report['profiles']) == 2 and all(p['status'] == 'PASS' for p in report['profiles'].values()):
            report['status'] = 'PASS'
    except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired) as error:
        report['errors'].append(str(error))
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=15)
        if emulator_log:
            emulator_log.close()
        report['finished_utc'] = datetime.now(timezone.utc).isoformat()
        (OUT / 'android-runtime.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'status': report['status'], 'errors': report['errors'], 'report': 'Tests/android-runtime.json'}))
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['prepare', 'test'])
    args = parser.parse_args()
    OUT.mkdir(exist_ok=True)
    if os.environ.get('GITHUB_ACTIONS') != 'true':
        sys.exit('This runner is restricted to a fresh GitHub CI environment; no local device was modified.')
    try:
        if args.action == 'prepare':
            prepare()
            sys.exit(0)
        sys.exit(test())
    except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired) as error:
        (OUT / 'ci-setup-error.log').write_text(str(error) + '\n')
        sys.exit('CI setup failed; diagnostic log preserved.')
