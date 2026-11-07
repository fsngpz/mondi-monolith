package com.mondi.machine.storage

import org.apache.commons.io.FilenameUtils
import org.springframework.web.multipart.MultipartFile

/**
 * the content types accepted for uploaded documents such as payment proofs and certificates.
 */
val DOCUMENT_CONTENT_TYPES = setOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/heic",
    "application/pdf"
)

/**
 * an extension function to validate that an uploaded file is a non-empty image or PDF.
 *
 * @author Ferdinand Sangap
 * @since 2026-11-07
 * @param fieldName the name of the field, used in the error message.
 */
fun MultipartFile.requireDocument(fieldName: String = "file") {
    require(!this.isEmpty) {
        "field '$fieldName' cannot be empty"
    }
    require(this.contentType?.lowercase() in DOCUMENT_CONTENT_TYPES) {
        "field '$fieldName' must be an image (JPEG, PNG, WEBP, HEIC) or a PDF"
    }
}

/**
 * an extension function to get the lowercase file extension of an uploaded file.
 *
 * @return the extension, or "bin" when the file name has none.
 */
fun MultipartFile.extensionOrDefault(): String {
    return FilenameUtils.getExtension(this.originalFilename)
        ?.lowercase()
        ?.filter { it.isLetterOrDigit() }
        ?.ifBlank { null }
        ?: "bin"
}
