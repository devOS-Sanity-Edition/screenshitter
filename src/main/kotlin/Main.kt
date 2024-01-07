package one.devos

import DiscordMessage
import MessageScreenshot
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.html5.WebStorage
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Wait
import java.io.ByteArrayOutputStream
import java.time.Duration
import javax.imageio.ImageIO
import kotlin.NoSuchElementException


val driver: WebDriver = ChromeDriver
    .builder()
    .apply {
        if (System.getenv("USE_REMOTE") != null) address(System.getenv("REMOTE_URL"))
    }
    .build()

suspend fun main() {
    println("Welcome to Screenshitter!")

    driver.manage().window().size = Dimension(950, 1280)
    authenticateToDiscordWeb()

    val kord = Kord(System.getenv("BOT_TOKEN"))

    kord.on<ReactionAddEvent> {
        println("Reaction added to message $messageId [${getMessage().reactions.size}]")
        println(getMessage().reactions.joinToString { "${it.data.emojiName} - ${it.data.emojiId} - ${it.id}" })

        if ((getMessage().reactions.find { it.data.emojiName == "⭐" }?.count ?: 0) >= 5) {
            val message = DiscordMessage(guildId!!.value.toLong(), channelId.value.toLong(), messageId.value.toLong())
            println("Screenshitting ${message.discordUrl}")
            val screenshot = createScreenshot(message)

            getGuildOrNull()!!
                .getChannelOf<TextChannel>(Snowflake("942533178336350348"))
                .createMessage {
                    addFile("Screenshit.png", ChannelProvider { ByteReadChannel(screenshot) })
                }
        }
    }

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}

suspend fun createScreenshot(discordMessage: DiscordMessage): ByteArray {
    driver.get(discordMessage.discordUrl)
    val htmlDiscordMessage = getMessageInDiscord(discordMessage)
    Thread.sleep(Duration.ofSeconds(1))

    val js = (driver as JavascriptExecutor)
    /*
    js.executeScript("document.getElementsByClassName(\"sidebar_ded4b5\")[0].remove()")
    js.executeScript("document.getElementsByClassName(\"wrapper_a7e7a8 guilds__2b93a\")[0].remove()")
    js.executeScript("document.getElementsByClassName(\"title_b7d661 container__11d72 themed_b152d4\")[0].remove()")
    js.executeScript("document.getElementsByClassName(\"form__13a2c\")[0].remove()")
    js.executeScript("document.getElementsByClassName(\"container_b2ce9c\")[0].remove()")
    */
    js.executeScript("document.getElementById(\"message-reactions-${discordMessage.messageId}\").remove()")

    val image = ByteArrayOutputStream().apply {
        withContext(Dispatchers.IO) {
            val ml = htmlDiscordMessage.location
            val ms = htmlDiscordMessage.size
            val m = MessageScreenshot(ml.x, ml.y - 10, ms.height + 20, ms.width)
            val screenshot = (driver as TakesScreenshot).getScreenshotAs(OutputType.BYTES)
            val crop = ImageIO
                .read(screenshot.inputStream())
                .getSubimage(m.x, m.y, m.w, m.h)

            ImageIO.write(crop, "PNG", this@apply)
        }
    }

    return image.toByteArray()
}

fun authenticateToDiscordWeb() {
    driver.get("https://discord.com/login")
    (driver as JavascriptExecutor).executeScript("""
        const iframe = document.createElement('iframe');
        document.head.append(iframe);
        const pd = Object.getOwnPropertyDescriptor(iframe.contentWindow, 'localStorage');
        iframe.remove();    
        Object.defineProperty(window, 'localStorage', pd);
    """.trimIndent())
    (driver as WebStorage).localStorage.setItem("token", "\"${System.getenv("ROOT_TOKEN")}\"")
    driver.get("https://discord.com/channels/@me")
}

fun getMessageInDiscord(discordMessage: DiscordMessage): WebElement {
    try {
        val wait: Wait<WebDriver> = FluentWait(driver)
            .withTimeout(Duration.ofSeconds(10))
            .pollingEvery(Duration.ofMillis(300))
            .ignoring(NoSuchElementException::class.java)

        return wait.until(ExpectedConditions.elementToBeClickable(By.id(discordMessage.elementId)))
    } catch (e: TimeoutException) {
        println("Selenium failed to load the message by Discord, this could be due to an outage or invalid token.")
        println((driver as TakesScreenshot).getScreenshotAs(OutputType.FILE))
        throw e
    }
}