#!/usr/bin/env python3
"""Checks that the built mod carries its linked-pet follow guard and Mixin config."""
from pathlib import Path
import sys
import zipfile

jar = Path(sys.argv[1]) if len(sys.argv) > 1 else None
if jar is None or not jar.is_file():
    raise SystemExit("Usage: python3 tools/validate_built_jar.py path/to/PetBed.jar")

with zipfile.ZipFile(jar) as archive:
    names = set(archive.namelist())
    required = {
        "petbed.mixins.json",
        "petbed.refmap.json",
        "com/petrick/petbed/mixin/FollowOwnerGoalMixin.class",
        "com/petrick/petbed/block/PetLinkService.class",
    }
    missing = sorted(required - names)
    if missing:
        raise SystemExit("Built JAR is missing linked-pet behavior files: " + ", ".join(missing))
    manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8", errors="replace")
    if "MixinConfigs: petbed.mixins.json" not in manifest:
        raise SystemExit("Built JAR manifest does not register petbed.mixins.json")

print(f"Built JAR follow-guard checks passed: {jar.name}")
