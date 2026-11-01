# Building the Frequency Works test jar without programming

**Status: Build workflow is prepared but has not run.** Its success will be confirmed only after GitHub Actions runs the NeoForge compiler. Do not put the source ZIP into Minecraft's mods folder.

## What you need

- A free GitHub account
- A repository containing the **extracted contents** of the source ZIP (not the ZIP as a single uploaded file)
- GitHub Actions enabled for that repository

## Steps

1. Create a new repository on GitHub (private is fine).
2. Upload the **contents** of this project folder to the repository, keeping `.github/workflows/build.yml`, `src`, and `build.gradle` in their original paths. GitHub's regular web file upload may hide dotfolders; make sure the `.github` folder is present.
3. Open the repository's **Actions** tab and select **Build NeoForge mod**.
4. Click **Run workflow** and wait for the run to complete.
5. If the run is green, open it and download the `frequencyworks-neoforge-jar` artifact. That artifact is a ZIP containing the compiled `.jar` file. Extract the `.jar` and put it in a **separate test copy** of ATM10's `mods` folder.
6. If the run is red, download the `frequencyworks-build-log` artifact and provide the log. That allows the compiler errors to be fixed precisely rather than guessed at.

A successful compile does **not** guarantee compatibility with ATM10 or item safety. Always test with a disposable world and back up your existing saves.

The source uses Java 21, Minecraft 1.21.1, NeoForge 21.1.265 and Gradle 8.10.2.
