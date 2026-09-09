#!/usr/bin/env bash
#
# Copyright (c) 2023 - 2026 Contributors to the Eclipse Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#
#

set -euo pipefail

projectPath=$1
# Normalize project path for gradle (ensure it starts with :)
gradleProjectPath=":${projectPath#:}"

# Normalize folder path (strip leading colon and replace colons with slashes)
folderPath="${projectPath#:}"
folderPath="${folderPath//://}"
folder="build/oss/$folderPath"
fileName="dependencies.txt"

mkdir -p "$folder"

# dependencies may look like the following:
# androidx.compose.ui:ui-test-manifest -> 1.5.0
# org.jetbrains.kotlin:kotlin-stdlib:1.9.0
# androidx.activity:activity:1.2.1 -> 1.7.2 (*)
# androidx.compose.ui:ui:1.5.0 (c)
# androidx.compose.ui:ui-tooling (n)
# androidx.compose.ui:ui-tooling FAILED

# https://github.com/eclipse/dash-licenses#example-gradle

# the following adaptions were done:
# - filter entries marked with (n) = not resolvable
# - filter entries marked FAILED
# - filter entries referencing a (sub-)project
# - change normalization step to be compatible with jetpack compose (androidx.compose.ui:ui-test-manifest -> 1.5.0)

unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     GREP="grep";; # Linux
    Darwin*)
        if command -v ggrep > /dev/null 2>&1; then
            GREP="ggrep"
        else
            GREP="grep"
        fi
        ;;
    *)          GREP="UNKNOWN:${unameOut}"
esac
echo "${GREP}"

deps_output=$(./gradlew "${gradleProjectPath}:dependencies")

echo "$deps_output" \
| ( ${GREP} -Poh "(?<=\-\-\- ).*" || true ) \
| ( ${GREP} -Pv "\([nc\*]\)" || true ) \
| ( ${GREP} -Pv "FAILED" || true ) \
| ( ${GREP} -Pv "project\s*[':]" || true ) \
| perl -pe 's/([\w\.\-]+):([\w\.\-]+):(?:[\w\.\-]+ -> )?([\w\.\-]+).*$/$1:$2:$3/gmi;t' \
| perl -pe 's/([\w\.\-]+):([\w\.\-]+) -> ([\w\.\-]+).*$/$1:$2:$3/gmi;t' \
| sort -u \
> "$folder"/"$fileName"
