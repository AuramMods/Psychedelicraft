"""Generate the port's original mono bee sounds with Python 3 and sndfile-convert."""
import math
from pathlib import Path
import random
import struct
import subprocess
from tempfile import TemporaryDirectory
import wave


RATE = 22050
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/psychedelicraft/sounds/bee"


def write_clip(name, duration, synthesis):
    frames = round(duration * RATE)
    samples = [synthesis(i / RATE) for i in range(frames)]
    pcm = struct.pack(f"<{frames}h", *(round(max(-1, min(1, sample)) * 32767) for sample in samples))
    with TemporaryDirectory() as directory:
        wav = Path(directory) / f"{name}.wav"
        with wave.open(str(wav), "wb") as clip:
            clip.setnchannels(1)
            clip.setsampwidth(2)
            clip.setframerate(RATE)
            clip.writeframes(pcm)
        subprocess.run(["sndfile-convert", str(wav), str(OUTPUT / f"{name}.ogg")], check=True)


def buzz(phase, brightness):
    """Wingbeat fundamental and progressively quieter harmonics."""
    return (
        math.sin(phase)
        + 0.38 * math.sin(2 * phase + 0.7)
        + brightness * (0.2 * math.sin(3 * phase) + 0.12 * math.sin(5 * phase + 0.3))
    )


def flight(t, duration, frequency, amplitude, brightness):
    orbit = math.tau * t / duration
    phase = math.tau * frequency * t + 1.4 * math.sin(3 * orbit) + 0.4 * math.sin(7 * orbit)
    flutter = 0.78 + 0.12 * math.cos(5 * orbit) + 0.1 * math.cos(11 * orbit)
    return amplitude * flutter * buzz(phase, brightness)


def chirp(duration, start, finish, amplitude, noise=0, wobble=0, seed=31):
    rng = random.Random(seed)
    filtered = 0.0

    def sample(t):
        nonlocal filtered
        position = t / duration
        envelope = min(t / 0.018, 1.0) * (1 - position) ** 1.6
        phase = math.tau * (start * t + (finish - start) * t * position / 2)
        filtered = filtered * 0.76 + rng.uniform(-1, 1) * 0.24
        return amplitude * envelope * (buzz(phase + wobble * math.sin(56 * t), 1 + noise) + noise * filtered)

    return sample


def shear():
    rng = random.Random(184)
    last = 0.0

    def sample(t):
        nonlocal last
        value = rng.uniform(-1, 1)
        scrape = value - last
        last = value
        cutoff = math.exp(-((t - 0.055) / 0.024) ** 2) + 0.63 * math.exp(-((t - 0.13) / 0.027) ** 2)
        return 0.25 * cutoff * (0.66 * scrape + 0.34 * math.sin(math.tau * 2400 * t))

    return sample


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name, duration, frequency, amplitude, brightness in (
        ("flight", 6, 208, 0.17, 0.65),
        ("flight_angry", 4, 258, 0.24, 1.6),
    ):
        write_clip(name, duration, lambda t: flight(t, duration, frequency, amplitude, brightness))
    for name, duration, start, end, amplitude, noise, wobble in (
        ("pollinate", 0.66, 182, 130, 0.06, 0.4, 1.4),
        ("sting", 0.28, 1150, 120, 0.17, 1.3, 0.3),
        ("hurt", 0.62, 316, 137, 0.25, 0.7, 1.8),
        ("death", 1.05, 317, 77, 0.25, 0.6, 2.7),
        ("nest_enter", 1.25, 263, 103, 0.13, 0.5, 2.0),
        ("nest_exit", 0.42, 189, 464, 0.17, 0.6, 1.5),
        ("nest_work", 1.22, 130, 94, 0.11, 0.6, 4.2),
    ):
        write_clip(name, duration, chirp(duration, start, end, amplitude, noise, wobble))
    write_clip("nest_shear", 0.28, shear())


if __name__ == "__main__":
    main()
