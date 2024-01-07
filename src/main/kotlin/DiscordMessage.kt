data class DiscordMessage(val guildId: Long, val channelId: Long, val messageId: Long) {
    val discordUrl = "https://discord.com/channels/${guildId}/${channelId}/${messageId}"
    val elementId = "chat-messages-${channelId}-${messageId}"
}