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
package gg.sona.eos.titlestorage

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Title Storage interface. Game-wide shared cloud files (not per-user).
 */
class EosTitleStorage internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetTitleStorageInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    fun queryFile(
        localUserId: EpicAccountId,
        filename: String,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_TitleStorage_QueryFileCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = TitleStorageQueryFileOptions(localUserId, filename)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_TitleStorage_QueryFile",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun queryFileList(localUserId: EpicAccountId): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_TitleStorage_QueryFileListCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, FileCount@24
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = TitleStorageQueryFileListOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_TitleStorage_QueryFileList",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun getFileMetadataCount(localUserId: EpicAccountId): Int {
        val options = TitleStorageGetFileMetadataCountOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invoke(
                "EOS_TitleStorage_GetFileMetadataCount",
                listOf(handle(), seg),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                ValueLayout.JAVA_INT,
            ) as Int
        }
    }

    fun copyFileMetadataByFilename(
        localUserId: EpicAccountId,
        filename: String,
    ): TitleStorageFileMetadata? = copyMetadata(
        "EOS_TitleStorage_CopyFileMetadataByFilename",
        TitleStorageCopyFileMetadataByFilenameOptions(localUserId, filename)
    )

    fun copyFileMetadataAtIndex(
        localUserId: EpicAccountId,
        index: Int,
    ): TitleStorageFileMetadata? = copyMetadata(
        "EOS_TitleStorage_CopyFileMetadataAtIndex",
        TitleStorageCopyFileMetadataAtIndexOptions(localUserId, index)
    )

    private fun copyMetadata(functionName: String, options: StructWriter): TitleStorageFileMetadata? =
        withCallArena { arena ->
            val outPtr = arena.allocate(ValueLayout.ADDRESS)
            val result = EosResult.fromValue(
                Native.invoke(
                    functionName,
                    listOf(handle(), options.writeTo(arena), outPtr),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result != EosResult.Success) return@withCallArena null
            val seg = outPtr.get(ValueLayout.ADDRESS, 0)
            if (seg.address() == 0L) return@withCallArena null
            val meta = TitleStorageFileMetadata(
                filename = readString(seg, 8),
                md5 = readString(seg, 16),
                fileSizeBytes = seg.getInt32(24).toLong() and 0xffffffffL,
                lastModifiedTime = seg.getInt64(32),
            )
            Native.downcall(
                "EOS_TitleStorage_FileMetadata_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            meta
        }

    fun deleteCache(localUserId: EpicAccountId? = null): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_TitleStorage_DeleteCacheCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = TitleStorageDeleteCacheOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_TitleStorage_DeleteCache",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    private fun readString(seg: MemorySegment, offset: Long): String {
        val ptr = seg.get(ValueLayout.ADDRESS, offset)
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(Long.MAX_VALUE).getString(0)
    }
}
