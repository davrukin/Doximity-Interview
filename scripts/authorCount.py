import subprocess, re
from collections import defaultdict

authors = defaultdict(int)
coauthors = defaultdict(int)

for line in subprocess.check_output(
    ["git", "shortlog", "-sne", "--all"],
    text=True
).splitlines():
    m = re.match(r"\s*(\d+)\s+(.*)", line)
    if m:
        authors[m.group(2)] = int(m.group(1))

msgs = subprocess.check_output(
    ["git", "log", "--all", "--format=%B"],
    text=True
)

for line in msgs.splitlines():
    m = re.match(r"Co-authored-by:\s*(.*)", line, re.I)
    if m:
        coauthors[m.group(1)] += 1

people = sorted(
    set(authors) | set(coauthors),
    key=lambda p: authors[p] + coauthors[p],
    reverse=True
)

print(f'{"Person":45} {"Author":>8} {"Coauthor":>8}')
for p in people:
    print(f'{p[:45]:45} {authors[p]:8} {coauthors[p]:8}')