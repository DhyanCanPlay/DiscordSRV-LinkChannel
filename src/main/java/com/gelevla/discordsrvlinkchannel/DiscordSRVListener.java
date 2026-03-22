package com.gelevla.discordsrvlinkchannel;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DiscordSRVListener {
    private final DiscordSRVLinkChannel plugin;

    public DiscordSRVListener(DiscordSRVLinkChannel plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void discordGuildMessageReceived(DiscordGuildMessageReceivedEvent event) {
        String linkingChannel = plugin.getConfig().getString("LinkingDiscordChannel");
        if (linkingChannel == null || linkingChannel.isBlank()) return;

        try {
            Object rawEvent = event.getRawEvent();
            Object channel = call(rawEvent, "getChannel");
            String channelId = (String) call(channel, "getId");
            if (!linkingChannel.equals(channelId)) return;

            Object author = call(rawEvent, "getAuthor");
            boolean isBot = (boolean) call(author, "isBot");
            Object message = call(rawEvent, "getMessage");

            if (!isBot) {
                String content = (String) call(message, "getContentRaw");
                String normalizedContent = content == null ? "" : content.trim();

                // Only link when the message is numeric (DiscordSRV code format)
                if (normalizedContent.matches("\\d+")) {
                String authorId = (String) call(author, "getId");
                String guildId = (String) call(call(rawEvent, "getGuild"), "getId");

                Object discordSrvPlugin = DiscordSRV.getPlugin();
                Object accountLinkManager = call(discordSrvPlugin, "getAccountLinkManager");
                String reply = processLinkRequest(accountLinkManager, normalizedContent, authorId, channelId, guildId);
                if (reply != null) {
                    Object messageAction = call(channel, "sendMessage", reply);
                    if (plugin.getConfig().getBoolean("RemoveMessages")) {
                        Consumer<Object> deleteReplyLater = sentMessage -> {
                            try {
                                deleteMessageLater(sentMessage, 10L);
                            } catch (ReflectiveOperationException e) {
                                plugin.getLogger().warning("Failed to delete bot reply message: " + e.getMessage());
                            }
                        };
                        call(messageAction, "queue", deleteReplyLater);
                    } else {
                        call(messageAction, "queue");
                    }
                }
                }
            }

            if (plugin.getConfig().getBoolean("RemoveMessages")) {
                deleteMessageLater(message, 10L);
            }
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Failed to process Discord link message due to DiscordSRV/JDA API mismatch: " + e.getMessage());
        }
    }

    private void deleteMessageLater(Object message, long delaySeconds) throws ReflectiveOperationException {
        Object deleteAction = call(message, "delete");
        call(deleteAction, "queueAfter", delaySeconds, TimeUnit.SECONDS);
    }

    private Object call(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private Object call(Object target, String methodName, Object... args) throws ReflectiveOperationException {
        Method method = findCompatibleMethod(target.getClass(), methodName, args);
        if (method == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + methodName + "(" + args.length + " args)");
        }
        return method.invoke(target, args);
    }

    private String processLinkRequest(Object accountLinkManager, String content, String authorId, String channelId, String guildId) throws ReflectiveOperationException {
        Object[][] attempts = new Object[][]{
                new Object[]{content, authorId},
                new Object[]{content, authorId, channelId, guildId},
                new Object[]{content, authorId, channelId, null},
                new Object[]{content, authorId, null, null}
        };

        ReflectiveOperationException lastError = null;
        for (Object[] args : attempts) {
            try {
                Object result = call(accountLinkManager, "process", args);
                return result == null ? null : result.toString();
            } catch (ReflectiveOperationException e) {
                lastError = e;
            }
        }

        throw lastError == null
                ? new NoSuchMethodException(accountLinkManager.getClass().getName() + ".process")
                : lastError;
    }

    private Method findCompatibleMethod(Class<?> type, String methodName, Object[] args) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName)) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length != args.length) continue;
            if (areCompatible(params, args)) return method;
        }
        return null;
    }

    private boolean areCompatible(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = args[i];
            Class<?> paramType = wrapPrimitive(paramTypes[i]);

            if (arg == null) {
                if (paramType.isPrimitive()) return false;
                continue;
            }

            Class<?> argType = wrapPrimitive(arg.getClass());
            if (!paramType.isAssignableFrom(argType)) return false;
        }
        return true;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }


    @Subscribe
    public void accountLinked(AccountLinkedEvent event) {

    }

    @Subscribe
    public void accountUnlinked(AccountUnlinkedEvent event) {

    }
}
