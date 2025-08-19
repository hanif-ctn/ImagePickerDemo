package org.hanif.imagepickerdemo

// Add these to your existing SharedImage class or create a new SharedDocument class
data class SharedDocument(
    val name: String,
    val mimeType: String?,
    val data: ByteArray?,
    val size: Long = data?.size?.toLong() ?: 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SharedDocument
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}
