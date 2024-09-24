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
- **Max worlds folder size** - Maximum number of worlds (that aren't kept, if any prefixes are set up) to keep in the `saves` folder. This number must be between 50 and 5000.
- **World Prefixes** - You can define custom world prefixes, and what to do with such worlds. For example, all world names beginning with `Random Speedrun #` will be deleted if they don't reach a bastion, otherwise they will stay in the saves folder and not get deleted. If a world prefix is not listed, such worlds are never deleted.
  
![image](https://github.com/user-attachments/assets/c6005378-08a1-4b7f-ae86-bf6d39ad0529)
