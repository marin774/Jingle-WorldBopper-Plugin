# Jingle WorldBopper Plugin
A Jingle plugin which clears certain worlds from your saves folder. This reduces disk space usage, and it helps the launcher load instance settings faster.

## Installation
Download the latest version from the [Releases page](https://github.com/marin774/Jingle-Worldbopper-Plugin/releases). Drag and drop it into your Jingle plugins folder, and restart Jingle.

## Setup
Once you've installed the plugin and restarted Jingle, enable the plugin and configure it:
1. Open the "Plugins" tab in Jingle.
2. Enable WorldBopper by clicking on the "Enable WorldBopper?" checkbox.
   
You can now configure the plugin.

## Config
- **Enable WorldBopper** - Whether worlds should be actively bopped or not.
- **Boppable worlds** - You can define custom world prefixes, and what to do with such worlds. For example:
  - World names beginning with `Random Speedrun #` will be deleted if they don't reach a bastion, otherwise they will stay in the saves folder and not get deleted.
     - Note: this prefix is special - it can't be deleted or modified; it tries to follow speedrun.com verification requirements (world you played in + 5 previous worlds)
  - World names beginning with `Benchmark Reset #` will always be deleted (only the most recent one will be kept).
  - World names beginning with `New World` will always be deleted (only the most recent 5 worlds will be kept).

<img width="544" height="329" alt="image" src="https://github.com/user-attachments/assets/14a30245-6675-4d25-97cb-ddf8eeff5aec" />

Note: World names that aren't listed here will NOT be deleted (i.e. practice maps).

Note: Some prefixes can NOT be removed or changed, and these worlds will NOT be cleared during wall screen due to leaderboard verification requirements (world you played in + 5 previous worlds).
