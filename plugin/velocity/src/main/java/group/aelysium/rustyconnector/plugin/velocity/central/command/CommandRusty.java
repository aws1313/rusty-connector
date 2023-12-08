package group.aelysium.rustyconnector.plugin.velocity.central.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import group.aelysium.rustyconnector.core.lib.data_transit.cache.CacheableMessage;
import group.aelysium.rustyconnector.core.lib.lang.Lang;
import group.aelysium.rustyconnector.core.lib.util.DependencyInjector;
import group.aelysium.rustyconnector.plugin.velocity.PluginLogger;
import group.aelysium.rustyconnector.plugin.velocity.central.Flame;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.scalar_family.ScalarServerFamily;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.static_family.StaticServerFamily;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.bases.PlayerFocusedServerFamily;
import group.aelysium.rustyconnector.plugin.velocity.lib.lang.VelocityLang;
import group.aelysium.rustyconnector.plugin.velocity.lib.server.PlayerServer;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.bases.BaseServerFamily;
import group.aelysium.rustyconnector.core.lib.data_transit.cache.MessageCacheService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;


public final class CommandRusty {
    public static BrigadierCommand create(DependencyInjector.DI3<Flame, PluginLogger, MessageCacheService> dependencies) {
        Flame flame = dependencies.d1();
        PluginLogger logger = dependencies.d2();
        MessageCacheService messageCacheService = dependencies.d3();

        LiteralCommandNode<CommandSource> rusty = LiteralArgumentBuilder
            .<CommandSource>literal("rc")
            .executes(context -> {
                CommandRusty.respond(VelocityLang.RC_ROOT_USAGE,context);
                return Command.SINGLE_SUCCESS;
            })
            .then(Message.build(flame, logger, messageCacheService))
            .then(Family.build(flame, logger, messageCacheService))
            .then(Send.build(flame, logger, messageCacheService))
            .then(Debug.build(flame, logger, messageCacheService))
            .then(Reload.build(flame, logger, messageCacheService))
            .build();

        // BrigadierCommand implements Command
        return new BrigadierCommand(rusty);
    }

    static void respond(Component text, CommandContext<CommandSource> context){
        context.getSource().sendMessage(text);
    }
}

class Message {
    public static ArgumentBuilder<CommandSource, ?> build(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("message")
                .executes(context -> {
                    CommandRusty.respond(VelocityLang.RC_MESSAGE_ROOT_USAGE, context);
                    return Command.SINGLE_SUCCESS;
                })
                .then(listMessages(flame, logger, messageCacheService))
                .then(getMessage(flame, logger, messageCacheService));
    }

