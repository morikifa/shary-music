#!/usr/bin/env python3
"""Проверка database.rules.json на ошибки, которые ловит только консоль Firebase.

Консоль отклоняет правила с сообщением вида
«Error saving rules - Line 17: ! only operates on booleans»
и ничего не сохраняет, поэтому ошибку лучше находить до публикации.

Проверяется два правила языка выражений Realtime Database:

1. `!` применим только к boolean. `!data...val()` — ошибка, даже если там false,
   потому что `.val()` возвращает значение, а не признак.
2. `data.parent()` — это родитель записываемого узла; второй `.parent()`
   в правилах уже не существует и условие становится заведомо ложным.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

RULES_PATH = Path(__file__).resolve().parent.parent / "database.rules.json"

# ! перед выражением, которое кончается на .val(): там значение, а не boolean.
# Цепочка может содержать child('...'), поэтому между точками допускаем скобки.
BANG_ON_VALUE = re.compile(r"!\s*[^&|!=]*?\.\s*val\s*\(")
DOUBLE_PARENT = re.compile(r"data\s*\.\s*parent\s*\(\s*\)\s*\.\s*parent\s*\(")
BANG_ON_CHILD = re.compile(r"!\s*[A-Za-z_$][\w$]*\s*\.\s*child\s*\(")


def walk(node, path="$"):
    """Обходит дерево правил и отдаёт пары (путь, выражение)"""
    if isinstance(node, dict):
        for key, value in node.items():
            yield from walk(value, f"{path}.{key}")
    elif isinstance(node, str):
        yield path, node


def main() -> int:
    if not RULES_PATH.exists():
        print(f"нет файла {RULES_PATH}")
        return 1

    raw = RULES_PATH.read_text(encoding="utf-8")

    try:
        rules = json.loads(raw)
    except json.JSONDecodeError as error:
        print(f"database.rules.json — невалидный JSON: {error}")
        return 1

    problems: list[str] = []

    for path, expression in walk(rules):
        if BANG_ON_VALUE.search(expression):
            problems.append(
                f"{path}: «!» перед .val() — .val() возвращает значение, а не boolean. "
                "Сравните явно: .val() !== true"
            )

        if BANG_ON_CHILD.search(expression) and "!data.exists()" not in expression:
            problems.append(
                f"{path}: «!» перед .child() — проверка существования делается "
                "через !....exists()"
            )

        if DOUBLE_PARENT.search(expression):
            problems.append(
                f"{path}: data.parent().parent() — второго родителя не существует, "
                "путь строится от root"
            )

    if problems:
        for problem in problems:
            print(f"ОШИБКА  {problem}")
        return 1

    print("database.rules.json: ошибок не найдено")
    return 0


if __name__ == "__main__":
    sys.exit(main())
