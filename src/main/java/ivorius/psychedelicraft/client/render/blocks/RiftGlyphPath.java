/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class RiftGlyphPath {
    private static final int SAMPLES = 50;
    private static final int[] X = {1, -1, -1, 1};
    private static final int[] Z = {-1, -1, 1, 1};
    private final List<Segment> segments = new ArrayList<>();
    private double length;

    static RiftGlyphPath spiral(Vec3 target, double radius, double growth, int turns) {
        double height = Math.max(0, target.y);
        double yx = target.y * target.x;
        double yz = target.y * target.z;
        return new RiftGlyphPath(height, turns, y -> radius * (1 + y * growth), yx, yz, 0);
    }

    static RiftGlyphPath sphere(double height, int turns) {
        return new RiftGlyphPath(height, turns,
                y -> Mth.cos((float) (y / height - 0.5) * (float) Math.PI), 0, 0, -height * 0.5);
    }

    private RiftGlyphPath(double height, int turns, DoubleUnaryOperator radius,
                          double yx, double yz, double offsetY) {
        Vec3 previous = null;
        Vec3 previousTangent = null;
        double turnHeight = height / turns;
        double turnStart = 0;
        for (int index = 0; index <= turns * 4; index++) {
            int corner = index % 4;
            if (corner == 0 && index != 0) turnStart += turnHeight;
            double y = turnStart + turnHeight * 0.25 * corner;
            if (y > height) break;
            double distance = radius.applyAsDouble(y);
            int next = (corner + 1) % 4;
            Vec3 point = new Vec3(X[corner] * distance + (yx == 0 ? 0 : y / yx), y + offsetY,
                    Z[corner] * distance + (yz == 0 ? 0 : y / yz));
            Vec3 tangent = new Vec3(X[next] * distance * 0.5, 0, Z[next] * distance * 0.5);
            if (previous != null) {
                Segment segment = new Segment(previous, previous.add(previousTangent), point.subtract(tangent), point);
                length += segment.length;
                segments.add(segment);
            }
            previous = point;
            previousTangent = tangent;
        }
    }

    Vec3 at(double progress) {
        double distance = ((progress % 1 + 1) % 1) * length;
        for (Segment segment : segments) {
            if (distance <= segment.length && segment.length > 0) {
                return segment.at(distance / segment.length);
            }
            distance -= segment.length;
        }
        return segments.get(segments.size() - 1).end;
    }

    private static final class Segment {
        private final Vec3 start;
        private final Vec3 depart;
        private final Vec3 approach;
        private final Vec3 end;
        private final double length;

        private Segment(Vec3 start, Vec3 depart, Vec3 approach, Vec3 end) {
            this.start = start;
            this.depart = depart;
            this.approach = approach;
            this.end = end;
            double distance = 0;
            Vec3 previous = start;
            for (int sample = 1; sample <= SAMPLES; sample++) {
                Vec3 point = at((double) sample / SAMPLES);
                distance += point.distanceTo(previous);
                previous = point;
            }
            length = distance;
        }

        private Vec3 at(double t) {
            double rest = 1 - t;
            return start.scale(rest * rest * rest).add(depart.scale(3 * rest * rest * t))
                    .add(approach.scale(3 * rest * t * t)).add(end.scale(t * t * t));
        }
    }
}
