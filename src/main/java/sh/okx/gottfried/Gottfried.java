package sh.okx.gottfried;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;
import sh.okx.gottfried.auth.MsaAuthenticationService;
import sh.okx.gottfried.auth.MsaAuthenticationService.MsCodeResponse;
import sh.okx.gottfried.listener.ChatListener;
import sh.okx.gottfried.listener.LoginListener;

public class Gottfried {
  private static final String MSA_CLIENT_ID = "5f5dcb60-d121-4de2-9aec-4e1acbb2e6e2";

  private static final Logger logger = Logger.getLogger(Gottfried.class.getName());

  public static void main(String[] args) throws IOException {
    File file = new File("config.toml");
    if (!file.exists()) {
      logger.info("Cannot find config: config.toml");
      return;
    }

    FileConfig config = FileConfig.of(file);
    config.load();

    login(config);
  }

  private static void login(FileConfig config) throws IOException {
    MinecraftProtocol protocol;
    if (config.<Boolean>get("offline-mode")) {
      protocol = new MinecraftProtocol(config.<String>get("username"));
    } else {
      try {
        String token = config.get("token");
        MsaAuthenticationService authService = new MsaAuthenticationService(MSA_CLIENT_ID);
        if (token != null) {
          authService.setRefreshToken(token);
        }
        String code = tryLogin(authService);

        // Can also use "new MinecraftProtocol(USERNAME, PASSWORD)"
        // if you don't need a proxy or any other customizations.
        protocol = new MinecraftProtocol(authService.getSelectedProfile(), authService.getAccessToken());

        if (code != null) {
          config.set("token", code);
        }
        config.save();
        logger.info("Authentication successful.");
      } catch (RequestException e) {
        e.printStackTrace();
        return;
      }
    }

    logger.info("Logging in");

    SessionService sessionService = new SessionService();

    Session client = new TcpClientSession(config.get("host"), config.getInt("port"), protocol);

    ServerConnection connection = new ServerConnection(client);


    client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
    client.addListener(new LoginListener(connection));
    client.addListener(new ChatListener(new BasicTranslator()));

    client.addListener(new SessionAdapter() {
      @Override
      public void disconnected(DisconnectedEvent event) {
        logger.info("Disconnected: " + event.getReason());
        if (event.getCause() != null) {
          event.getCause().printStackTrace();
        }
      }
    });

    /*client.setConnectTimeout(0);
    client.setReadTimeout(0);
    client.setWriteTimeout(0);*/
    client.connect();
  }

  private static String tryLogin(MsaAuthenticationService service) throws RequestException {
    try {
      service.login();
      return service.getRefreshToken();
    } catch (RequestException ex) {
      MsCodeResponse authCode = service.getAuthCode(true);
      logger.info(authCode.message);
      logger.info("Press enter when done.");
      new Scanner(System.in).nextLine();
      return tryLogin(service);
    }
  }
}
