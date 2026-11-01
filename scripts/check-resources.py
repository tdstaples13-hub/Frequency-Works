#!/usr/bin/env python3
"""Local JSON and basic resource sanity checks, no NeoForge dependencies required."""
import json
from pathlib import Path
root = Path(__file__).resolve().parent.parent
resources = root/'src/main/resources'
json_files = sorted(resources.rglob('*.json'))
for path in json_files:
    try:
        json.loads(path.read_text(encoding='utf-8'))
    except Exception as exc:
        raise SystemExit(f'Invalid JSON in {path.relative_to(root)}: {exc}')
assert json_files, 'No resource files found'
print(f'PASS: {len(json_files)} resource JSON files valid')
