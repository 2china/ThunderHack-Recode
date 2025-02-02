package thunder.hack;

import com.mojang.logging.LogUtils;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.util.math.BlockPos;
import thunder.hack.core.Core;
import thunder.hack.core.impl.*;
import thunder.hack.gui.mainmenu.CreditsScreen;
import thunder.hack.gui.notification.NotificationManager;
import thunder.hack.modules.client.RPC;
import thunder.hack.utility.SoundUtility;
import thunder.hack.utility.ThunderUtility;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class ThunderHack implements ModInitializer {
    public static final ModMetadata MOD_META;
    public static final String MOD_ID = "thunderhack";
    public static final IEventBus EVENT_BUS = new EventBus();
    public static final String VERSION = "1.4b2212";

    public static boolean isOutdated = false;
    public static float TICK_TIMER = 1f;
    public static BlockPos gps_position;
    public static Color copy_color = new Color(-1);
    public static long initTime;
    public static KeyListening currentKeyListener;
    public static String[] contributors = new String[16];

    /*-----------------    Managers  ---------------------*/
    public static NotificationManager notificationManager = new NotificationManager();
    public static WayPointManager wayPointManager = new WayPointManager();
    public static ModuleManager moduleManager = new ModuleManager();
    public static FriendManager friendManager = new FriendManager();
    public static ServerManager serverManager = new ServerManager();
    public static PlayerManager playerManager = new PlayerManager();
    public static CombatManager combatManager = new CombatManager();
    public static ConfigManager configManager = new ConfigManager();
    public static ShaderManager shaderManager = new ShaderManager();
    public static AsyncManager asyncManager = new AsyncManager();
    public static MacroManager macroManager = new MacroManager();
    public static CommandManager commandManager = new CommandManager();
    public static Core core = new Core();
    /*--------------------------------------------------------*/

    static {
        MOD_META = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .orElseThrow()
                .getMetadata();
    }


    @Override
    public void onInitialize() {
        initTime = System.currentTimeMillis();

        EVENT_BUS.registerLambdaFactory("thunder.hack", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        EVENT_BUS.subscribe(notificationManager);
        EVENT_BUS.subscribe(serverManager);
        EVENT_BUS.subscribe(playerManager);
        EVENT_BUS.subscribe(combatManager);
        EVENT_BUS.subscribe(asyncManager);
        EVENT_BUS.subscribe(core);

        FriendManager.loadFriends();
        configManager.load(configManager.getCurrentConfig());
        moduleManager.onLoad();
        configManager.loadChestStealer();
        configManager.loadInvCleaner();
        configManager.loadAutoBuy();
        configManager.loadSearch();
        configManager.loadNuker();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FriendManager.saveFriends();
            configManager.save(configManager.getCurrentConfig());
            wayPointManager.saveWayPoints();
            macroManager.saveMacro();
            configManager.saveChestStealer();
            configManager.saveSearch();
            configManager.saveNuker();
            configManager.saveInvCleaner();
            configManager.saveAutoBuy();
        }));

        macroManager.onLoad();
        wayPointManager.onLoad();

        Render2DEngine.initShaders();

        SoundUtility.registerSounds();
        syncVersion();
        syncContributors();
        ThunderUtility.parseChangeLog();

        if (isOnWindows())
            RPC.getInstance().startRpc();

        LogUtils.getLogger().info("""
                \n /$$$$$$$$ /$$                                 /$$                     /$$   /$$                     /$$     \s
                |__  $$__/| $$                                | $$                    | $$  | $$                    | $$     \s
                   | $$   | $$$$$$$  /$$   /$$ /$$$$$$$   /$$$$$$$  /$$$$$$   /$$$$$$ | $$  | $$  /$$$$$$   /$$$$$$$| $$   /$$
                   | $$   | $$__  $$| $$  | $$| $$__  $$ /$$__  $$ /$$__  $$ /$$__  $$| $$$$$$$$ |____  $$ /$$_____/| $$  /$$/
                   | $$   | $$  \\ $$| $$  | $$| $$  \\ $$| $$  | $$| $$$$$$$$| $$  \\__/| $$__  $$  /$$$$$$$| $$      | $$$$$$/\s
                   | $$   | $$  | $$| $$  | $$| $$  | $$| $$  | $$| $$_____/| $$      | $$  | $$ /$$__  $$| $$      | $$_  $$\s
                   | $$   | $$  | $$|  $$$$$$/| $$  | $$|  $$$$$$$|  $$$$$$$| $$      | $$  | $$|  $$$$$$$|  $$$$$$$| $$ \\  $$
                   |__/   |__/  |__/ \\______/ |__/  |__/ \\_______/ \\_______/|__/      |__/  |__/ \\_______/ \\_______/|__/  \\__/   \s
                   \n \t\t\t\t\t\tBy\s""" + ThunderUtility.getAuthors());

        LogUtils.getLogger().info("[ThunderHack] Init time: " + (System.currentTimeMillis() - initTime) + " ms.");

        initTime = System.currentTimeMillis();
    }

    public static void syncVersion() {
        try {
            if (!new BufferedReader(new InputStreamReader(new URL("https://raw.githubusercontent.com/Pan4ur/THRecodeUtil/main/syncVersion.txt").openStream())).readLine().equals(VERSION))
                isOutdated = true;
        } catch (Exception ignored) {
        }
    }

    public static void syncContributors() {
        try {
            URL list = new URL("https://raw.githubusercontent.com/Pan4ur/THRecodeUtil/main/thTeam.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(list.openStream(), StandardCharsets.UTF_8));
            String inputLine;
            int i = 0;
            while ((inputLine = in.readLine()) != null) {
                contributors[i] = inputLine.trim();
                i++;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isOnWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    public static boolean isFuturePresent() {
        return !FabricLoader.getInstance().getModContainer("future").isEmpty();
    }

    public enum KeyListening {
        ThunderGui,
        ClickGui,
        Search,
        Sliders,
        Strings
    }
}

