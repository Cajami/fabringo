"""Generate the offline Fabringo calls with Piper, NumPy and FFmpeg."""

import argparse
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import subprocess

import numpy as np
from piper import PiperVoice, SynthesisConfig


SMALL = (
    "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
    "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete",
    "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidós", "veintitrés",
    "veinticuatro", "veinticinco", "veintiséis", "veintisiete", "veintiocho", "veintinueve",
)
TENS = {3: "treinta", 4: "cuarenta", 5: "cincuenta", 6: "sesenta", 7: "setenta"}
LETTERS = ("Be", "I", "Ene", "Ge", "O")
PAUSE_SECONDS = 1.15
MAX_SECONDS = 4.8


def say_number(number):
    if number < 30:
        return SMALL[number]
    tens, units = divmod(number, 10)
    return TENS[tens] if units == 0 else f"{TENS[tens]} y {SMALL[units]}"


def synthesize(voice, phrase, length_scale):
    config = SynthesisConfig(length_scale=length_scale)
    samples = np.concatenate([
        chunk.audio_int16_array for chunk in voice.synthesize(phrase, syn_config=config)
    ]).astype(np.float32)
    audible = np.flatnonzero(np.abs(samples) > 170)
    if not audible.size:
        raise ValueError(f"Locución vacía: {phrase}")
    rate = voice.config.sample_rate
    margin = int(rate * 0.045)
    samples = samples[max(0, audible[0] - margin): audible[-1] + margin + 1]
    peak = np.max(np.abs(samples))
    return np.rint(samples * (25500 / peak)).astype("<i2")


def fit_speech(samples, rate, tempo):
    if tempo <= 1:
        return samples
    result = subprocess.run([
        "ffmpeg", "-hide_banner", "-loglevel", "error",
        "-f", "s16le", "-ar", str(rate), "-ac", "1", "-i", "pipe:0",
        "-af", f"atempo={tempo:.6f}", "-f", "s16le", "pipe:1",
    ], input=samples.tobytes(), stdout=subprocess.PIPE, check=True)
    return np.frombuffer(result.stdout, dtype="<i2")


def generate_voice(model, destination):
    voice = PiperVoice.load(model)
    rate = voice.config.sample_rate
    pause = np.zeros(round(PAUSE_SECONDS * rate), dtype="<i2")
    destination.mkdir(parents=True, exist_ok=True)
    max_duration = 0
    for number in range(1, 76):
        letter = LETTERS[(number - 1) // 15]
        first = f"{letter}, {say_number(number)}."
        second = f"{letter}, {SMALL[number // 10]}, {SMALL[number % 10]}."
        intro = synthesize(voice, first, 0.94)
        repeat = intro if number < 60 else synthesize(voice, second, 0.94)
        speech_budget = (MAX_SECONDS - PAUSE_SECONDS - 0.1) * rate
        tempo = (len(intro) + len(repeat)) / speech_budget
        intro = fit_speech(intro, rate, tempo)
        repeat = intro if number < 60 else fit_speech(repeat, rate, tempo)
        call = np.concatenate((intro, pause, repeat))
        duration = len(call) / rate
        if duration > MAX_SECONDS:
            raise ValueError(f"Bola {number}: {duration:.2f} s en {model}")

        subprocess.run([
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
            "-f", "s16le", "-ar", str(rate), "-ac", "1", "-i", "pipe:0",
            "-metadata", f"title={first} {first if number < 60 else second}",
            "-codec:a", "libvorbis", "-q:a", "4", str(destination / f"{number:02d}.ogg"),
        ], input=call.tobytes(), check=True)
        max_duration = max(max_duration, duration)
    print(f"{destination.name}: 75 locuciones, máximo {max_duration:.2f} s", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--female", type=Path, help="Piper female ONNX model")
    parser.add_argument("--male", type=Path, help="Piper male ONNX model")
    args = parser.parse_args()
    models = [(gender, model) for gender, model in vars(args).items() if model is not None]
    if not models:
        parser.error("Supply at least one model with --female or --male")
    output = Path(__file__).resolve().parents[1] / "src/main/assets/calls"
    with ThreadPoolExecutor(max_workers=len(models)) as workers:
        jobs = [workers.submit(generate_voice, model, output / gender) for gender, model in models]
        for job in jobs:
            job.result()


if __name__ == "__main__":
    main()
