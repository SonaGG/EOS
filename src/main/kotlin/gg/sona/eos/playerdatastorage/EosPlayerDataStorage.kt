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
package gg.sona.eos.playerdatastorage

import gg.sona.eos.EosPlatform
import gg.sona.eos.EosResult
import gg.sona.eos.common.ProductUserId
import gg.sona.eos.internal.*
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.util.concurrent.CompletableFuture

/**
 * Player Data Storage interface. Per-user cloud save files.
 */
class EosPlayerDataStorage internal constructor(private val platform: EosPlatform) {

    private fun handle(): Long {
        val fn = Native.downcall(
            "EOS_Platform_GetPlayerDataStorageInterface",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
        )
        return fn.invokeExact(platform.handle) as Long
    }

    fun queryFile(
        localUserId: ProductUserId,
        filename: String,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_PlayerDataStorage_QueryFileCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = PlayerDataStorageQueryFileOptions(localUserId, filename)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_PlayerDataStorage_QueryFile",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun queryFileList(
        localUserId: ProductUserId,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_PlayerDataStorage_QueryFileListCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16, FileCount@24
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = PlayerDataStorageQueryFileListOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_PlayerDataStorage_QueryFileList",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    /**
     * Returns the number of cached file metadata entries, or 0 if the query fails.
     *
     * `EOS_PlayerDataStorage_GetFileMetadataCount` returns an [EosResult] and reports
     * the count through an `int32_t*` out parameter - it is not the return value.
     */
    fun getFileMetadataCount(localUserId: ProductUserId): Int {
        val options = PlayerDataStorageGetFileMetadataCountOptions(localUserId)
        return withCallArena { arena ->
            val seg = options.writeTo(arena)
            val outCount = arena.allocate(ValueLayout.JAVA_INT)
            val result = EosResult.fromValue(
                Native.invoke(
                    "EOS_PlayerDataStorage_GetFileMetadataCount",
                    listOf(handle(), seg, outCount),
                    listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    ValueLayout.JAVA_INT,
                ) as Int
            )
            if (result == EosResult.Success) outCount.get(ValueLayout.JAVA_INT, 0) else 0
        }
    }

    fun copyFileMetadataByFilename(
        localUserId: ProductUserId,
        filename: String,
    ): FileMetadata? = copyMetadata(
        "EOS_PlayerDataStorage_CopyFileMetadataByFilename",
        PlayerDataStorageCopyFileMetadataByFilenameOptions(localUserId, filename)
    )

    fun copyFileMetadataAtIndex(
        localUserId: ProductUserId,
        index: Int,
    ): FileMetadata? = copyMetadata(
        "EOS_PlayerDataStorage_CopyFileMetadataAtIndex",
        PlayerDataStorageCopyFileMetadataAtIndexOptions(localUserId, index)
    )

    private fun copyMetadata(functionName: String, options: StructWriter): FileMetadata? =
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
            val meta = FileMetadata(
                filename = readString(seg, 8),
                md5 = readString(seg, 16),
                fileSizeBytes = seg.getInt32(24).toLong() and 0xffffffffL,
                lastModifiedTime = seg.getInt64(32),
            )
            Native.downcall(
                "EOS_PlayerDataStorage_FileMetadata_Release",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            ).invokeExact(seg)
            meta
        }

    fun deleteFile(
        localUserId: ProductUserId,
        filename: String,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_PlayerDataStorage_DeleteFileCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = PlayerDataStorageDeleteFileOptions(localUserId, filename)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_PlayerDataStorage_DeleteFile",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun duplicateFile(
        localUserId: ProductUserId,
        sourceFilename: String,
        destinationFilename: String,
    ): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_PlayerDataStorage_DuplicateFileCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = PlayerDataStorageDuplicateFileOptions(localUserId, sourceFilename, destinationFilename)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_PlayerDataStorage_DuplicateFile",
                listOf(handle(), seg, MemorySegment.NULL, stub.segment),
                listOf(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            )
        }
        return future
    }

    fun deleteCache(localUserId: ProductUserId? = null): CompletableFuture<EosResult> {
        val future = CompletableFuture<EosResult>()
        // EOS_PlayerDataStorage_DeleteCacheCallbackInfo: ResultCode@0, ClientData@8, LocalUserId@16
        val stub = CallbackStubs.register(EosCallback { data ->
            future.complete(EosResult.fromValue(data.getInt32(0)))
        })
        val options = PlayerDataStorageDeleteCacheOptions(localUserId)
        withCallArena { arena ->
            val seg = options.writeTo(arena)
            Native.invokeVoid(
                "EOS_PlayerDataStorage_DeleteCache",
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
