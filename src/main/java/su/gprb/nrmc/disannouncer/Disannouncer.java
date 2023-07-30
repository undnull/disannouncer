package su.gprb.nrmc.disannouncer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

@SuppressWarnings("unused")
public final class Disannouncer extends JavaPlugin implements CommandExecutor
{
    private final HttpClient http = HttpClient.newHttpClient();

    private String discordWebhookURL = null;
    private NamedTextColor onEnableColor = NamedTextColor.GRAY;
    private NamedTextColor onDisableColor = NamedTextColor.GRAY;
    private NamedTextColor onCommandColor = NamedTextColor.GRAY;

    private NamedTextColor getColor(@NotNull String value)
    {
        return switch (value.toLowerCase()) {
            case "black" -> NamedTextColor.BLACK;
            case "dark_blue" -> NamedTextColor.DARK_BLUE;
            case "dark_green" -> NamedTextColor.DARK_GREEN;
            case "dark_aqua" -> NamedTextColor.DARK_AQUA;
            case "dark_red" -> NamedTextColor.DARK_RED;
            case "dark_purple" -> NamedTextColor.DARK_PURPLE;
            case "gold" -> NamedTextColor.GOLD;
            case "dark_gray" -> NamedTextColor.DARK_GRAY;
            case "blue" -> NamedTextColor.BLUE;
            case "green" -> NamedTextColor.GREEN;
            case "aqua" -> NamedTextColor.AQUA;
            case "red" -> NamedTextColor.RED;
            case "light_purple" -> NamedTextColor.LIGHT_PURPLE;
            case "yellow" -> NamedTextColor.YELLOW;
            case "white" -> NamedTextColor.WHITE;
            default -> NamedTextColor.GRAY;
        };
    }

    private String getHeadURL(@NotNull String username)
    {
        return String.format("https://mc-heads.net/avatar/%s/32", username);
    }

    private String getDateStringUTC()
    {
        final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private void doBroadcast(@NotNull String message, @NotNull String author, NamedTextColor color)
    {
        final TextComponent text = Component.text()
            .append(Component.text("[PSA]", color, TextDecoration.BOLD))
            .append(Component.text(String.format(" %s", message)))
            .build();
        Bukkit.broadcast(text);

        if((discordWebhookURL != null) && !discordWebhookURL.equals("null")) {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("`attachments`:[],");
            json.append("`content`:null,");
            json.append("`embeds`:[{");
            json.append(String.format("`title`:`%s`,", message));
            json.append(String.format("`color`:%d,", color.value()));
            json.append(String.format("`timestamp`:`%s`,", getDateStringUTC()));
            json.append(String.format("`author`:{`name`:`%s`,`icon_url`:`%s`}", author, getHeadURL(author)));
            json.append("}]");
            json.append("}");

            try {
                HttpRequest.Builder request = HttpRequest.newBuilder();
                request.uri(URI.create(discordWebhookURL));
                request.header("Content-Type", "application/json");
                request.POST(HttpRequest.BodyPublishers.ofString(json.toString().replace('`', '"')));

                http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            }
            catch(Exception ex) {
                Bukkit.getLogger().warning(ex.toString());
            }
        }
    }

    @Override
    public void reloadConfig()
    {
        super.reloadConfig();

        FileConfiguration config = getConfig();
        config.addDefault("discord_webhook", "null");
        config.addDefault("server_start_color", "gray");
        config.addDefault("server_stop_color", "gray");
        config.addDefault("command_color", "gray");
        config.options().copyDefaults(true);

        discordWebhookURL = config.getString("discord_webhook");
        onEnableColor = getColor(Objects.requireNonNull(config.getString("server_start_color")));
        onDisableColor = getColor(Objects.requireNonNull(config.getString("server_stop_color")));
        onCommandColor = getColor(Objects.requireNonNull(config.getString("command_color")));

        saveConfig();
    }

    @Override
    public void onEnable()
    {
        reloadConfig();
        Objects.requireNonNull(getCommand("broadcast")).setExecutor(this);
        doBroadcast("Server started", "CONSOLE", onEnableColor);
    }

    @Override
    public void onDisable()
    {
        doBroadcast("Server stopped", "CONSOLE", onDisableColor);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        if(args.length > 0)
            doBroadcast(String.join(" ", args), sender.getName(), onCommandColor);
        return true;
    }
}
