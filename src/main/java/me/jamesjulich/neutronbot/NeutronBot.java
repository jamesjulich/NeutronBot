package me.jamesjulich.neutronbot;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class NeutronBot
{
    public static String token;
    public static String guildID;
    public static String voiceChannelID;
    public static String roleID;
    public static Snowflake botID;

    public static ArrayList<Snowflake> meanyFaces = new ArrayList<Snowflake>();

    public static void main(String[] args) throws FileNotFoundException
    {
        File configFile = new File("config.yml");

        if (!configFile.exists())
        {
            info("Could not find configuration file config.yml in current directory, exiting.");
            System.exit(0);
        }

        List<String> configValues = new ArrayList<String>();

        Scanner configScanner = new Scanner(configFile);

        while (configScanner.hasNextLine())
        {
            String line = configScanner.nextLine();

            //If line is not blank and is not a comment line, the line is part of the config.
            if (line.trim().length() != 0 && !line.startsWith("#"))
            {
                configValues.add(line);
            }
        }

        if (configValues.size() < 5)
        {
            info("Could not retrieve needed config values. Exiting.");
            System.exit(0);
        }

        token = configValues.get(0).trim();
        guildID = configValues.get(1).trim();
        voiceChannelID = configValues.get(2).trim();
        roleID = configValues.get(3).trim();

        List<String> privellegedUsers = new ArrayList<String>();
        for (int i = 4; i < configValues.size(); i++)
        {
            privellegedUsers.add(configValues.get(i).trim());
        }

        GatewayDiscordClient client = DiscordClient.create(token).login().block();
        VoiceChannel vc = (VoiceChannel) client.getChannelById(Snowflake.of(voiceChannelID)).block();
        Guild guild = client.getGuildById(Snowflake.of(guildID)).block();

        Collection<Snowflake> startingMembers = getIDsInVoice(vc);

        botID = client.getSelfId();

        //Remove all users in guild and not on VC from role.
        for (Member m : guild.getMembers().toIterable())
        {
            if (hasRank(m) && !startingMembers.contains(m.getId()))
            {
                info("Removing user " + m.getId() + " from privileged role.");
                m.removeRole(Snowflake.of(roleID)).block();
            }
        }

        //Add all users currently in VC to role.
        for (Snowflake s : startingMembers)
        {
            Member m = guild.getMemberById(s).block();
            if (!hasRank(m))
            {
                info("Adding user " + m.getId() + " to privileged role.");
                m.addRole(Snowflake.of(roleID)).block();
            }
        }

        //Create a privelleged channel if it doesn't already exist.
        if (!textChannelExists(client.getGuildById(Snowflake.of(guildID)).block()))
        {
            createChannel(guild);
        }

        //Create event to add/remove people from privelleged role and clear text chat.
        client.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(event -> {
           if (userJoinedTargetChannel(event))
            {
                if (!hasRank(event.getCurrent().getMember().block()))
                {
                    event.getCurrent().getMember().block().addRole(Snowflake.of(roleID)).block();
                }
            }
            else
            {
                if (hasRank(event.getCurrent().getMember().block()))
                {
                    event.getCurrent().getMember().block().removeRole(Snowflake.of(roleID)).block();
                }

                if (getUsersInVoice(vc).size() == 0)
                {
                    info("Clearing text channel.");
                    clearChannel(client.getGuildById(Snowflake.of(guildID)).block());
                }
            }

            //basically a perm mute
            if (meanyFaces.contains(event.getCurrent().getUserId()) && !event.getCurrent().isMuted() && event.getCurrent().getChannelId().isPresent())
            {
                event.getCurrent().getMember().block().edit(spec -> {
                    spec.setMute(true);
                }).block();
            }

            //When your friends try to be funny by muting you, but you can code.
            if (privellegedUsers.contains(event.getCurrent().getUserId().asString()) && event.getCurrent().isMuted() && event.getCurrent().getChannelId().isPresent() && !meanyFaces.contains(event.getCurrent().getUserId()))
            {
                event.getCurrent().getMember().block().edit(spec -> {
                    spec.setMute(false);
                }).block();
            }
        });

        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            if (event.getMessage().getContent().trim().equals("*stop"))
            {
                if (event.getMember().isPresent())
                {
                    if (!privellegedUsers.contains(event.getMember().get().getId().asString()))
                    {
                        event.getMessage().getChannel().block().createMessage("Permission denied.").block();
                        return;
                    }
                    info("Stopping.");
                    client.logout().block();
                }
            }
            else if (event.getMessage().getContent().startsWith("*meanyface")) //When your friends can mute you, but you can program.
            {
                if (!privellegedUsers.contains(event.getMember().get().getId().asString()))
                {
                    event.getMessage().getChannel().block().createMessage("Permission denied.").block();
                    return;
                }

                String[] cmdArgs = event.getMessage().getContent().trim().split(" ");

                if (cmdArgs.length != 2)
                {
                    event.getMessage().getChannel().block().createMessage("Incorrect parameters. Usage: *meanyface <userID>").block();
                    return;
                }

                if (!userExists(client, Snowflake.of(cmdArgs[1])))
                {
                    event.getMessage().getChannel().block().createMessage("User not found.").block();
                    return;
                }
                meanyFaces.add(Snowflake.of(cmdArgs[1]));

                Member member = event.getGuild().block().getMemberById(Snowflake.of(cmdArgs[1])).block();
                if (userIsInVoice(member))
                {
                    member.edit(spec -> {
                        spec.setMute(true);
                    }).block();
                }
                event.getMessage().getChannel().block().createMessage(member.getMention() + ", prepare to face the wrath of Jimmy Neutron: Boy Genius.").block();
            }
            else if (event.getMessage().getContent().startsWith("*forgive"))
            {
                if (!privellegedUsers.contains(event.getMember().get().getId().asString()))
                {
                    event.getMessage().getChannel().block().createMessage("Permission denied.").block();
                    return;
                }

                String[] cmdArgs = event.getMessage().getContent().trim().split(" ");

                if (cmdArgs.length != 2)
                {
                    event.getMessage().getChannel().block().createMessage("Incorrect parameters. Usage: *forgive <userID>").block();
                    return;
                }

                if (!meanyFaces.contains(Snowflake.of(cmdArgs[1])))
                {
                    event.getMessage().getChannel().block().createMessage("User is not a meanyface.").block();
                    return;
                }
                meanyFaces.remove(Snowflake.of(cmdArgs[1]));

                Member member = event.getGuild().block().getMemberById(Snowflake.of(cmdArgs[1])).block();
                if (userIsInVoice(member))
                {
                    member.edit(spec -> {
                        spec.setMute(false);
                    }).block();
                }
                event.getMessage().getChannel().block().createMessage("User forgiven.").block();
            }
        });

        client.onDisconnect().block();
    }

    //Deletes all channels named "vc-text-chat"
    public static void clearChannel(Guild guild)
    {
        if (textChannelExists(guild))
        {
            getPrivellegedChannel(guild).delete().block();
        }
        createChannel(guild);
    }

    public static void createChannel(Guild guild)
    {
        VoiceChannel vc = (VoiceChannel) guild.getChannelById(Snowflake.of(voiceChannelID)).block();
        Snowflake newChannelID = guild.createTextChannel(spec -> {
            spec.setName("vc-text-chat");
            if (vc.getCategoryId().isPresent())
            {
                spec.setParentId(vc.getCategoryId().get());
            }
            spec.setPosition(guild.getChannelById(Snowflake.of(voiceChannelID)).block().getPosition().block());
        }).block().getId();

        //Allow bot to see channel so it can continue editing permissions/delete the channel eventually.
        guild.getChannelById(newChannelID).block().addMemberOverwrite(botID, PermissionOverwrite.forRole(Snowflake.of(roleID),
                PermissionSet.of(Permission.VIEW_CHANNEL),
                PermissionSet.none())).block();

        guild.getChannelById(newChannelID).block().addRoleOverwrite(Snowflake.of(roleID), PermissionOverwrite.forRole(Snowflake.of(roleID),
                PermissionSet.of(Permission.VIEW_CHANNEL),
                PermissionSet.none())).block();

        //Block @everyone from seeing channel. The @everyone role has the same ID as the guild, per discord api docs.
        guild.getChannelById(newChannelID).block().addRoleOverwrite(Snowflake.of(guildID), PermissionOverwrite.forRole(Snowflake.of(guildID),
                PermissionSet.none(),
                PermissionSet.of(Permission.VIEW_CHANNEL))).block();
    }

    public static boolean textChannelExists(Guild guild)
    {
        return getPrivellegedChannel(guild) != null;
    }

    //Sometimes this method needs time to retrieve updates not from cache, so it is run 3 times in intervals of 100ms before returning null.
    public static TextChannel getPrivellegedChannel(Guild guild)
    {
        int attempts = 0;
        while (attempts < 3)
        {
            for (Channel c : guild.getChannels().toIterable())
            {
                if (c instanceof TextChannel)
                {
                    if (((TextChannel) c).getName().equals("vc-text-chat"))
                    {
                        return (TextChannel) c;
                    }
                }
            }
            attempts++;
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                info("Unknown exception. Exiting.");
                System.exit(0);
            }
        }
        return null;
    }

    public static boolean hasRank(Member member)
    {
        return member.getRoleIds().contains(Snowflake.of(roleID));
    }

    public static boolean userJoinedTargetChannel(VoiceStateUpdateEvent event)
    {
        return (event.getCurrent().getChannelId().isPresent() && event.getCurrent().getChannelId().get().equals(Snowflake.of(voiceChannelID)));
    }

    public static boolean userIsInVoice(Member member)
    {
        return member.getVoiceState().block() != null;
    }

    public static Collection<Member> getUsersInVoice(VoiceChannel vc)
    {
        return makeCollection(vc.getVoiceStates().map(vs -> {
            return vs.getMember().block();
        }).toIterable());
    }

    public static Collection<Snowflake> getIDsInVoice(VoiceChannel vc)
    {
        return makeCollection(vc.getVoiceStates().map(vs -> {
            return vs.getMember().block().getId();
        }).toIterable());
    }

    public static boolean userExists(GatewayDiscordClient client, Snowflake id)
    {
        try
        {
            client.getUserById(id).block();
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    public static void info(String msg)
    {
        System.out.println("[NeutronBot] [" + new Date().toString() + "] " + msg);
    }

    public static <E> Collection<E> makeCollection(Iterable<E> iter)
    {
        Collection<E> list = new ArrayList<E>();
        for (E item : iter)
        {
            list.add(item);
        }
        return list;
    }
}
