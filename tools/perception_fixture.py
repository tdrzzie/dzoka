"""Generate an authored synthetic English WAV using the installed Windows voice, not a recording."""
import argparse
from pathlib import Path
import subprocess
import sys
import wave

ROOT=Path(__file__).resolve().parents[1]
def generate(output):
    output=Path(output).resolve()
    if not output.is_relative_to((ROOT/'.test-tmp').resolve()):raise ValueError('Fixture must remain in ignored workspace test directory')
    output.parent.mkdir(parents=True,exist_ok=True);raw=output.with_suffix('.sapi.wav')
    command="""$ErrorActionPreference='Stop';Add-Type -AssemblyName System.Speech;
$synth=New-Object System.Speech.Synthesis.SpeechSynthesizer;
$format=New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(16000,[System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen,[System.Speech.AudioFormat.AudioChannel]::Mono);
$synth.SetOutputToWaveFile($args[0],$format);$synth.Speak('blue travel bag number seven four two');$synth.Dispose()"""
    # The generated workspace path is passed as a proper single quoted literal,
    # never user-controlled PowerShell source or an execution-policy change.
    command=command.replace('$args[0]',"'"+str(raw).replace("'","''")+"'")
    subprocess.run(['powershell.exe','-NoProfile','-Command',command],check=True,timeout=45,capture_output=True)
    with wave.open(str(raw),'rb') as source:
        if source.getnchannels()!=1 or source.getsampwidth()!=2 or source.getframerate()!=16000:raise ValueError('Unexpected synthetic voice format')
        data=source.readframes(source.getnframes())
    with wave.open(str(output),'wb') as target:
        target.setnchannels(1);target.setsampwidth(2);target.setframerate(16000);target.writeframes(data)
    print('Generated synthetic local English voice fixture, no accuracy benchmark',flush=True)
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',default=str(ROOT/'.test-tmp/perception-fixtures/audio.wav'));args=parser.parse_args()
    try:generate(args.output)
    except Exception:print('Synthetic Windows voice fixture unavailable; no substitute recording used',file=sys.stderr);raise SystemExit(1)
