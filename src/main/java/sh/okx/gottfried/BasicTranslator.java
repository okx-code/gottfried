package sh.okx.gottfried;

import java.text.MessageFormat;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicTranslator implements Translator {

  @Override
  public @NotNull Key name() {
    return Key.key("gottfried", "basic_translator");
  }

  @Override
  public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
    if (key.equals("multiplayer.player.joined")) {
      return new MessageFormat("{0} joined the game");
    } else if (key.equals("chat.type.text")) {
      return new MessageFormat("<{0}> {1}");
    } else if (key.equals("chat.type.announcement")) {
      return new MessageFormat("[{0}] {1}");
    }
    return null;
  }
}
