package gg.sona.eos.playerdatastorage

public class FileMetadata(
    public val filename: String,
    public val md5: String,
    public val fileSizeBytes: Long,
    public val lastModifiedTime: Long,
)