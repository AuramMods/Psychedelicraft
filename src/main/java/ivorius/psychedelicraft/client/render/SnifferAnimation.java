package ivorius.psychedelicraft.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SnifferAnimation {
    static final Map<String, SnifferAnimation> CLIPS = load();
    private final float seconds;
    private final boolean loop;
    private final List<Channel> channels = new ArrayList<>();

    private SnifferAnimation(JsonObject json) {
        seconds = json.get("seconds").getAsFloat();
        loop = json.get("loop").getAsBoolean();
        for (JsonElement track : json.getAsJsonArray("channels")) channels.add(new Channel(track.getAsJsonArray()));
    }

    void apply(Map<String, SnifferModel.Part> parts, float time, float weight) {
        float position = loop ? time % seconds : Math.min(time, seconds);
        for (Channel channel : channels) channel.apply(parts.get(channel.part), position, weight);
    }

    private static Map<String, SnifferAnimation> load() {
        String path = "/assets/psychedelicraft/animations/sniffer.json";
        try (InputStream stream = SnifferAnimation.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing animation " + path);
            JsonObject root = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, SnifferAnimation> clips = new HashMap<>();
            root.entrySet().forEach(entry -> clips.put(entry.getKey(), new SnifferAnimation(entry.getValue().getAsJsonObject())));
            return clips;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load animation " + path, exception);
        }
    }

    private enum Property { POSITION, ROTATION, SCALE }

    private static final class Channel {
        private final String part;
        private final Property property;
        private final Frame[] frames;

        private Channel(JsonArray json) {
            part = json.get(0).getAsString();
            property = Property.valueOf(json.get(1).getAsString());
            JsonArray keys = json.get(2).getAsJsonArray();
            frames = new Frame[keys.size()];
            for (int i = 0; i < keys.size(); i++) frames[i] = new Frame(keys.get(i).getAsJsonArray());
        }

        private void apply(SnifferModel.Part part, float time, float weight) {
            int end = 0;
            while (end < frames.length - 1 && frames[end].time < time) end++;
            int start = Math.max(0, end - 1);
            Frame from = frames[start];
            Frame to = frames[end];
            float delta = start == end ? 0F : MathHelper.clamp((time - from.time) / (to.time - from.time), 0F, 1F);
            Frame previous = frames[Math.max(0, start - 1)];
            Frame next = frames[Math.min(frames.length - 1, end + 1)];
            float x = interpolate(to.spline, previous.x, from.x, to.x, next.x, delta) * weight;
            float y = interpolate(to.spline, previous.y, from.y, to.y, next.y, delta) * weight;
            float z = interpolate(to.spline, previous.z, from.z, to.z, next.z, delta) * weight;
            switch (property) {
                case POSITION: part.translate(x, y, z); break;
                case ROTATION: part.rotate(x, y, z); break;
                case SCALE: part.scale(x, y, z); break;
            }
        }

        private static float interpolate(boolean spline, float a, float b, float c, float d, float t) {
            if (!spline) return b + (c - b) * t;
            return .5F * ((2F * b) + (c - a) * t
                    + (2F * a - 5F * b + 4F * c - d) * t * t
                    + (3F * b - a - 3F * c + d) * t * t * t);
        }
    }

    private static final class Frame {
        private final float time, x, y, z;
        private final boolean spline;

        private Frame(JsonArray json) {
            time = json.get(0).getAsFloat();
            x = json.get(1).getAsFloat();
            y = json.get(2).getAsFloat();
            z = json.get(3).getAsFloat();
            spline = json.get(4).getAsBoolean();
        }
    }
}
