#
# Copyright (c) 2023 Contributors to the Eclipse Foundation
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

projectName=$1
folder=build/oss/"$projectName"
fileName=dependencies.txt

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
    Darwin*)    GREP="ggrep";; # Mac
    *)          GREP="UNKNOWN:${unameOut}"
esac
echo "${GREP}"

./gradlew "$projectName":dependencies \
| ${GREP} -Poh "(?<=\-\-\- ).*" \
| ${GREP} -Pv "\([nc\*]\)" \
| ${GREP} -Pv "FAILED" \
| ${GREP} -Pv "project :[a-zA-Z0-9]+" \
| perl -pe 's/([\w\.\-]+):([\w\.\-]+):(?:[\w\.\-]+ -> )?([\w\.\-]+).*$/$1:$2:$3/gmi;t' \
| perl -pe 's/([\w\.\-]+):([\w\.\-]+) -> ([\w\.\-]+).*$/$1:$2:$3/gmi;t' \
| sort -u \
> "$folder"/"$fileName"
