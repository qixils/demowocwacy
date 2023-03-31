# demowocwacy

Discord bot to support the 2023 HTSTEM April Fools event.

- **[Setup ↓](#Setup)**

[//]: # ( TODO: paste backstory from notion)

---

## Requirements
- Kotlin
- Gradle
- Discord bot token, server

## Configuration

A skeleton configuration will be generated at first run at `bot.conf`. The token, guild, channel, and roles are required at minimum.

```yaml
{
    "token" : "Bot [token]",
    "guild" : 0,
    "channel" : 0, # channel for election threads
  
    # not effected by decrees
    "protected-channels" : [],
    "protected-users" : [],

    "roles" : {
      "candidate" : 0,
      "current-leader" : 0,
      "past-leader" : 0,
      "staff" : [],
      "voter" : 0
    },
    
    "decrees" : {
        "htc" : {
            "guild" : 0
        },
        "unserious" : {
            "discussion-channels" : [],
            "hidden-category" : 0
        },
        # ...
    }
}
```

## Running

A gradle task is configured to run the application.

```shell
./gradlew run
```