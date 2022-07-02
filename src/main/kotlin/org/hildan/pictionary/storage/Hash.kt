package org.hildan.pictionary.storage

import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.readBytes

fun Path.hash(): String = readBytes().hash()

fun ByteArray.hash(): String = sha256().encodeBase64()

fun ByteArray.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)

fun ByteArray.encodeBase64(): String = Base64.getEncoder().encodeToString(this)
