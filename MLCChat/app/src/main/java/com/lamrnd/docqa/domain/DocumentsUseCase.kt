package com.lamrnd.docqa.domain

import android.util.Log
import com.lamrnd.docqa.data.Document
import com.lamrnd.docqa.data.DocumentsDB
import com.lamrnd.docqa.domain.readers.Readers
import com.lamrnd.docqa.domain.splitters.WhiteSpaceSplitter
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import setProgressDialogText

@Singleton
class DocumentsUseCase
@Inject
constructor(private val chunksUseCase: ChunksUseCase, private val documentsDB: DocumentsDB) {

    suspend fun addDocument(
        inputStream: InputStream,
        fileName: String,
        documentType: Readers.DocumentType
    ) =
        withContext(Dispatchers.IO) {
            val text =
                Readers.getReaderForDocType(documentType).readFromInputStream(inputStream)
                    ?: return@withContext
            Log.e("APP", "PDF/EMAIL Text: $text")
            val newDocId =
                documentsDB.addDocument(
                    Document(
                        docText = text,
                        docFileName = fileName,
                        docAddedTime = System.currentTimeMillis()
                    )
                )
            Log.d("APP", "New doc id: $newDocId")
            Log.d("APP", "Text length: ${text.length}")
            Log.d("APP", "docFileName: $fileName")

            setProgressDialogText("Creating chunks...")
            //val chunks = WhiteSpaceSplitter.createChunks(text, chunkSize = 500, chunkOverlap = 50)
            val chunks = WhiteSpaceSplitter.createChunks(text, chunkSize = 500, chunkOverlap = 0)
            Log.d("APP", "Chunks size: ${chunks.size}" )
            Log.d("APP", "Chunks: $chunks")

            // 안전하게 chunks의 요소에 접근
                    // 모든 Chunk를 출력
            chunks.forEachIndexed { index, chunk ->
                Log.d("APP", "Chunk[$index]: $chunk")
            }
            /*
            if (chunks.isNotEmpty()) Log.d("APP", "Chunks[0]: ${chunks[0]}")
            if (chunks.size > 1) Log.d("APP", "Chunks[1]: ${chunks[1]}")
            if (chunks.size > 2) Log.d("APP", "Chunks[2]: ${chunks[2]}")
            if (chunks.size > 3) Log.d("APP", "Chunks[3]: ${chunks[3]}")
            if (chunks.size > 4) Log.d("APP", "Chunks[4]: ${chunks[4]}")
             */
            //Log.d("APP", "Chunks[0]: ${chunks[0]}")
            //Log.d("APP", "Chunks[1]: ${chunks[1]}")
            //Log.d("APP", "Chunks[2]: ${chunks[2]}")
            //Log.d("APP", "Chunks[3]: ${chunks[3]}")
            //Log.d("APP", "Chunks[4]: ${chunks[4]}")
            setProgressDialogText("Adding chunks to database...")
            val size = chunks.size
            chunks.forEachIndexed { index, s ->
                setProgressDialogText("Added ${index+1}/${size} chunk(s) to database...")
                chunksUseCase.addChunk(newDocId, fileName, s)
            }
        }

    fun getAllDocuments(): Flow<List<Document>> {
        return documentsDB.getAllDocuments()
    }

    fun removeDocument(docId: Long) {
        documentsDB.removeDocument(docId)
        chunksUseCase.removeChunks(docId)
    }

    fun getDocsCount(): Long {
        return documentsDB.getDocsCount()
    }
}
