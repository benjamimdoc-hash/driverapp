package com.driverapp.leitura

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Lê o texto de uma imagem (ex.: um print de oferta) direto no celular, sem internet.
 * Usado para TESTAR a leitura com prints de referência, antes de usar com as ofertas reais.
 * Devolve as linhas de cima para baixo, da esquerda para a direita.
 */
object LeituraDeImagem {
    suspend fun linhas(context: Context, uri: Uri): List<String> = suspendCancellableCoroutine { cont ->
        val imagem = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            cont.resumeWithException(e)
            return@suspendCancellableCoroutine
        }
        val leitor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        leitor.process(imagem)
            .addOnSuccessListener { resultado ->
                val linhas = resultado.textBlocks
                    .flatMap { it.lines }
                    .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
                    .map { it.text.trim() }
                    .filter { it.isNotEmpty() }
                leitor.close()
                cont.resume(linhas)
            }
            .addOnFailureListener { e ->
                leitor.close()
                cont.resumeWithException(e)
            }
    }
}
