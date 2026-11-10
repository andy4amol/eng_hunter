#!/usr/bin/env python3
"""
【里程碑 M2】《小学英语核心 60 句高保真卡拉 OK 资源包》离线烘焙流水线 (bake_edge_tts_pack.py)

功能说明：
1. 自动扫描或导出内置的 60 个小学英语核心句式 (`sentences_manifest.json`)
2.支持两种烘焙模式：
   - `--mode manifest-only`（零网络依赖，默认）：根据 60 句词库与音节韵律模型直接生成带精确毫秒级 `WordBoundary` 的 `app/src/main/assets/sentences/sentences_manifest.json`
   - `--mode edge-tts`（需联网且安装 `pip install edge-tts`）：调用微软免费 `en-US-AnaNeural` 美式少儿音色，批量合成 60 句常速 (1.0x) 与慢速 (0.78x) 语音文件及真实 WordBoundary 时间戳，写入 `app/src/main/assets/sentences/`

用法示例：
  python3 tools/bake_edge_tts_pack.py --mode manifest-only
  python3 tools/bake_edge_tts_pack.py --mode edge-tts --voice en-US-AnaNeural
"""

import argparse
import asyncio
import json
import os
import re
from pathlib import Path
from typing import Any, Dict, List

ROOT_DIR = Path(__file__).resolve().parent.parent
KT_CATALOG_PATH = ROOT_DIR / "app/src/main/java/com/enghunter/tv/model/PrimarySchool60Catalog.kt"
ASSETS_SENTENCES_DIR = ROOT_DIR / "app/src/main/assets/sentences"
MANIFEST_OUT_PATH = ASSETS_SENTENCES_DIR / "sentences_manifest.json"
AUDIO_OUT_DIR = ASSETS_SENTENCES_DIR / "audio"

UNIT_META = {
    "UNIT_1": ("u01", "Unit 1 · 礼貌问候与日常交际 (10句)"),
    "UNIT_2": ("u02", "Unit 2 · 美食点餐与礼貌请求 (10句)"),
    "UNIT_3": ("u03", "Unit 3 · 校园生活与文具物品 (10句)"),
    "UNIT_4": ("u04", "Unit 4 · 家庭成员与兴趣运动 (10句)"),
    "UNIT_5": ("u05", "Unit 5 · 日常作息与天气穿搭 (10句)"),
    "UNIT_6": ("u06", "Unit 6 · 动物世界与问路出行 (10句)"),
}


def estimate_syllables(word: str) -> int:
    clean = re.sub(r"[^a-z]", "", word.lower())
    if not clean:
        return 1
    count = 0
    prev_vowel = False
    for ch in clean:
        is_vowel = ch in "aeiouy"
        if is_vowel and not prev_vowel:
            count += 1
        prev_vowel = is_vowel
    if clean.endswith("e") and count > 1:
        count -= 1
    return max(1, count)


def build_timings(sentence: str, slow_mode: bool) -> List[Dict[str, Any]]:
    words = [w for w in sentence.strip().split() if w]
    speed_factor = 1.38 if slow_mode else 1.0
    gap_ms = 45 if slow_mode else 28
    cursor_ms = 0
    out: List[Dict[str, Any]] = []
    for w in words:
        clean = re.sub(r"[^a-zA-Z]", "", w)
        syllables = estimate_syllables(clean)
        has_punct = w.endswith((",", ".", "?", "!"))
        base_ms = 175 + syllables * 95 + len(clean) * 14 + (90 if has_punct else 0)
        duration_ms = max(180, int(base_ms * speed_factor))
        out.append({
            "word": w,
            "startMs": cursor_ms,
            "durationMs": duration_ms
        })
        cursor_ms += duration_ms + gap_ms
    return out


