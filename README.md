# demowocwacy

Discord bot for running the April Fools 2024 event on HTwins STEM,
featuring an election cycle, and a decree system inspired by Blaseball.

- **[Backstory ↓](#Backstory)**
- **[Setup ↓](#Setup)**

---

## Backstory

The original backstory for the event.
Unfortunately, it largely went unused.

> HTwins STEM+ has deteriorated from the iron grip of the server's corrupt, authoritarian moderation team. A group of rebels going by the title Free And Mighty (F.A.M.) has successfully staged a coup and taken over the server.
>
> *(or maybe the Secret Union of Sovereignty: S.U.S.)*
>
> The F.A.M.'s celebration is cut short by a shout from the audience: "...so who's our ruler now?”
>
> Silence falls upon the crowd as people hesitantly look around at their peers. Then, with a bang and a flash of light, a body falls to the ground. Then a second, then a third. All hell has broken loose.
>
> A week has past. Chaos and anarchy has descended upon HTSTEM. What the server needs is a strong ruler who can bring peace back to the nation.

---

## Setup

### Requirements
- Kotlin
- Gradle
- Discord bot token, server
- OpenAI token

### Configuration

A skeleton configuration will be generated at first run at `bot.conf`. The token, guild, channel, and roles are required at minimum.

[//]: # (TODO: update)

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

Also be sure to create AutoMod rules to accompany the AutoMod decrees.

### Running

A gradle task is configured to run the application.

```shell
./gradlew run
```