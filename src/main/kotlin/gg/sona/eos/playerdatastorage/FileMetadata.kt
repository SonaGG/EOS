package gg.sona.eos.playerdatastorage

class FileMetadata(
    val filename: String,
    val md5: String,
    val fileSizeBytes: Long,
    val lastModifiedTime: Long,
)