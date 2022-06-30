package sh.okx.gottfried.listener;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import java.util.Locale;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.Translator;

public class ChatListener extends SessionAdapter {

  private final Logger logger = Logger.getLogger("Chat");

  private final ComponentRenderer<Locale> renderer;

  public ChatListener(Translator translator) {
    this.renderer = TranslatableComponentRenderer.usingTranslationSource(translator);
  }

  @Override
  public void packetReceived(Session session, Packet packet) {
    try {
      if (packet instanceof ClientboundChatPacket chatPacket) {
        String string = PlainTextComponentSerializer.plainText().serialize(
            renderer.render(chatPacket.getMessage(), Locale.ENGLISH));
        if (!string.isEmpty()) {
          logger.info("[CHAT] " + string);
        }
      }
    } catch (RuntimeException ex) {
      ex.printStackTrace();
    }
  }
}