    private static ArgumentBuilder<CommandSource, ?> listMessages(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("list")
                .executes(context -> {
                    new Thread(() -> {
                        try {
                            if(messageCacheService.size() > 10) {
                                int numberOfPages = Math.floorDiv(messageCacheService.size(),10) + 1;

                                List<CacheableMessage> messagesPage = messageCacheService.fetchMessagesPage(1);

                                CommandRusty.respond(VelocityLang.RC_MESSAGE_PAGE.build(messagesPage, 1, numberOfPages), context);

                                return;
                            }

                            List<CacheableMessage> messages = messageCacheService.messages();

                            CommandRusty.respond(VelocityLang.RC_MESSAGE_PAGE.build(messages, 1, 1), context);

                        } catch (Exception e) {
                            CommandRusty.respond(VelocityLang.RC_MESSAGE_ERROR.build("There was an issue getting those messages!\n"+e.getMessage()), context);
                        }
                    }).start();

                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("page-number", IntegerArgumentType.integer())
                        .executes(context -> {
                            new Thread(() -> {
                                try {
                                    int pageNumber = context.getArgument("page-number", Integer.class);

                                    List<CacheableMessage> messages = messageCacheService.fetchMessagesPage(pageNumber);

                                    int numberOfPages = Math.floorDiv(messageCacheService.size(),10) + 1;

                                    CommandRusty.respond(VelocityLang.RC_MESSAGE_PAGE.build(messages, pageNumber, numberOfPages), context);
                                } catch (Exception e) {
                                    CommandRusty.respond(VelocityLang.RC_MESSAGE_ERROR.build("There was an issue getting that page!\n"+e.getMessage()),context);
                                }

                            }).start();
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private static ArgumentBuilder<CommandSource, ?> getMessage(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("get")
                .executes(context -> {
                    CommandRusty.respond(VelocityLang.RC_MESSAGE_GET_USAGE, context);
                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, Long>argument("snowflake", LongArgumentType.longArg())
                        .executes(context -> {
                            try {
                                Long snowflake = context.getArgument("snowflake", Long.class);

                                CacheableMessage message = messageCacheService.findMessage(snowflake);

                                CommandRusty.respond(VelocityLang.RC_MESSAGE_GET_MESSAGE.build(message), context);
                            } catch (Exception e) {
                                CommandRusty.respond(VelocityLang.RC_MESSAGE_ERROR.build("There's no saved message with that ID!"),context);
                            }

                            return Command.SINGLE_SUCCESS;
                        })
                );
    }
}
class Family {
    public static ArgumentBuilder<CommandSource, ?> build(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("family")
                .executes(context -> {
                    try {
                        CommandRusty.respond(VelocityLang.RC_FAMILY.build(), context);
                    } catch (Exception e) {
                        CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("Something prevented us from getting the families!\n"+e.getMessage()), context);
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("familyName", StringArgumentType.string())
                        .executes(context -> {
                            try {
                                String familyName = context.getArgument("familyName", String.class);
                                BaseServerFamily family = flame.services().familyService().find(familyName);
                                if(family == null) throw new NullPointerException();

                                if(family instanceof ScalarServerFamily)
                                    CommandRusty.respond(VelocityLang.RC_SCALAR_FAMILY_INFO.build((ScalarServerFamily) family), context);
                                if(family instanceof StaticServerFamily)
                                    CommandRusty.respond(VelocityLang.RC_STATIC_FAMILY_INFO.build((StaticServerFamily) family), context);
                            } catch (NullPointerException e) {
                                CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("A family with that name doesn't exist!"),context);
                            } catch (Exception e) {
                                CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("Something prevented us from getting that family!\n"+e.getMessage()),context);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(resetIndex(flame, logger, messageCacheService))
                        .then(sort(flame, logger, messageCacheService))
                        .then(locked(flame, logger, messageCacheService))
                );
    }

    private static ArgumentBuilder<CommandSource, ?> resetIndex(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("resetIndex")
                .executes(context -> {
                    try {
                        String familyName = context.getArgument("familyName", String.class);
                        BaseServerFamily family = flame.services().familyService().find(familyName);
                        if(family == null) throw new NullPointerException();
                        if(!(family instanceof PlayerFocusedServerFamily)) {
                            CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("You can only resetIndex on scalar and static families!"),context);
                            return Command.SINGLE_SUCCESS;
                        }

                        ((PlayerFocusedServerFamily) family).loadBalancer().resetIndex();

                        if(family instanceof ScalarServerFamily)
                            CommandRusty.respond(VelocityLang.RC_SCALAR_FAMILY_INFO.build((ScalarServerFamily) family), context);
                        if(family instanceof StaticServerFamily)
                            CommandRusty.respond(VelocityLang.RC_STATIC_FAMILY_INFO.build((StaticServerFamily) family), context);
                    } catch (NullPointerException e) {
                        CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("A family with that name doesn't exist!"),context);
                    } catch (Exception e) {
                        CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("Something prevented us from doing that!\n"+e.getMessage()),context);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static ArgumentBuilder<CommandSource, ?> sort(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("sort")
                .executes(context -> {
                    try {
                        String familyName = context.getArgument("familyName", String.class);
                        BaseServerFamily family = flame.services().familyService().find(familyName);
                        if(family == null) throw new NullPointerException();
                        if(!(family instanceof PlayerFocusedServerFamily)) {
                            CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("You can only use sort on scalar and static families!"),context);
                            return Command.SINGLE_SUCCESS;
                        }

                        ((PlayerFocusedServerFamily) family).loadBalancer().completeSort();

                        if(family instanceof ScalarServerFamily)
                            CommandRusty.respond(VelocityLang.RC_SCALAR_FAMILY_INFO.build((ScalarServerFamily) family), context);
                        if(family instanceof StaticServerFamily)
                            CommandRusty.respond(VelocityLang.RC_STATIC_FAMILY_INFO.build((StaticServerFamily) family), context);
                    } catch (NullPointerException e) {
                        CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("A family with that name doesn't exist!"),context);
                    } catch (Exception e) {
                        CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("Something prevented us from doing that!\n"+e.getMessage()),context);
                    }
                    return 1;
                });
    }

    private static ArgumentBuilder<CommandSource, ?> locked(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("locked")
                .executes(context -> {
                    try {
                        String familyName = context.getArgument("familyName", String.class);
                        BaseServerFamily family = flame.services().familyService().find(familyName);
                        if(family == null) throw new NullPointerException();

                        if(family instanceof ScalarServerFamily)
                            CommandRusty.respond(VelocityLang.RC_SCALAR_FAMILY_INFO_LOCKED.build((ScalarServerFamily) family),context);
                        if(family instanceof StaticServerFamily)
                            CommandRusty.respond(VelocityLang.RC_STATIC_FAMILY_INFO_LOCKED.build((StaticServerFamily) family),context);
                    } catch (NullPointerException e) {
                        CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("A family with that name doesn't exist!"),context);
                    } catch (Exception e) {
                        CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("Something prevented us from doing that!\n"+e.getMessage()),context);
                    }
                    return 1;
                });
    }
}
class Send {
    public static ArgumentBuilder<CommandSource, ?> build(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("send")
                .executes(context -> {
                    CommandRusty.respond(VelocityLang.RC_SEND_USAGE, context);
                    return Command.SINGLE_SUCCESS;
                })
                .then(defaultSender(flame, logger, messageCacheService))
                .then(serverSender(flame, logger, messageCacheService));
    }

    private static ArgumentBuilder<CommandSource, ?> defaultSender(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                .executes(context -> {
                    CommandRusty.respond(VelocityLang.RC_SEND_USAGE, context);
                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("familyName", StringArgumentType.greedyString())
                        .executes(context -> {
                            try {
                                String familyName = context.getArgument("familyName", String.class);
                                String username = context.getArgument("username", String.class);

                                Player player = Tinder.get().velocityServer().getPlayer(username).orElse(null);
                                if(player == null) {
                                    CommandRusty.respond(VelocityLang.RC_SEND_NO_PLAYER.build(username),context);
                                    return Command.SINGLE_SUCCESS;
                                }

                                BaseServerFamily family = flame.services().familyService().find(familyName);
                                if(family == null) {
                                    CommandRusty.respond(VelocityLang.RC_SEND_NO_FAMILY.build(familyName),context);
                                    return Command.SINGLE_SUCCESS;
                                }
                                if(!(family instanceof PlayerFocusedServerFamily)) {
                                    CommandRusty.respond(VelocityLang.RC_FAMILY_ERROR.build("You can only directly send player to scalar and static families!"),context);
                                    return Command.SINGLE_SUCCESS;
                                }

                                ((PlayerFocusedServerFamily) family).connect(player);
                            } catch (Exception e) {
                                CommandRusty.respond(VelocityLang.BOXED_MESSAGE_COLORED.build("There was an issue using that command! "+e.getMessage(), NamedTextColor.RED),context);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private static ArgumentBuilder<CommandSource, ?> serverSender(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("server")
                .executes(context -> {
                    CommandRusty.respond(VelocityLang.RC_SEND_USAGE, context);
                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.string())
                        .executes(context -> {
                            CommandRusty.respond(VelocityLang.RC_SEND_USAGE, context);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("serverName", StringArgumentType.greedyString())
                                .executes(context -> {
                                    try {
                                        String serverName = context.getArgument("serverName", String.class);
                                        String username = context.getArgument("username", String.class);

                                        Player player = Tinder.get().velocityServer().getPlayer(username).orElse(null);
                                        if (player == null) {
                                            CommandRusty.respond(VelocityLang.RC_SEND_NO_PLAYER.build(username),context);
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        RegisteredServer registeredServer = Tinder.get().velocityServer().getServer(serverName).orElse(null);
                                        if (registeredServer == null) {
                                            CommandRusty.respond(VelocityLang.RC_SEND_NO_SERVER.build(serverName), context);
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        PlayerServer server = flame.services().serverService().search(registeredServer.getServerInfo());
                                        if (server == null) {
                                            CommandRusty.respond(VelocityLang.RC_SEND_NO_SERVER.build(serverName),context);
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        server.connect(player);
                                    } catch (Exception e) {
                                        CommandRusty.respond(VelocityLang.BOXED_MESSAGE_COLORED.build("There was an issue using that command! "+e.getMessage(), NamedTextColor.RED),context);
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }
}
class Debug {
    public static ArgumentBuilder<CommandSource, ?> build(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("debug")
                .executes(context -> {
                    for (Component c :flame.bootLog()) CommandRusty.respond(c,context);
                    return Command.SINGLE_SUCCESS;
                });
    }
}
class Reload {
    public static ArgumentBuilder<CommandSource, ?> build(Flame flame, PluginLogger logger, MessageCacheService messageCacheService) {
        return LiteralArgumentBuilder.<CommandSource>literal("reload")
                .executes(context -> {
                    logger.log("Reloading the proxy...");
                    try {
                        Tinder.get().rekindle();
                        logger.log("Done reloading!");
                        return 1;
                    } catch (Exception e) {
                        logger.error(e.getMessage(),e);
                    }
                    return 0;
                });
    }
}