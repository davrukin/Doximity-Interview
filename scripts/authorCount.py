import subprocess, re
from collections import defaultdict

# Canonical identity for agents that appeared under multiple emails.
# Prefer noreply addresses; map any alias to the canonical key.
ALIASES = {
    "Gemini CLI <gemini-cli@google.com>": "Gemini CLI <noreply@google.com>",
}

def canonical(name: str) -> str:
    return ALIASES.get(name, name)

authors: defaultdict[str, int] = defaultdict(int)
coauthors: defaultdict[str, int] = defaultdict(int)

for line in subprocess.check_output(
    ["git", "shortlog", "-sne", "--all"],
    text=True,
).splitlines():
    m = re.match(r"\s*(\d+)\s+(.*)", line)
    if m:
        authors[canonical(m.group(2))] += int(m.group(1))

msgs = subprocess.check_output(
    ["git", "log", "--all", "--format=%B"],
    text=True,
)

for line in msgs.splitlines():
    m = re.match(r"Co-authored-by:\s*(.*)", line, re.I)
    if m:
        coauthors[canonical(m.group(1).strip())] += 1

people = sorted(
    set(authors) | set(coauthors),
    key=lambda p: authors[p] + coauthors[p],
    reverse=True,
)

print(f'{"Person":45} {"Author":>8} {"Coauthor":>8} {"Note":}')
for p in people:
    note = ""
    if "Gemini" in p and authors[p] > 0:
        note = "* committed directly (did not follow Co-Authored-By convention)"
    print(f'{p[:45]:45} {authors[p]:8} {coauthors[p]:8}  {note}')

if any("Gemini" in p and authors[p] > 0 for p in people):
    print()
    print("* Gemini CLI can commit directly via its tool and did not always observe the")
    print("  CLAUDE.md convention of listing AI agents in Co-Authored-By trailers only.")
