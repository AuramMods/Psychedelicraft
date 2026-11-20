#!/usr/bin/env python3
"""Describe actual runtime observations and pinned source mappings; never gates a launch."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[3]
ACCEPTED = "51286f8f3e6866e8b952175c9c60c7f6e63bef8d"
ORIGINAL = "319a4ea0160d6600cffcd5f5cc9e363ceaab44a1"
BASE = "src/main/java/ivorius/psychedelicraft/"
MOD = "psychedelicraft:"
BARRELS = {wood + "_barrel" for wood in ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak")}


def source(ref, path):
    return subprocess.check_output(["git", "show", ref + ":" + BASE + path], cwd=ROOT, text=True)


def uncomment(text):
    # Preserve quoted URLs/escapes while removing real Java comments.
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/'
    return re.sub(pattern, lambda m: m[0] if m[0][0] in "\"'" else "\n" * m[0].count("\n"), text, flags=re.S)


def declarations(ref, path):
    return {m[2] for m in re.findall(r'\b([A-Z][A-Z_0-9]+)\s*=\s*(\w+)\(\s*"([^"]+)"', uncomment(source(ref, path)))}


def short(identity):
    return identity.removeprefix(MOD)


def implementation(name, owner):
    file = name.split("$", 1)[0].replace(".", "/") + ".java"
    if (ROOT / "src/main/java" / file).exists():
        return f"[{name.rsplit('.', 1)[-1]}](../src/main/java/{file})"
    return f"Native `{name.rsplit('.', 1)[-1]}` via [{Path(owner).stem}](../{BASE}{owner})"


def behavior(identity):
    """Curated behavior evidence, distinct from the generic C35 asset observation."""
    name = short(identity)
    if name in BARRELS or name in {"barrel", "flask", "distillery"}:
        return "C7 processing/automation; C6 contents"
    if name.startswith(("juniper_", "stripped_juniper_")) or name == "fruiting_juniper_leaves":
        return "C11 wood/fruit/vehicle integration"
    if name.startswith("potted_"):
        return "C10/C11 native pots; C26 all five drug pots"
    if name in {"drying_table", "iron_drying_table"}:
        return "C4 drying/sided automation"
    if name in {"mash_tub", "mash_tub_edge"}:
        return "C8 batches/walls/automation"
    if name in {"bottle_rack", "placed_drink", "tray", "bunsen_burner"}:
        return "C9 display/props/rack; C32 exact display drops"
    if name in {"lattice", "wine_grape_lattice", "morning_glory_lattice", "morning_glory", "morning_glory_seeds", "wine_grapes"}:
        return "C10 vine/flower integration; C33 exact harvests"
    if name in {"rift_jar", "glitch", "reality_rift"}:
        return "C14 rifts/jars/glitch; C36 combined native rendering/reload/resize"
    if name in {"vomit", "paper_bag", "bag_o_vomit"}:
        return "C18 stomach/bag/disguise; C33 native item visibility"
    if name in {"joint", "peyote_joint", "cigarette", "cigar", "smoking_pipe", "bong", "harmonium"}:
        return "C5 smoking/stacking; C30 optional Harmonium trades"
    if name == "molotov_cocktail":
        return "C13 native projectile and fluid-preserving conversion"
    if name in {"wooden_mug", "stone_cup", "glass_chalice", "shot_glass", "bottle", "syringe", "filled_bowl", "filled_bucket", "filled_glass_bottle"}:
        return "C6 transfer/servings; C12 world buckets"
    if name in {"hash_muffin", "lsd_pill", "obsidian_bottle", "obsidian_dust"}:
        return "C2 consumption; C28 acquisition/container recipes"
    if name in {"brown_magic_mushrooms", "red_magic_mushrooms", "dried_peyote", "jolly_rancher", "cocaine_powder", "lsd_square"}:
        return "C1/C2 scalar/use benefits; C4 drying; C10 LSD paper"
    if name.startswith("dried_"):
        return "C3 material source; C4 drying ingredient/output"
    if name in {"cannabis", "hop", "tobacco", "coca", "coffea", "peyote", "agave_plant", "jimsonweed", "belladonna", "tomatoes"}:
        return "C3 placement and growth, C39 native wild generation"
    if name in {"agave_leaf", "belladonna_berries", "belladonna_leaf", "belladonna_seeds", "cannabis_buds", "cannabis_leaf", "cannabis_seeds", "coca_leaves", "coca_seeds", "coffea_cherries", "coffee_beans", "hop_cones", "hop_seeds", "jimsonweed_leaf", "jimsonweed_seed_pod", "jimsonweed_seeds", "tobacco_seeds", "tomato", "tomato_leaf", "tomato_seeds"}:
        return "C3 crops/materials/roasting; C4/C6 processing inputs"
    return "Behavior evidence needs assignment"


OWNERS = {
    "ITEMS": "item/PSItems.java", "BLOCKS": "block/PSBlocks.java", "FLUIDS": "fluid/PSFluids.java",
    "FLUID_TYPES": "fluid/PSFluids.java", "ENTITY_TYPES": "entity/PSEntities.java",
    "BLOCK_ENTITY_TYPES": "block/entity/PSBlockEntities.java", "PARTICLE_TYPES": "particle/PSParticles.java",
    "SOUND_EVENTS": "PSSounds.java", "MENU_TYPES": "screen/PSMenus.java", "RECIPE_TYPES": "recipe/PSRecipes.java",
    "RECIPE_SERIALIZERS": "recipe/PSRecipes.java", "VILLAGER_PROFESSIONS": "entity/PSTradeOffers.java",
    "POI_TYPES": "entity/PSTradeOffers.java", "FEATURES": "world/gen/PSWorldGen.java",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--english", type=Path, required=True)
    parser.add_argument("--polish", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    en, pl = (json.loads(path.read_text()) for path in (args.english, args.polish))
    jar_sha = hashlib.sha256(args.jar.read_bytes()).hexdigest()
    out = ["# Current Forge 1.20.1 registration and asset matrix", "",
           "This is the C35 runtime inventory, replacing the historical **Initial registered-content inventory** as the current registration view. "
           "It describes the production jar loaded on installed Forge, rather than counting declarations as runtime proof. "
           "All mod IDs below have the `psychedelicraft:` namespace unless another namespace is shown.", "",
           f"Minecraft **{en['minecraft']}**, Forge **{en['forge']}**; production SHA-256 **`{jar_sha}`**. "
           "The production source is C34 (`388fd078`); C35 adds the separate QA sidecar and this evidence. "
           "Final runtime snapshots and four contact sheets are retained in ignored `run/production/cycle35/game-final/registration-audit`.", "",
           "The [reproducible audit tool](../tools/qa/registration-audit/README.md) exports actual registry values, loaded recipes, "
           "advancements, commands and drug types, then calls native model baking/selection and GUI item rendering. "
           "It repeats the client observations after a real Polish language/resource reload. "
           "C35 proves registration/assets/default names; behavior evidence is separately assigned below. "
           "It does not prove every NBT subtype, all block-state pixels, natural acquisition, or multiplayer lifecycle paths.", "",
           "## Sources and status", "",
           f"[Accepted Forge source](https://github.com/AuramMods/Psychedelicraft/tree/{ACCEPTED}/{BASE}) supplies custom behavior and fixes; "
           f"[original 1.20.1 source](https://github.com/AuramMods/Psychedelicraft/tree/{ORIGINAL}/{BASE}) supplies the complete modern baseline. "
           "Chosen implementation links identify the current Java class or its native registration owner. "
           "C-number evidence refers to [the cycle report](PORT_1201.md). "
           "[Native mappings](PORT_1201_NATIVE.md), [all 140 settings](PORT_1201_SETTINGS.md), and [audiovisual evidence](PORT_1201_RENDERING.md) are part of this matrix.", "",
           "This inventory retains its C35 snapshot provenance. C36 verifies combined rendering, C37 verifies the two-player lifecycle, and C38–C39 verify native and wild acquisition. The main report links these later checks. "
           "Oculus and modified BSL compatibility testing is deferred.", "",
           "| Runtime registry | Mod entries |", "| --- | --- |"]
    out += [f"| `{kind}` | {len(rows)} |" for kind, rows in en["registries"].items()]
    out += [f"| Loaded mod recipes | {len(en['recipes'])} |", f"| Advancements | {len(en['advancements'])} |", f"| Mod drug profiles (not Forge potions) | {len(en['effects'])} |", f"| Operator command roots | {len(en['commands'])} |", f"| Forge settings | {len(en['settings'])}; enumerated in linked settings matrix |", f"| Advancement criteria | {len(en['criteria'])} |", ""]
    missing = []
    for kind, rows in en["registries"].items():
        out += ["## " + kind, "", "| ID | Chosen implementation | Evidence / current status |", "| --- | --- | --- |"]
        for identity, row in rows.items():
            name = short(identity)
            evidence = behavior(identity)
            if kind == "ITEMS":
                a, b = en["itemModels"][identity], pl["itemModels"][identity]
                flags = ["missing model"] if a["missing"] or b["missing"] else []
                if "minecraft:missingno" in a["sprites"] or "minecraft:missingno" in b["sprites"]:
                    flags.append("missing sprite")
                if a["name"] == a["description"] or b["name"] == b["description"]:
                    flags.append("untranslated default name")
                missing.extend((identity, flag) for flag in flags)
                evidence += "; C35 EN/PL native default render/name: " + (", ".join(flags) if flags else "present")
            elif kind == "BLOCKS":
                if row["class"].endswith("PhysicalFluidBlock"):
                    evidence = "C12 world fluid/metadata; C34 conduit classification"
                states = en["blockModels"][identity]
                bad = [locale + ":" + s for locale, data in (("en_us", en), ("pl_pl", pl)) for s, model in data["blockModels"][identity].items() if model["missing"] or "minecraft:missingno" in model["sprites"]]
                missing.extend((s, "missing model/sprite") for s in bad)
                evidence += f"; C35 {row['states']} states, {len(states)} MODEL states baked" if states else f"; native `{row['shape']}` render path"
            elif kind in {"FLUIDS", "FLUID_TYPES"}:
                evidence = "C6 servings/transfers; C12 finite world flow/metadata; C34 native wet integration"
                if kind == "FLUIDS":
                    evidence += f"; source `{short(row['source'])}`, water tag `{str(row['waterTag']).lower()}`"
            elif kind == "ENTITY_TYPES":
                evidence += "; C35 renderer `" + en["entityRenderers"][identity].rsplit(".", 1)[-1] + "`"
            elif kind == "BLOCK_ENTITY_TYPES":
                evidence = {"fluid_attributes": "C12 source/flow metadata", "juniper_sign": "C11 native sign", "juniper_hanging_sign": "C11 native hanging sign"}.get(name, behavior(identity))
            else:
                evidence = {"SOUND_EVENTS": "C9/C14 interactions; C22/C24 native sound lifecycle; C35 resource inventory", "PARTICLE_TYPES": "C5 smoke; C13 bubbles; C24 native audio/particle paths", "MENU_TYPES": "C4/C7 native menus, transfer and automation", "RECIPE_TYPES": "C4 drying / C8 mashing native processing", "RECIPE_SERIALIZERS": "C4/C6/C7/C8/C13/C28 actual recipe paths; C25 JEI", "VILLAGER_PROFESSIONS": "C15 trades/professions; C16 work; C17/C30 native acquisition", "POI_TYPES": "C15 drying-table job acquisition", "FEATURES": "C3 soil-aware natural patches; C10/C11 vine/tree generation"}.get(kind, evidence)
            if "needs assignment" in evidence:
                missing.append((kind + ":" + identity, "behavior evidence needs assignment"))
            out.append(f"| `{name}` | {implementation(row['class'], OWNERS[kind])} | {evidence} |")
        out.append("")
    out += ["## Every loaded mod recipe", "", "Runtime lookup confirms these recipe objects were actually loaded. "
            "The resource path is the chosen data implementation; the processing and conservation behavior of its serializer is covered in the linked cycle report. "
            "A loaded entry is not an assertion that every ingredient combination was crafted individually.", "",
            "| ID / source data | Runtime type | Runtime serializer |", "| --- | --- | --- |"]
    for identity, row in en["recipes"].items():
        name = short(identity)
        out.append(f"| [`{name}`](../src/main/resources/data/psychedelicraft/recipes/{name}.json) | `{row['type']}` | `{row['serializer']}` |")
    out += ["", "## Every advancement", "", "All 24 use the restored native criteria and client award UI (C27). "
            "C32 separately verifies the actual native trident/Channeling awards.", "", "| ID / data source | Evidence |", "| --- | --- |"]
    out += [f"| [`{short(identity)}`](../src/main/resources/data/psychedelicraft/advancements/{short(identity)}.json) | C27 criteria, native awards, localization and restart |" for identity in en["advancements"]]
    out += ["", "## Every drug profile", "", "These are the mod's scalar profiles, not registered vanilla potion effects. "
            "The accepted `coccaine` spelling is retained for saved/command identity. C2 covers the scalar/use hooks; C19–C24 and C29 cover the restored audiovisual/text channels. "
            "C36 verifies combined rendering. C37 verifies a seven-profile custom-benefit cohort across its installed multiplayer lifecycle.", "", "| ID | Chosen implementation | Status |", "| --- | --- | --- |"]
    lifecycle = {"brown_shrooms", "red_shrooms", "peyote", "caffeine", "alcohol", "coccaine", "lsd"}
    for identity, row in en["effects"].items():
        name = short(identity)
        evidence = "C2 scalar profile and shared progression"
        if name in lifecycle:
            evidence += ", C37 installed benefit and lifecycle checks"
        elif name == "tobacco":
            evidence += ", C5 smoking and C37 native saturation"
        out.append(f"| `{name}` | {implementation(row['class'], 'entity/drug/DrugType.java')} | {evidence} |")
    out += ["", "## Every setting and criterion", "", "The live config-spec export contains "
            f"**{sum(k.startswith('common:') for k in en['settings'])} common** and **{sum(k.startswith('client:') for k in en['settings'])} client** leaves. "
            "Each is listed with its accepted source, default, consumer and evidence in [the settings matrix](PORT_1201_SETTINGS.md). "
            "Its native UI/EN/PL/scope/persistence checks are in C31; those source files are unchanged through C35. "
            "The separate native gamerule `doSleepDeprivation` is registered in `Psychedelicraft` and covered in C2/C22/C27.", "",
            "| Native criterion ID | Chosen implementation / evidence |", "| --- | --- |"]
    for identity in sorted(en["criteria"]):
        out.append(f"| `{short(identity)}` | [PSCriteria / DrugEffectsChangedCriterion](../{BASE}advancement/PSCriteria.java); C27 native award/clear/restart |")
    out += ["", "## Every operator command", "", "Actual Brigadier usage, obtained from the running server with its operator source. "
            "C18/C23/C29 cover execution, permissions, synchronization and parsing; C35 records the current tree.", "", "| Native root / usage | Source |", "| --- | --- |"]
    for name, row in en["commands"].items():
        owner = {"drug": "DrugCommand", "vomit": "VomitCommand", "hallucinate": "HallucinateCommand"}[name]
        for usage in row.get("usage", []):
            out.append(f"| `/{name}{' ' + usage if usage else ''}` | [command/{owner}](../{BASE}command/{owner}.java) |")
    out += ["", "## Additional dynamic registrations", "", "These IDs are loaded from the server's registry access. "
            "Native registry duplicates of the tables above are omitted here. "
            "World-generation sampling and configuration are covered in C3/C10/C11, native pool graphs in C30, and custom damage/lifecycle behavior in C2/C13/C18/C24.", "",
            "| Registry | ID | Chosen implementation / evidence |", "| --- | --- | --- |"]
    additional = {"minecraft:creative_mode_tab": "item/PSItems.java; C25/C31 native creative/search rebuilding", "minecraft:damage_type": "PSDamageTypes.java + damage_type data; C2/C13/C18/C24", "minecraft:worldgen/configured_feature": "world/gen/PSWorldGen.java + configured_feature data; C3/C10/C11", "minecraft:worldgen/placed_feature": "worldgen/placed_feature data; C3/C10/C11", "forge:biome_modifier": "forge/biome_modifier data; C3/C10/C11", "forge:global_loot_modifier_serializers": "world/gen/ChestLootModifier.java; C17 acquisition loot"}
    for kind, evidence in additional.items():
        for identity in en["datapackRegistries"].get(kind, []):
            out.append(f"| `{kind}` | `{short(identity)}` | {evidence} |")
    out += ["", "## Accepted and original registry reconciliation", "",
            "This source comparison strips Java comments before extracting literal factory declarations, "
            "then explicitly expands the accepted six `barrel(PSBlocks.*)` items and the original Terraform boat helper's two items. "
            "It is a comparison of these pinned sources, not a generic Java evaluator. "
            "The runtime tables above remain the authority for what actually loaded.", ""]
    aliases = {"iron_chain": "chain", "glowing_ink": "glow_ink_sac", "sign_wax": "honeycomb", "sign_black_dye": "black_dye", "kelp_stalk": "kelp_plant"}
    for kind, key, path in (("items", "ITEMS", "item/PSItems.java"), ("blocks", "BLOCKS", "block/PSBlocks.java")):
        actual = {short(identity) for identity in en["registries"][key]}
        old = declarations(ACCEPTED, path)
        modern = declarations(ORIGINAL, path)
        if kind == "items":
            old |= BARRELS
            modern |= {"juniper_boat", "juniper_chest_boat"}
        out += [f"### {kind.title()}", "", f"Accepted named entries: **{len(old)}**. Original named entries including the explicit helper expansion: **{len(modern)}**. "
                f"Original entries absent from the runtime mod registry: **{', '.join(sorted(modern - actual)) or 'none'}**.", "",
                "| Accepted ID absent from mod namespace | Native target / state mapping | Evidence / scope |", "| --- | --- | --- |"]
        for name in sorted(old - actual):
            if name.startswith("natural_"):
                target = MOD + name.removeprefix("natural_") + "[natural=true]"
                evidence = "C3 native crop state replaces duplicate wild block"
            elif name == "juniper_double_slab":
                target, evidence = MOD + "juniper_slab[type=double]", "C11 native slab state"
            elif name == "powered_juniper_trapdoor":
                target, evidence = MOD + "juniper_trapdoor[powered=true]", "C11 native redstone state"
            else:
                target = "minecraft:" + aliases.get(name, name)
                evidence = "Native implementation; interaction evidence and remaining scope in native mapping report"
            out.append(f"| `{name}` | `{target}` | {evidence} |")
        out += ["", "Target additions relative to the original named table: " + (", ".join(f"`{name}`" for name in sorted(actual - modern)) or "none") + ".", ""]
    out += ["The original `kava_seeds` and `kava_root` lines are commented TODOs, not registered items. "
            "The 27 additional target block IDs correspond to the original dynamically registered physical-fluid families, "
            "not additional gameplay. The seven native potted crop/sapling forms are included in the original block table and all are present. "
            "[The native mapping report](PORT_1201_NATIVE.md) records the deliberate namespace migration for new worlds; no legacy-world importer is claimed.", ""]
    old_fluids = declarations(ACCEPTED, "fluid/PSFluids.java")
    modern_fluids = set(re.findall(r'Psychedelicraft.id\("([^"]+)"\)', uncomment(source(ORIGINAL, "fluid/PSFluids.java"))))
    current_fluids = {short(row["source"]) for row in en["registries"]["FLUIDS"].values()}
    out += ["### Fluid families", "", f"Accepted source families: **{len(old_fluids)}**; original: **{len(modern_fluids)}**; runtime: **{len(current_fluids)}**. "
            f"Accepted families absent at runtime: **{', '.join(sorted(old_fluids-current_fluids)) or 'none'}**. "
            f"Original families absent at runtime: **{', '.join(sorted(modern_fluids-current_fluids)) or 'none'}**. "
            "Each runtime source has its explicitly listed flowing registration, native liquid block and Forge fluid type. "
            "The original physical-fluid registration is dynamic; C12 verifies the actual source/flow/block/metadata paths.", ""]
    accepted_entities = set(re.findall(r'\.id\(Psychedelicraft.id\("([^"]+)"\)', source(ACCEPTED, "entity/PSEntities.java"))) | {"juniper_boat", "juniper_chest_boat"}
    out += ["### Entity types", "", "All accepted entity registrations are accounted for. "
            "The four mod-specific types remain mod types; the six vanilla replacements use native entities. "
            "C35 binds the four current mod renderers. The native report retains the model/acquisition/lifecycle scopes not yet observed.", "",
            "| Accepted ID | Target entity ID | Evidence / scope |", "| --- | --- | --- |"]
    for name in sorted(accepted_entities):
        target = MOD + name if MOD + name in en["registries"]["ENTITY_TYPES"] else "minecraft:" + ("trident" if name == "thrown_trident" else name)
        out.append(f"| `{name}` | `{target}` | {behavior(MOD + name) if target.startswith(MOD) else 'Native implementation; see native mapping report for bounded evidence'} |")
    old_tiles = re.findall(r'GameRegistry.registerTileEntity\([^\n]+Psychedelicraft.id\("([^"]+)"\)', source(ACCEPTED, "block/PSBlocks.java"))
    retired_tiles = {"brushable_block": "minecraft:brushable_block", "bee_nest": "minecraft:beehive", "campfire": "minecraft:campfire", "conduit": "minecraft:conduit", "lattice_crop": "lattice block states replace the legacy tile", "waterlogged_block": "native WATERLOGGED state/interfaces", "peyote": "native block AGE state"}
    out += ["", "### Block entity types and native wood helpers", "", "| Accepted block entity ID | Target / state mapping |", "| --- | --- |"]
    for name in old_tiles:
        target = MOD + name if MOD + name in en["registries"]["BLOCK_ENTITY_TYPES"] else retired_tiles.get(name, "Mapping needs review")
        out.append(f"| `{name}` | `{target}` |")
    out += ["", "The target adds `juniper_hanging_sign` as the native separate hanging-sign block-entity type. "
            "`PSWoodTypes` registers `psychedelicraft:juniper` once in each native WoodType and BlockSetType helper registry; C11 verifies their native wood/sign behavior.", ""]
    with zipfile.ZipFile(args.jar) as jar:
        for language in ("en_us", "pl_pl"):
            catalog = json.loads(jar.read(f"assets/psychedelicraft/lang/{language}.json"))
            for identity, row in en["registries"]["ITEMS"].items():
                description = row["description"]
                if description.startswith(("item.psychedelicraft.", "block.psychedelicraft.")) and description not in catalog:
                    missing.append((identity, f"{language} direct item-description entry missing"))
        sounds = json.loads(jar.read("assets/psychedelicraft/sounds.json"))
        missing_audio = []
        for event, data in sounds.items():
            for entry in data.get("sounds", []):
                name = entry if isinstance(entry, str) else entry["name"]
                kind = "sound" if isinstance(entry, str) else entry.get("type", "sound")
                if kind == "sound" and name.startswith(MOD):
                    path = "assets/psychedelicraft/sounds/" + short(name) + ".ogg"
                    if path not in jar.namelist() or not jar.read(path).startswith(b"OggS"):
                        missing_audio.append((event, path))
    out += ["## Observation limits and remaining work", "",
            f"C35 inspects **{sum(len(v) for v in en['blockModels'].values())}** MODEL block states, "
            f"{len(en['itemModels'])} native default item renders per language, all {len(en['entityRenderers'])} mod entity-renderer bindings, loaded recipes/advancements and all {len(en['effects'])} custom profiles. "
            "Each mod item-description key is also checked directly in both packaged language catalogs, independently of native fallback resolution. "
            "Quad/sprite observations use seed zero for each direction and the unculled face; this does not enumerate every weighted-model outcome. "
            f"The production archive contains **{len(sounds)}** sound definitions; local sound references with missing/invalid Ogg headers: **{len(missing_audio)}**. "
            "Native/delegated sound events and dynamic music have separate C22/C24 runtime evidence.", "",
            "The complete 140-setting source/consumer/default/evidence matrix remains in [PORT_1201_SETTINGS.md](PORT_1201_SETTINGS.md), "
            "with native UI/EN/PL/save/restart/scope evidence in C31. This inventory does not turn a registered ID or a valid model into broad gameplay proof. "
            "Read the later actual behavior samples in the cycle report. C37 verifies death, respawn, dimension travel, custom benefits and near-end expiry. "
            "C38 and C39 verify native ocean and wild crop acquisition.", "",
            "Audit findings needing review:", ""]
    findings = missing + missing_audio
    out += [f"- `{identity}`: {issue}" for identity, issue in findings] if findings else ["- None in the covered registry/model/default-name/local-audio checks."]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(out) + "\n")
    print(json.dumps({"output": str(args.output), "findings": findings, "jarSha256": jar_sha}))


if __name__ == "__main__":
    main()
