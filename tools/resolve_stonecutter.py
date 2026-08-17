#!/usr/bin/env python3
"""
Resolve TaCZ Tweaks Stonecutter `//?` comment conditionals to a single target
config (loader=fabric, minecraft=26.2) and flatten each file to its active
branches only. Inactive branches are removed; `//?` directives are removed.

Usage: python3 tools/resolve_stonecutter.py [--write] FILE...
"""
import re
import sys

# Target config
LOADER = "fabric"
VERSION = "26.2"

# version predicate truth table for 26.2
def ver(tok: str) -> bool:
    if tok in ("1.20.1", "1.21.1", "1.20", "1.21"):
        return False
    if tok == "<1.20.5" or tok == "<1.20.2" or tok == "<1.21" or tok == "<1.21.1":
        return False
    if tok == ">=1.20.5" or tok == ">=1.21" or tok == ">=1.21.1":
        return True
    raise ValueError(f"unknown version token {tok}")

def atom(tok: str) -> bool:
    t = tok.strip()
    if t == "fabric": return LOADER == "fabric"
    if t == "forge": return LOADER == "forge"
    if t == "neoforge": return LOADER == "neoforge"
    if t.startswith("<") or t.startswith(">="):
        return ver(t)
    if re.fullmatch(r"\d+\.\d+(\.\d+)?", t):
        return ver(t)
    raise ValueError(f"unknown atom {t}")

def eval_cond(expr: str) -> bool:
    # expr is like "forge || fabric", "1.20.1 && forge",
    # "(fabric && 1.21.1) || neoforge"
    # Replace atoms with True/False then eval safely.
    if not expr.strip():
        return True
    s = expr.strip()
    # tokenize by operators and parens, respecting atoms
    tokens = re.findall(r"\(|\)|&&|\|\||[^()&|]+", s)
    out = []
    for tk in tokens:
        st = tk.strip()
        if st == "":
            continue
        out.append(st)
    # build a python-evaluable string
    mapped = []
    for tk in out:
        if tk in ("(", ")", "&&", "||"):
            mapped.append(tk)
        else:
            mapped.append("True" if atom(tk) else "False")
    code = " ".join(mapped).replace("&&", "and").replace("||", "or")
    return bool(eval(code, {"__builtins__": {}}))

# ---- directive detection ----
# Returns one of:
#   ('if', cond), ('elseif', cond), ('else',), ('end',)
# or None if not a directive.
DIR_IF = re.compile(r"^//\?[\s]?if\s+(.+?)\s*\{?\s*$")
DIR_ELSEIF = re.compile(r"^//\?\}\s*else\s+if\s+(.+?)\s*\{?\s*$")
DIR_ELSE = re.compile(r"^//\?\}\s*else\s*\{?\s*$")
DIR_END = re.compile(r"^//\?\}\s*$")

def detect(line: str):
    # strip a leading '*/' that closes a wrapper (glued to directive)
    s = line.strip()
    s2 = s
    if s2.startswith("*/"):
        s2 = s2[2:].lstrip()
    m = DIR_IF.match(s2)
    if m: return ("if", m.group(1).strip())
    m = DIR_ELSEIF.match(s2)
    if m: return ("elseif", m.group(1).strip())
    m = DIR_ELSE.match(s2)
    if m: return ("else",)
    m = DIR_END.match(s2)
    if m: return ("end",)
    return None

# A "content line" may carry a leading wrapper marker '/*' (open) glued to it.
def strip_open_marker(line: str):
    s = line.lstrip()
    if s.startswith("/*"):
        return line.replace("/*", "", 1)
    return line

# Recursive parser producing a list of items.
# item = ('text', line) | ('chain', [branch,...])
# branch = (cond_or_None, [items])  cond None == else
def parse(lines):
    pos = [0]
    def parse_block(top=False):
        items = []
        while pos[0] < len(lines):
            line = lines[pos[0]]
            d = detect(line)
            if d is None:
                items.append(("text", strip_open_marker(line)))
                pos[0] += 1
                continue
            if d[0] == "end":
                pos[0] += 1
                return items
            if d[0] in ("else", "elseif"):
                if top:
                    # shouldn't happen
                    return items
                return items  # let chain handler take over
            if d[0] == "if":
                # consume the opening directive, then parse the chain body
                pos[0] += 1
                chain = parse_chain(d[1])
                items.append(("chain", chain))
                continue
        return items

    def parse_chain(first_cond):
        branches = []
        cur_cond = first_cond
        cur_items = None
        while pos[0] < len(lines):
            line = lines[pos[0]]
            d = detect(line)
            if d is None:
                if cur_items is None:
                    cur_items = []
                cur_items.append(("text", strip_open_marker(line)))
                pos[0] += 1
                continue
            if d[0] == "if":
                # nested chain inside current branch
                pos[0] += 1
                nested = parse_chain(d[1])
                if cur_items is None: cur_items = []
                cur_items.append(("chain", nested))
                continue
            if d[0] in ("elseif", "else"):
                # close current branch
                branches.append((cur_cond, cur_items or []))
                if d[0] == "elseif":
                    cur_cond = d[1]
                else:
                    cur_cond = None
                cur_items = None
                pos[0] += 1
                continue
            if d[0] == "end":
                branches.append((cur_cond, cur_items or []))
                pos[0] += 1
                return branches
        branches.append((cur_cond, cur_items or []))
        return branches

    items = parse_block(top=True)
    return items

def resolve(items, out):
    for it in items:
        if it[0] == "text":
            out.append(it[1])
        else:  # chain
            chosen = None
            for (cond, body) in it[1]:
                if cond is None:
                    chosen = body
                    break
                if eval_cond(cond):
                    chosen = body
                    break
            if chosen is None:
                chosen = []
            resolve(chosen, out)

def process(path, write):
    with open(path, "r", encoding="utf-8") as f:
        src = f.read()
    # Preserve newline style
    newline = "\r\n" if "\r\n" in src else "\n"
    raw_lines = src.splitlines()
    if not raw_lines and src.endswith("\n"):
        raw_lines = [""]
    items = parse(raw_lines)
    out = []
    resolve(items, out)
    result = newline.join(out) + newline
    if src != result:
        if write:
            with open(path, "w", encoding="utf-8", newline="") as f:
                f.write(result)
            print(f"UPDATED {path}")
        else:
            print(f"CHANGED {path}")
    else:
        print(f"unchanged {path}")

if __name__ == "__main__":
    write = "--write" in sys.argv
    files = [a for a in sys.argv[1:] if not a.startswith("--")]
    for f in files:
        process(f, write)
