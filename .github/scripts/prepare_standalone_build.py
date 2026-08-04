#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

settings_path = ROOT / "settings.gradle"
build_path = ROOT / "build.gradle"

settings = settings_path.read_text(encoding="utf-8")
settings = settings.replace(
    "\ninclude 'dragonminez'\nproject(':dragonminez').projectDir = file('dragonminez')\n",
    "\n",
)
settings_path.write_text(settings.rstrip() + "\n", encoding="utf-8")

build = build_path.read_text(encoding="utf-8")
start = build.find("evaluationDependsOn(':dragonminez')")
end = build.find("\ngroup = mod_group_id", start)
if start >= 0 and end >= 0:
    build = build[:start] + build[end + 1:]

repositories_marker = "repositories {\n    // Put repositories for dependencies here\n"
if "flatDir { dirs 'libs' }" not in build:
    if repositories_marker not in build:
        raise RuntimeError("Could not locate repositories block")
    build = build.replace(
        repositories_marker,
        "repositories {\n    flatDir { dirs 'libs' }\n\n    // Put repositories for dependencies here\n",
        1,
    )

old_dependencies = '''    compileOnly project(':dragonminez')
    runtimeOnly files(dragonminezDevJar)
    runtimeOnly fg.deobf('software.bernie.geckolib:geckolib-forge-1.20.1:4.8.3')
    runtimeOnly 'com.eliotlash.mclib:mclib:20'
    runtimeOnly fg.deobf('com.github.glitchfiend:TerraBlender-forge:1.20.1-3.0.1.10')
    runtimeOnly fg.deobf('top.theillusivec4.curios:curios-forge:5.14.1+1.20.1')
    // GeckoLib — compile-only so the mixin on BoneVisibilityHandler can reference its types.
    // At runtime GeckoLib is already on the classpath via DragonMineZ.
    compileOnly fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.8.3")
'''
new_dependencies = '''    // This is an addon: compile against local release jars instead of rebuilding DragonMineZ.
    // The release workflow downloads and validates these into libs/ before Gradle runs.
    compileOnly fg.deobf("local:dragonminez:2.1.3")
    compileOnly fg.deobf("local:geckolib-forge-1.20.1:4.8.3")
'''
if old_dependencies in build:
    build = build.replace(old_dependencies, new_dependencies, 1)
elif new_dependencies not in build:
    raise RuntimeError("Could not locate DragonMineZ dependency block")

build_path.write_text(build.rstrip() + "\n", encoding="utf-8")
