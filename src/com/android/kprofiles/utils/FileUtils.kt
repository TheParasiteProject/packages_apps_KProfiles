/*
 * SPDX-FileCopyrightText: The CyanogenMod Project
 * SPDX-FileCopyrightText: TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.kprofiles.utils

import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object FileUtils {
    private const val TAG: String = "FileUtils"

    /**
     * Reads the first line of text from the given file. Reference {@link BufferedReader#readLine()}
     * for clarification on what a line is
     *
     * @return the read line contents, or null on failure
     */
    fun readOneLine(fileName: String): String? =
        try {
            File(fileName).bufferedReader(bufferSize = 512).use { reader -> reader.readLine() }
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file ${fileName} for reading", e)
            null
        } catch (e: IOException) {
            Log.e(TAG, "Could not read from file ${fileName}", e)
            null
        }

    /**
     * Writes the given value into the given file
     *
     * @return true on success, false on failure
     */
    fun writeLine(fileName: String, value: String): Boolean =
        try {
            File(fileName).writeText(value)
            true
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file ${fileName} for writing", e)
            false
        } catch (e: IOException) {
            Log.e(TAG, "Could not write to file ${fileName}", e)
            false
        }

    /**
     * Checks whether the given file exists
     *
     * @return true if exists, false if not
     */
    fun fileExists(fileName: String): Boolean = File(fileName).exists()

    /**
     * Checks whether the given file is readable
     *
     * @return true if readable, false if not
     */
    fun isFileReadable(fileName: String): Boolean = File(fileName).canRead()

    /**
     * Checks whether the given file is writable
     *
     * @return true if writable, false if not
     */
    fun isFileWritable(fileName: String): Boolean = File(fileName).canWrite()

    /**
     * Deletes an existing file
     *
     * @return true if the delete was successful, false if not
     */
    fun delete(fileName: String): Boolean {
        val file = File(fileName)
        return try {
            file.delete()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException trying to delete ${fileName}", e)
            false
        }
    }

    /**
     * Renames an existing file
     *
     * @return true if the rename was successful, false if not
     */
    fun rename(srcPath: String, dstPath: String): Boolean =
        try {
            File(srcPath).renameTo(File(dstPath))
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException trying to rename ${srcPath} to ${dstPath}", e)
            false
        } catch (e: NullPointerException) {
            Log.e(TAG, "NullPointerException trying to rename ${srcPath} to ${dstPath}", e)
            false
        }
}
