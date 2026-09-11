#!/usr/bin/env bash
#
# Release procedure for simple-spel-shell.
#
# Run from the repository root, with "master" checked out and a clean
# working directory. See project conventions in git history (e.g. commits
# 94a9c888 and 81d71da0) for the version-bumping pattern this follows.

#not reviewed
exit 1

set -Eeuo pipefail

trap 'echo "release.sh: failed at line ${LINENO}" >&2' ERR

REMOTE="origin"

for cmd in git mvn grep sed; do
    command -v "$cmd" >/dev/null 2>&1 || { echo "ERROR: required command '$cmd' not found" >&2; exit 1; }
done

# Replace the single occurrence of $old with $new in $file, aborting if the
# occurrence count isn't exactly one (safety net against unexpected files).
replace_version() {
    local file="$1" old="$2" new="$3" count old_escaped
    count=$(grep -Fc -- "$old" "$file")
    if [[ "$count" -ne 1 ]]; then
        echo "ERROR: expected exactly one occurrence of '$old' in $file, found $count" >&2
        exit 1
    fi
    old_escaped=$(printf '%s' "$old" | sed 's/\./\\./g')
    sed -i "s/${old_escaped}/${new}/" "$file"
}

# --- Step 1: master must be checked out, clean, and in sync with origin ---

current_branch=$(git rev-parse --abbrev-ref HEAD)
if [[ "$current_branch" != "master" ]]; then
    echo "ERROR: must be on branch 'master' (currently on '${current_branch}')" >&2
    exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
    echo "ERROR: working directory is not clean" >&2
    git status --short >&2
    exit 1
fi

echo "Fetching ${REMOTE}..."
git fetch "$REMOTE" master

local_sha=$(git rev-parse master)
remote_sha=$(git rev-parse "${REMOTE}/master")
if [[ "$local_sha" != "$remote_sha" ]]; then
    echo "ERROR: local master (${local_sha}) differs from ${REMOTE}/master (${remote_sha})." >&2
    echo "       Pull/push to bring master in sync before releasing." >&2
    exit 1
fi

# --- Step 2: determine PROJECT_VERSION from pom.xml, minus -SNAPSHOT ---

snapshot_version=$(grep -m1 -oE '<version>[^<]+</version>' pom.xml | sed -E 's#</?version>##g')
if [[ "$snapshot_version" != *-SNAPSHOT ]]; then
    echo "ERROR: pom.xml project version '${snapshot_version}' does not end in -SNAPSHOT" >&2
    exit 1
fi
PROJECT_VERSION="${snapshot_version%-SNAPSHOT}"

version_parts=(${PROJECT_VERSION//./ })
last_idx=$(( ${#version_parts[@]} - 1 ))
if ! [[ "${version_parts[$last_idx]}" =~ ^[0-9]+$ ]]; then
    echo "ERROR: last version component of '${PROJECT_VERSION}' is not numeric, can't bump it" >&2
    exit 1
fi

echo "Releasing version: ${PROJECT_VERSION}"

release_branch="release/v-${PROJECT_VERSION}"
release_tag="v-${PROJECT_VERSION}"

if git show-ref --verify --quiet "refs/heads/${release_branch}" \
        || git show-ref --verify --quiet "refs/remotes/${REMOTE}/${release_branch}"; then
    echo "ERROR: branch '${release_branch}' already exists locally or on ${REMOTE}" >&2
    exit 1
fi
if git rev-parse -q --verify "refs/tags/${release_tag}" >/dev/null; then
    echo "ERROR: tag '${release_tag}' already exists" >&2
    exit 1
fi

# --- Step 3: create and checkout the release branch ---

git checkout -b "$release_branch"

# --- Step 4: strip -SNAPSHOT from the version in pom.xml and README.md ---

replace_version pom.xml "$snapshot_version" "$PROJECT_VERSION"
replace_version README.md "$snapshot_version" "$PROJECT_VERSION"

# --- Step 5: build ---

echo "Building..."
mvn clean package

# --- Step 6 & 7: commit and tag ---

git add pom.xml README.md
git commit -m "Release ${PROJECT_VERSION}"
git tag "$release_tag"

# --- Step 8: pause for manual verification ---

read -r -p "Ready to push a new release branch to git. Please verify changes and press Enter to proceed."

# --- Step 9: push branch and tag ---

git push "$REMOTE" "$release_branch"
git push "$REMOTE" "$release_tag"

# --- Step 10: switch back to master ---

git checkout master

# --- Step 11: bump to the next development version, leave unstaged ---

last_part="${version_parts[$last_idx]}"
version_parts[$last_idx]=$(( last_part + 1 ))
next_version=$(IFS=.; echo "${version_parts[*]}")
next_snapshot_version="${next_version}-SNAPSHOT"

replace_version pom.xml "$snapshot_version" "$next_snapshot_version"
replace_version README.md "$snapshot_version" "$next_snapshot_version"

# --- Step 12: done, leave pom.xml/README.md unstaged for manual commit ---

echo "Released ${PROJECT_VERSION} (branch ${release_branch}, tag ${release_tag})."
echo "master now has an uncommitted version bump to ${next_snapshot_version}. Review and commit it yourself."
