#!/usr/bin/env python3
"""Check repository invariants, not runtime behavior or all possible secrets."""
from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = {'.git', 'target', '.tools', '.m2', '.idea', '.vscode', '__pycache__'}
REQUIRED = [
    'README.md', 'CONTRIBUTING.md', '.env.example', 'Dockerfile', 'render.yaml',
    '.github/workflows/ci.yml', '.github/pull_request_template.md',
    '.specify/memory/constitution.md', 'specs/001-backend-sdd/spec.md',
    'specs/001-backend-sdd/plan.md', 'specs/001-backend-sdd/tasks.md',
    'docs/VERIFICACION.md', 'docs/TRAZABILIDAD.md', 'docs/DESPLIEGUE.md',
]


def routes() -> list[list[str]]:
    result: list[list[str]] = []
    for source in (ROOT / 'src/main/java').rglob('*Controller.java'):
        text = source.read_text(encoding='utf-8')
        base = re.search(r'@RequestMapping\(\s*"([^"]*)"\s*\)', text)
        prefix = base.group(1) if base else ''
        for method, arguments in re.findall(
                r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^\n]*)\))?', text):
            path = re.match(r'\s*"([^"]*)"', arguments)
            result.append([method.upper(), prefix + (path.group(1) if path else '')])
    return sorted(result)


def files_for_hygiene() -> list[Path]:
    # Inspect tracked paths in CI; in a clean delivery inspect packaged source files.
    if (ROOT / '.git').exists():
        output = subprocess.run(['git', 'ls-files', '-z'], cwd=ROOT,
                                capture_output=True, check=True).stdout
        return [ROOT / name.decode('utf-8') for name in output.split(b'\0') if name]
    return [p for p in ROOT.rglob('*') if p.is_file()
            and not EXCLUDED.intersection(p.relative_to(ROOT).parts)]


def main() -> int:
    errors: list[str] = []
    for name in REQUIRED:
        if not (ROOT / name).is_file():
            errors.append('Missing artifact: ' + name)

    baseline_path = ROOT / 'specs/001-backend-sdd/contracts/api-baseline.json'
    baseline = json.loads(baseline_path.read_text(encoding='utf-8'))
    actual = routes()
    if actual != baseline['routes']:
        errors.append('HTTP method/path inventory changed; review the compatibility contract.')
    v1 = ROOT / 'src/main/resources/db/migration/V1__schema.sql'
    if hashlib.sha256(v1.read_bytes()).hexdigest() != baseline['v1_sha256']:
        errors.append('Applied migration V1 was modified. Create a new migration instead.')

    checked = files_for_hygiene()
    for path in checked:
        relative = path.relative_to(ROOT).as_posix()
        if not path.is_file():
            continue
        if (path.name == '.env' or path.name == 'local.properties'
                or path.suffix in {'.pem', '.key', '.p12', '.jks', '.db', '.class', '.jar'}
                or relative.startswith(('data/', 'uploads/', 'target/', '.idea/', '.tools/'))):
            errors.append('Private/runtime artifact must not be versioned: ' + relative)
            continue
        if path.suffix in {'.java', '.md', '.properties', '.yml', '.yaml', '.http'}:
            text = path.read_text(encoding='utf-8')
            if '/*missing*/' in text:
                errors.append('Unresolved source placeholder: ' + relative)
            # Limited safety net. Does not replace manual review or a secret scanner.
            if (re.search(r'sb_secret_[A-Za-z0-9_-]{24,}', text)
                    or '-----BEGIN PRIVATE KEY-----' in text
                    or '-----BEGIN RSA PRIVATE KEY-----' in text):
                errors.append('Possible private credential: ' + relative)

    links = 0
    for document in (p for p in checked if p.suffix == '.md' and p.is_file()):
        prose = re.sub(r'```.*?```', '', document.read_text(encoding='utf-8'), flags=re.S)
        for target in re.findall(r'\[[^\]]+\]\(([^\s)]+)\)', prose):
            if target.startswith(('http:', 'https:', 'mailto:', '#')):
                continue
            destination = (document.parent / unquote(target.split('#', 1)[0])).resolve()
            links += 1
            if not destination.exists():
                errors.append(f'Broken relative link in {document.relative_to(ROOT)}: {target}')

    if errors:
        print('\n'.join('FAIL: ' + item for item in errors), file=sys.stderr)
        return 1
    print(f'PASS: {len(REQUIRED)} required artifacts, {len(actual)} routes, immutable V1, '
          f'{links} relative links, hygiene for {len(checked)} files.')
    print('This check is not a Spring compilation, database test, or complete secret scan.')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print('FAIL: repository check could not complete: ' + str(error), file=sys.stderr)
        sys.exit(1)