def parse_kotlin_60_catalog(kt_path: Path) -> List[Dict[str, Any]]:
    text = kt_path.read_text(encoding="utf-8")
    blocks = text.split("c(\n")[1:]
    sentences: List[Dict[str, Any]] = []

    for blk in blocks:
        id_m = re.search(r'id\s*=\s*"([^"]+)"', blk)
        unit_m = re.search(r'unit\s*=\s*CurriculumUnit\.(UNIT_\d)', blk)
        pattern_m = re.search(r'pattern\s*=\s*"([^"]+)"', blk)
        en_m = re.search(r'en\s*=\s*"([^"]+)"', blk)
        zh_m = re.search(r'zh\s*=\s*"([^"]+)"', blk)
        emoji_m = re.search(r'emoji\s*=\s*"([^"]+)"', blk)
        chunks_m = re.search(r'chunks\s*=\s*listOf\(([^)]+)\)', blk)
        q_en_m = re.search(r'qEn\s*=\s*"([^"]+)"', blk)
        q_zh_m = re.search(r'qZh\s*=\s*"([^"]+)"', blk)

        if not (id_m and unit_m and en_m):
            continue

        cid = id_m.group(1)
        unit_key = unit_m.group(1)
        unit_id, unit_title = UNIT_META.get(unit_key, ("u01", "Unit 1"))
        en = en_m.group(1)
        zh = zh_m.group(1) if zh_m else ""
        pattern = pattern_m.group(1) if pattern_m else en
        emoji = emoji_m.group(1) if emoji_m else "🌟"
        chunks = re.findall(r'"([^"]+)"', chunks_m.group(1)) if chunks_m else [en]

        subs = []
        for sub_m in re.finditer(
            r'SlotOption\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"\)',
            blk
        ):
            subs.append({
                "keyword": sub_m.group(1),
                "zhHint": sub_m.group(2),
                "emoji": sub_m.group(3),
                "fullEn": sub_m.group(4),
                "fullZh": sub_m.group(5),
            })

        sentences.append({
            "id": cid,
            "unitId": unit_id,
            "unitTitle": unit_title,
            "pattern": pattern,
            "en": en,
            "zh": zh,
            "emoji": emoji,
            "chunks": chunks,
            "wordTimingsNormal": build_timings(en, slow_mode=False),
            "wordTimingsSlow": build_timings(en, slow_mode=True),
            "substitutions": subs,
            "rolePlayQuestion": q_en_m.group(1) if q_en_m else "What would you like?",
            "rolePlayQuestionZh": q_zh_m.group(1) if q_zh_m else "你想要点什么？",
            "prebakedAudioNormalAsset": f"sentences/audio/{cid}_normal.wav",
            "prebakedAudioSlowAsset": f"sentences/audio/{cid}_slow.wav",
        })

    return sentences


async def bake_with_edge_tts(sentences: List[Dict[str, Any]], voice: str) -> None:
    try:
        import edge_tts  # type: ignore
    except ImportError:
        raise RuntimeError("请先安装 edge-tts：pip install edge-tts")

    AUDIO_OUT_DIR.mkdir(parents=True, exist_ok=True)
    for item in sentences:
        cid = item["id"]
        en = item["en"]
        for slow_flag, rate_str, key_name, suffix in [
            (False, "+0%", "wordTimingsNormal", "normal"),
            (True, "-22%", "wordTimingsSlow", "slow"),
        ]:
            communicate = edge_tts.Communicate(en, voice, rate=rate_str)
            word_boundaries: List[Dict[str, Any]] = []
            audio_bytes = bytearray()
            async for chunk in communicate.stream():
                if chunk["type"] == "audio":
                    audio_bytes.extend(chunk["data"])
                elif chunk["type"] == "WordBoundary":
                    word_boundaries.append({
                        "word": chunk["text"],
                        "startMs": int(chunk["offset"] / 10_000),
                        "durationMs": max(120, int(chunk["duration"] / 10_000)),
                    })
            if word_boundaries:
                item[key_name] = word_boundaries
            out_mp3 = AUDIO_OUT_DIR / f"{cid}_{suffix}.mp3"
            out_mp3.write_bytes(bytes(audio_bytes))
            print(f"[Edge-TTS] 已烘焙 {cid} ({suffix}): {len(word_boundaries)} 个词边界 -> {out_mp3.name}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Bake EngHunter 60-sentence karaoke manifest & audio pack.")
    parser.add_argument(
        "--mode",
        choices=["manifest-only", "edge-tts"],
        default="manifest-only",
        help="manifest-only (offline zero-network) or edge-tts (fetch live Microsoft Edge neural audio)"
    )
    parser.add_argument("--voice", default="en-US-AnaNeural", help="Edge-TTS voice name")
    args = parser.parse_args()

    sentences = parse_kotlin_60_catalog(KT_CATALOG_PATH)
    if len(sentences) != 60:
        raise SystemExit(f"Expected 60 sentences from PrimarySchool60Catalog.kt, got {len(sentences)}")

    if args.mode == "edge-tts":
        asyncio.run(bake_with_edge_tts(sentences, args.voice))

    ASSETS_SENTENCES_DIR.mkdir(parents=True, exist_ok=True)
    manifest = {
        "schemaVersion": "enghunter.karaoke.pack.v2",
        "voice": args.voice,
        "totalCount": len(sentences),
        "units": [
            {"unitId": uid, "unitTitle": utitle, "count": sum(1 for s in sentences if s["unitId"] == uid)}
            for uid, utitle in UNIT_META.values()
        ],
        "sentences": sentences,
    }
    MANIFEST_OUT_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"✅ 成功生成 60 句离线卡拉 OK 资源包清单: {MANIFEST_OUT_PATH} (共 {len(sentences)} 句)")


if __name__ == "__main__":
    main()
