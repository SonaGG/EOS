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

/**
 * Thrown when an EOS API call returns a failure result code.
 *
 * The original [EosResult] is preserved for inspection. Most EOS APIs return
 * failure codes through their return value or a `ResultCode` field in a
 * callback info struct; this exception is only thrown by binding helpers that
 * prefer to surface failures through Kotlin's exception machinery (such as
 * `runCatching` blocks).
 */
class EosException(
    val result: EosResult,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message ?: "EOS operation failed: $result", cause) {

    constructor(message: String) : this(EosResult.UnexpectedError, message, null)
}
