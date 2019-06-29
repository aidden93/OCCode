package com;

/**
 * Created by IntelliJ IDEA.
 * User: Mihael Bercic
 * Date: 18. 06. 2019
 * Time: 22:14
 */

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.runemate.game.api.hybrid.Environment;
import com.runemate.game.api.hybrid.local.Screen;
import com.runemate.game.api.hybrid.util.StopWatch;
import com.runemate.game.api.script.framework.AbstractBot;
import com.runemate.game.api.script.framework.listeners.EngineListener;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by IntelliJ IDEA.
 * User: Mihael Bercic
 * Date: 18. 06. 2019
 * Time: 15:22
 */
public class OCCodeWebServices implements EngineListener {


    private String developerToken;
    private AbstractBot bot;
    private Gson gson = new Gson();

    private Map<String, Object> details = new HashMap<>();
    private Map<String, Object> customMetricsMap = new HashMap<>();
    private Map<String, Consumer<String>> responseMap = new HashMap<>();
    private List<JsonObject> customActions = new ArrayList<>();
    private String server = "https://occode.io/service";
    private StopWatch stopWatch = new StopWatch();

    // Details
    private String state = "run";
    private String loginUsername = "";
    private String status = "Status not set!";

    // Optional setup
    private Supplier<Boolean> shouldStop = () -> false;
    private Supplier<Boolean> isSafeToStop = () -> true;

    // Generated
    private int sessionId;

    // Client details
    private boolean isSetup = false;
    private boolean takeScreenshot = false;

    /**
     * Initialize Web Services class.
     *
     * @param token Your developer token.
     * @param bot   The bot it's currently running on.
     */
    public OCCodeWebServices(@Nonnull String token, @Nonnull AbstractBot bot) {
        this.developerToken = token;
        this.bot = bot;
        bot.getEventDispatcher().addListener(this);
    }

    /**
     * Adds a custom action to the initial setup.
     *
     * @param name           Action name.
     * @param buttonText     Text display on the button under "Options" on dashboard.
     * @param selectedAction In this case (no options), the argument passed to the consumer will be an empty string.
     */
    public void addCustomAction(String name, String buttonText, Consumer<String> selectedAction) {
        addCustomAction(name, buttonText, null, selectedAction);
    }

    /**
     * Adds a custom action with options.
     *
     * @param name           Action name.
     * @param buttonText     Text to be displayed on a button. Recommended: "Select", "Choose"...
     * @param actions        Possible options / actions for the user to choose from. [Combobox]
     * @param selectedAction Provided consumer to be invoked by a dashboard interface feature by the user. Selected action is the argument of the consumer.
     */
    public void addCustomAction(String name, String buttonText, List<String> actions, Consumer<String> selectedAction) {
        final JsonArray actionsArray = new JsonArray();
        if (actions != null) actions.forEach(actionsArray::add);
        JsonObject object = new JsonObject();
        object.addProperty("name", name);
        object.addProperty("buttonText", buttonText);
        object.add("actions", actionsArray);
        customActions.removeIf(element -> element.get("name").getAsString().equals(name));
        customActions.add(object);
        responseMap.put(name, selectedAction);
    }

    /**
     * Sets current session status
     *
     * @param status Brief, short description of what the bot is doing.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Set login username / Alias (Optional)
     *
     * @param username Recommended: Environment.getAccountAlias();
     */
    public void setLoginUsername(String username) {
        this.loginUsername = username;
    }


    /**
     * Adds a custom metric to keep track of. If the metric is a number, there will be graphs generated.
     *
     * @param metricName  Metric name to be displayed.
     * @param metricValue Metric value to be displayed.
     */
    public void setCustomMetric(String metricName, Object metricValue) {
        customMetricsMap.put(metricName, metricValue);
    }

    /**
     * Changes the sate to paused on our servers.
     */
    public void onPause() {
        changeState("pause");
    }

    /**
     * Changes the sate to running on our servers.
     */
    public void onResume() {
        changeState("run");
    }


