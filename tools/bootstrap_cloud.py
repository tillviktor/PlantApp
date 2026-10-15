"""Prepare verified toolchains and proxy trust under /workspace, without root."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import urllib.parse
import xml.etree.ElementTree as ET
import zipfile

BASE = Path('/workspace/toolchains')
BASE.mkdir(parents=True, exist_ok=True)
def download(url, target):
    subprocess.run(['curl', '--fail', '--silent', '--show-error', '--location', '--retry', '2', url, '-o', str(target)], check=True)
def verify(path, expected, algorithm='sha256'):
    if hashlib.new(algorithm, path.read_bytes()).hexdigest() != expected.strip():
        raise RuntimeError(f'Checksum mismatch for {path.name}')

if not (BASE/'jdk-21/bin/javac').exists():
    release = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz'
    download(release, BASE/'jdk.tar.gz')
    download(release+'.sha256.txt', BASE/'jdk.sha256')
    verify(BASE/'jdk.tar.gz', (BASE/'jdk.sha256').read_text().split()[0])
    target = BASE/'jdk-21'
    target.mkdir(exist_ok=True)
    with tarfile.open(BASE/'jdk.tar.gz') as archive:
        for member in archive.getmembers():
            member.name = '/'.join(member.name.split('/')[1:])
            if member.name:
                archive.extract(member, target, filter='data')

if not (BASE/'android-sdk/cmdline-tools/latest/bin/sdkmanager').exists():
    archive_name = 'commandlinetools-linux-11076708_latest.zip'
    download('https://dl.google.com/android/repository/'+archive_name, BASE/'android.zip')
    download('https://dl.google.com/android/repository/repository2-1.xml', BASE/'repository.xml')
    checksum = None
    for package in ET.parse(BASE/'repository.xml').getroot():
        for archive in package.findall('./archives/archive'):
            if archive.findtext('./complete/url') == archive_name:
                checksum = archive.find('./complete/checksum')
    if checksum is None:
        raise RuntimeError('Pinned Android tools checksum missing from official manifest')
    verify(BASE/'android.zip', checksum.text, checksum.attrib.get('type', 'sha1'))
    target = BASE/'android-sdk/cmdline-tools'
    target.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(BASE/'android.zip') as archive:
        archive.extractall(target)
    (target/'cmdline-tools').rename(target/'latest')
for tool in (BASE/'android-sdk/cmdline-tools/latest/bin').iterdir():
    tool.chmod(tool.stat().st_mode | 0o111)

# Trust the platform's CA without disabling HTTPS verification.
store = BASE/'cacerts'
shutil.copyfile('/etc/ssl/certs/java/cacerts', store)
cert = os.environ.get('CODEX_PROXY_CERT')
if cert:
    if not cert.startswith('/'):
        target = BASE/'proxy-ca.pem'
        target.write_text(cert)
        cert = str(target)
    subprocess.run([str(BASE/'jdk-21/bin/keytool'), '-importcert', '-noprompt', '-alias', 'cloud-proxy', '-file', cert,
                    '-keystore', str(store), '-storepass', 'changeit'], check=True, capture_output=True)
props = [f'systemProp.javax.net.ssl.trustStore={store}', 'systemProp.javax.net.ssl.trustStorePassword=changeit']
proxy = urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY', ''))
args = []
if proxy.hostname:
    if proxy.username:
        raise RuntimeError('Authenticated proxy requires platform-supported Java configuration')
    for protocol in ['http', 'https']:
        props += [f'systemProp.{protocol}.proxyHost={proxy.hostname}', f'systemProp.{protocol}.proxyPort={proxy.port or 80}']
    args = ['--proxy=http', f'--proxy_host={proxy.hostname}', f'--proxy_port={proxy.port or 80}']
gradle_home = BASE/'gradle-home'
gradle_home.mkdir(exist_ok=True)
(gradle_home/'gradle.properties').write_text('\n'.join(props)+'\n')
(BASE/'sdk-proxy-args').write_text('\n'.join(args)+'\n' if args else '')
user = BASE/'android-user'
(user/'cache').mkdir(parents=True, exist_ok=True)
env = dict(os.environ, JAVA_HOME=str(BASE/'jdk-21'), ANDROID_HOME=str(BASE/'android-sdk'),
           ANDROID_USER_HOME=str(user), JAVA_TOOL_OPTIONS=f'-Djavax.net.ssl.trustStore={store} -Djavax.net.ssl.trustStorePassword=changeit')
manager = str(BASE/'android-sdk/cmdline-tools/latest/bin/sdkmanager')
subprocess.run([manager, *args, '--licenses'], input='y\n'*100, text=True, env=env, check=True)
subprocess.run([manager, *args, 'platforms;android-35', 'build-tools;35.0.0', 'platform-tools'], env=env, check=True)
print('Cloud Android toolchain prepared.')
