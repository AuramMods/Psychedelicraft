#!/usr/bin/env python3
"""Launch an installed Forge 1.20.1 client on an isolated Linux display."""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import re
import tempfile
import urllib.request
import uuid

parser = argparse.ArgumentParser()
parser.add_argument("--install", type=Path, required=True)
parser.add_argument("--game", type=Path, required=True)
parser.add_argument("--assets", type=Path, required=True)
parser.add_argument("--player", required=True)
parser.add_argument("--mode", default="owner")
args = parser.parse_args()

install = args.install.resolve()
game = args.game.resolve()
game.mkdir(parents=True, exist_ok=True)
base = json.loads((install / "versions/1.20.1/1.20.1.json").read_text())
profile = json.loads(next(install.glob("versions/*-forge-*/*.json")).read_text())
library_directory = install / "libraries"

def allowed(entry):
    if "rules" not in entry:
        return True
    result = False
    for rule in entry["rules"]:
        if rule.get("features") and any(rule["features"].values()):
            continue
        operating_system = rule.get("os", {})
        if operating_system.get("name", "linux") != "linux":
            continue
        if not re.search(operating_system.get("arch", ".*"), "x86_64"):
            continue
        if not re.search(operating_system.get("version", ".*"), os.uname().release):
            continue
        result = rule["action"] == "allow"
    return result

def fetch(spec, directory=library_directory):
    destination = directory / spec["path"]
    if destination.exists() and hashlib.sha1(destination.read_bytes()).hexdigest() == spec["sha1"]:
        return destination
    destination.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(spec["url"]) as response:
        payload = response.read()
    if hashlib.sha1(payload).hexdigest() != spec["sha1"]:
        raise ValueError("Download checksum differs for " + spec["path"])
    with tempfile.NamedTemporaryFile(dir=destination.parent, delete=False) as output:
        temporary = Path(output.name)
        output.write(payload)
    temporary.replace(destination)
    return destination

asset_root = install / "assets"
index_path = asset_root / "indexes" / (base["assetIndex"]["id"] + ".json")
index_path.parent.mkdir(parents=True, exist_ok=True)
with urllib.request.urlopen(base["assetIndex"]["url"]) as response:
    index_payload = response.read()
if hashlib.sha1(index_payload).hexdigest() != base["assetIndex"]["sha1"]:
    raise ValueError("Minecraft asset index checksum differs")
index_path.write_bytes(index_payload)
objects = json.loads(index_payload)["objects"]
missing_assets = {}
for asset in objects.values():
    digest = asset["hash"]
    path = digest[:2] + "/" + digest
    source = args.assets / "objects" / path
    target = asset_root / "objects" / path
    target.parent.mkdir(parents=True, exist_ok=True)
    if source.is_file():
        try:
            target.symlink_to(source)
        except FileExistsError:
            pass
    else:
        missing_assets[digest] = {"path": path, "sha1": digest,
                "url": "https://resources.download.minecraft.net/" + path}
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
    list(pool.map(lambda spec: fetch(spec, asset_root / "objects"), missing_assets.values()))

overrides = {library["name"].rsplit(":", 1)[0] for library in profile["libraries"]}
specifications = []
for library in base["libraries"]:
    if allowed(library) and library["name"].rsplit(":", 1)[0] not in overrides:
        specifications.append(library["downloads"]["artifact"])
for library in profile["libraries"]:
    if allowed(library):
        specifications.append(library["downloads"]["artifact"])
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
    classpath = list(pool.map(fetch, specifications))
classpath.append(install / "versions/1.20.1/1.20.1.jar")

offline_id = uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:" + args.player).encode()).digest(), version=3)
substitutions = {
    "auth_player_name": args.player,
    "auth_uuid": str(offline_id),
    "auth_access_token": "0",
    "auth_xuid": "",
    "clientid": "",
    "user_type": "legacy",
    "version_name": profile["id"],
    "version_type": "release",
    "game_directory": str(game),
    "assets_root": str(asset_root),
    "assets_index_name": base["assetIndex"]["id"],
    "natives_directory": str(game / "natives"),
    "launcher_name": "Psychedelicraft QA",
    "launcher_version": "1",
    "library_directory": str(library_directory),
    "classpath_separator": os.pathsep,
    "classpath": os.pathsep.join(map(str, classpath)),
}
def expand(argument):
    for key, value in substitutions.items():
        argument = argument.replace("${" + key + "}", value)
    if re.search(r"\$\{[^}]+}", argument):
        raise ValueError("Unexpanded launcher argument " + argument)
    if argument.startswith("-DignoreList="):
        argument += ",1.20.1.jar"
    return argument

def arguments(kind):
    result = []
    for entry in base["arguments"][kind] + profile["arguments"][kind]:
        if isinstance(entry, str):
            result.append(expand(entry))
        elif allowed(entry):
            values = entry["value"] if isinstance(entry["value"], list) else [entry["value"]]
            result.extend(map(expand, values))
    return result

command = ["java", "-Xmx2G", "-Dportqa.mode=" + args.mode]
command += arguments("jvm")
command += [profile["mainClass"]] + arguments("game") + ["--width", "854", "--height", "480"]
os.chdir(game)
os.execvp(command[0], command)
