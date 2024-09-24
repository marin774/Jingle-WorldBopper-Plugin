# Jingle WorldBopper Plugin
A Jingle plugin which keeps your saves folder under a certain size. This reduces disk space usage, and it helps MultiMC to load instance settings faster (since it has fewer worlds to load).

## Installation
Download the latest version from the [Releases page](https://github.com/marin774/Jingle-Worldbopper-Plugin/releases). Drag and drop it into your Jingle plugins folder, and restart Jingle.

## Setup
Once you've installed the plugin and restarted Jingle, enable the plugin and configure it:
1. Open the "Plugins" tab in Jingle.
2. Click on "Open Config" next to WorldBopper.
3. Enable WorldBopper by clicking on the "Enable WorldBopper?" checkbox.

## Config
- **Enable WorldBopper?** - Whether worlds should be actively bopped or not.
- **Keep worlds with nether enters?** - Whether to keep 'Random Speedrun #' worlds that have reached the Nether. You can still manually clear them in Jingle by pressing the Clear Worlds button.
- **Max worlds folder size** - Maximum number of worlds (that aren't nether enters, if that option is enabled) to keep in the `saves` folder. This number must be between 50 and 5000.

![image](https://github.com/user-attachments/assets/4a1d0a42-6a24-4715-9025-3715ccad4d97)

## What worlds are deleted?
WorldBopper plugin deletes the same worlds a Jingle `Clear Worlds` function would.
This includes world files that begin with 'New World', 'Random Speedrun #', 'Set Speedrun #', 'Benchmark Reset #', 'Practice Seed' and 'Seed Paster'.

If you have enabled the 'Keep worlds with nether enters?' option, 'Random Speedrun #' worlds that have reached Nether will not be deleted.
