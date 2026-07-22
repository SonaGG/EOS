/*
 * Copyright 2026 Sona Softworks LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gg.sona.eos

import java.nio.file.Path

/**
 * Runtime configuration for locating the Epic Online Services (EOS) SDK native binaries.
 */
object EosNatives {
    @Volatile
    @JvmStatic
    var baseUrl: String? =
        System.getProperty("eos.natives.baseUrl")
            ?: System.getenv("EOS_NATIVES_BASE_URL")

    @Volatile
    @JvmStatic
    var cacheDir: Path? =
        System.getProperty("eos.natives.cacheDir")?.let(Path::of)
}
