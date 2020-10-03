# NeutronBot
NeutronBot is a small Discord bot for use on friend servers. It provides a few useful functions for the server it is configured for. Currently, the main function of NeutronBot is to create a text channel only available to users in voice chat. This is incredibly useful, and its something I've wanted for a while. 

Imagine you're in a voice channel with others, and you want to send a link to only the people in the channel. You don't want to create a group DM because you're lazy, and you also don't want to send the link to public chat (because you don't want to spam other members with links they won't care about). This is main problem that NeutronBot solves. When a user joins the target voice channel, they recieve a special role that gives them access to a text channel. This text channel is visible only to members in the voice channel. When the user leaves the voice channel, the special role is removed, and the text channel becomes invisible to them again. When the voice channel becomes empty, NeutronBot deletes and replaces the special text channel to give it a clean slate.

There's also a few fun commands for the person running the instance of NeutronBot. I'll let you discover those in the code on your own :P

## Installation & Running
NeutronBot is distributed as a standard jar file. To run the bot, simply run

```bash
screen
java -jar <insertBotJarNameHere>.jar
```

If you want to run the bot in the background 24/7, you can daemonize NeutronBot on a Linux server by running the command 'screen' and then running the jar file as shown above in the new window.

## Configuration
NeutronBot requires a config file to function. An example config file is provided in the release .zip file for reference.

## Contributing & Support
This project was a short 1-2 day thing for me, so updates will likely be few and far in between. You can compile NeutronBot into a jar using the gradle 'shadowJar' task. Pull requests will be reviewed. If you are interested in setting up an instance for yourself and need help, feel free to contact me through keybase [here](https://keybase.io/jamesjulich).

## Final Notes
As mentioned, this was a really short project that I mostly did for fun/learning. It has a very specific use case, and is not meant to be used with more than 1 server. It's a fragile little thing, too, and if you try to break it (by removing it from its guild, removing perms it needs, other silly things), you'll probably be successful. It is not bug-free, and you use this bot at your own risk.

Maybe if I'm bored I'll hook the bot up to Mongo and make it public. idk. 

follow me on [github](https://github.com/jamesjulich) and [keybase](https://keybase.io/jamesjulich) okbye

## License
[GNU GPL V3](https://www.gnu.org/licenses/gpl-3.0.en.html)