    /**
     * Setups the OCCodeWebServices file.
     *
     * @param runnable Runnable in which you set the update method and custom actions (if any).
     */
    public void setup(Runnable runnable) {
        isSetup = true;
        stopWatch.start();
        runnable.run();
        details.put("token", developerToken);
        details.put("developer_username", bot.getMetaData().getAuthor());
        details.put("client_username", Environment.getForumName());
        details.put("client_name", "runemate");
        details.put("script_name", bot.getMetaData().getName());
        details.put("custom_setup", customActions);
        details.put("custom", customMetricsMap);
    }


    /**
     * [OPTIONAL] Set when your bot should stop. Example: When out of food.
     *
     * @param supplier Condition when the bot should stop.
     */
    public void setWhenToStop(Supplier<Boolean> supplier) {
        shouldStop = supplier;
    }


    /**
     * Set the boolean supplier when the bot is allowed to stop. Example: When not under attack.
     *
     * @param supplier Boolean supplier to be checked before the bot is stopped. Bot will stop once the condition is met.
     */
    public void setIsSafeToStop(Supplier<Boolean> supplier) {
        isSafeToStop = supplier;
    }

    /**
     * Sends a notification to your user.
     *
     * @param title   Notification title. Best to keep it short.
     * @param message Notification message.
     * @param type    Notification Type. One of: [SUCCESS, WARNING, ERROR, INFORMATION]. Check enum at the end of the file.
     */
    public void sendNotification(String title, String message, Notification type) {
        Map<String, Object> map = new HashMap<>(details);
        map.put("type", type.opcode);
        map.put("title", title);
        map.put("message", message);
        sendRequest(server + "/notify", "POST", gson.toJson(map));
    }


    /**
     * Sets custom metrics and their values for updating your session.
     *
     * @param runnable In this runnable set customMetrics, status (and login username, if it changes during runtime).
     */
    public void update(Runnable runnable) {
        if (!isSetup) {
            bot.getLogger().warn("[OCCODE] Update must not be called before setup!");
            bot.stop("[OCCODE] Update must not be called before setup!");
            return;
        }
        new Thread(() -> {
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (sessionId == 0) getSessionId();
                        if (shouldStop.get() || bot.isStopped()) {
                            timer.cancel();
                            timer.purge();
                        } else {
                            runnable.run(); // Runs update runnable.
                            generateMap(); // Generates details map.

                            // Requests a response from our server.
                            JsonObject response = gson.fromJson(sendRequest(server + "/update", "POST", gson.toJson(details)), JsonObject.class);

                            // Response contains the following: Current bot state, screenshot request and custom action element.
                            String newState = response.get("state").getAsString();
                            boolean hasToTakeScreenshot = response.get("screenshot").getAsBoolean();
                            JsonElement actionElement = response.get("action");
                            if (actionElement != null) {
                                JsonObject actionObject = actionElement.getAsJsonObject();
                                String actionName = actionObject.get("name").getAsString();
                                String selectedAction = actionObject.get("action").getAsString();
                                if (responseMap.containsKey(actionName)) responseMap.get(actionName).accept(selectedAction);
                            }
                            // Basic bot controls.
                            switch (state) {
                                case "run":
                                    if (!bot.isRunning() && !bot.isStopped()) bot.resume();
                                    break;
                                case "pause":
                                    if (!bot.isPaused()) bot.pause();
                                    break;
                                case "stop":
                                    if (isSafeToStop.get()) bot.stop("Session stopped through WebServices [Occode]");
                                    break;
                            }
                            // Sets current bot state to the state that the server sent.
                            state = newState;

                            // Because due to lag it can occur that if i'd do takeScreenshot = hasToTakeScreenshot, sometimes it would fail.
                            if (hasToTakeScreenshot && !takeScreenshot) takeScreenshot = true;
                        }
                    } catch (Exception e) {
                        bot.getLogger().severe("[OCCODE] Error with OCCode");
                        e.printStackTrace();
                    }
                }
            };
            timer.scheduleAtFixedRate(timerTask, 0, 1000);
        }).start();
    }

    /**
     * Generates details map.
     */
    private void generateMap() {
        details.put("session_id", sessionId);
        details.put("status", status);
        details.put("state", state);
        details.put("login_username", loginUsername);
        details.put("custom", customMetricsMap);
        details.put("custom_setup", customActions);
        details.put("runtime", stopWatch.getRuntime());
    }

    /**
     * Changes state of the session.
     *
     * @param state String representation of one of the following states: [Run, Pause, Stop].
     */
    private void changeState(String state) {
        this.state = state;
        generateMap();
        sendRequest(server + "/state", "POST", gson.toJson(details));
    }

    /**
     * Requests a new session id from the server.
     */
    private void getSessionId() {
        sessionId = Integer.parseInt(Objects.requireNonNull(sendRequest(server + "/generate", "POST", gson.toJson(details))));
    }

    /**
     * Sends HTTPS request.
     *
     * @param url           URL to send the requests to.
     * @param requestMethod POST, GET, ...
     * @param body          Body for the request.
     * @return Returns the result of the request as a String.
     */
    private String sendRequest(@Nonnull String url, @Nonnull String requestMethod, String body) {
        if (isSetup) {
            try {
                bot.getLogger().debug("[OCCODE] Sending to server: " + body);
                URL sURL = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) sURL.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod(requestMethod);
                connection.getOutputStream().write((body + "\r\n").getBytes(Charset.forName("UTF-8")));

                int code = connection.getResponseCode();
                return new BufferedReader(new InputStreamReader(code == 200 ? connection.getInputStream() : connection.getErrorStream())).readLine();
            } catch (Exception e) {
                bot.getLogger().severe("[OCCODE] Error sending request");
                e.printStackTrace();
            }
        } else {
            bot.getLogger().warn("[OCCODE] Setup must be called before a request can be sent");   
        }
        
        return null;
    }


    /**
     * Returns the new scaled dimension for the image.
     *
     * @param imageSize Current image size.
     * @param boundary  New image size.
     * @return Returns the dimension of the new image.
     */
    private Dimension getScaledDimension(Dimension imageSize, Dimension boundary) {
        int ow = imageSize.width;
        int oh = imageSize.height;

        int bw = boundary.width;
        int bh = boundary.height;

        int nw = ow;
        int nh = oh;

        // first check if we need to scale width
        if (ow > bw) {
            //scale width to fit
            nw = bw;
            //scale height to maintain aspect ratio
            nh = nw * oh / ow;
        }

        // then check if we need to scale even with the new height
        if (nh > bh) {
            //scale height to fit instead
            nh = bh;
            //scale width to maintain aspect ratio
            nw = nh * ow / oh;
        }
        return new Dimension(nw, nh);
    }

    /**
     * Resizes the image to the desired dimension.
     *
     * @param originalImage Image to be resized.
     * @param dimension     Dimensions of the new image.
     * @return Resized image.
     */
    private BufferedImage resizeImage(Image originalImage, Dimension dimension) {
        BufferedImage resizedImage = new BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(originalImage, 0, 0, dimension.width, dimension.height, null);
        g2.dispose();
        return resizedImage;
    }

    /**
     * Sends a screenshot to the server.
     */
    private void sendScreenshot() {
        try {
            BufferedImage image = Screen.capture();
            if (image != null) {
                if (image.getWidth() > 1280 || image.getHeight() > 720) image = resizeImage(image, getScaledDimension(new Dimension(image.getWidth(), image.getHeight()), new Dimension(1280, 720)));
                Map<String, Object> map = new HashMap<>(details);
                map.put("screenshot", imgToBase64String(image));
                sendRequest(server + "/screenshot", "POST", gson.toJson(map));
            }
        } catch (Exception e) {
            bot.getLogger().warn("[OCCODE] The request to send a screenshot failed");
            e.printStackTrace();
        }
    }

    /**
     * Image in the form of base64 string
     *
     * @param img Client image
     * @return Image in 64-bit string form
     */
    private String imgToBase64String(final BufferedImage img) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", os);
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Take a screenshot when needed. TODO: Think of a way to have full images.
     */
    @Override
    public void onCycleStart() {
        if (takeScreenshot) {
            takeScreenshot = false;
            sendScreenshot();
        }
    }

    public enum Notification {
        SUCCESS(0), WARNING(1), ERROR(2), INFORMATION(3);

        private int opcode;

        Notification(int opcode) {
            this.opcode = opcode;
        }

    }
}
