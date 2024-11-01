package com.lamrnd.docqa.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class Chunk(
    @Id var chunkId: Long = 0,
    @Index var docId: Long = 0,
    var docFileName: String = "",
    var chunkData: String = "",
    @HnswIndex(dimensions = 384) var chunkEmbedding: FloatArray = floatArrayOf()
)

@Entity
data class Document(
    @Id var docId: Long = 0,
    var docText: String = "",
    var docFileName: String = "",
    var docAddedTime: Long = 0,
)

data class RetrievedContext(val fileName: String, val context: String)

data class QueryResult(val response: String, val context: List<RetrievedContext>)

data class OnDeviceQueryResult(
    //val responses: List<String>, // 모든 수집된 결과를 포함하는 리스트
    val responses: String, // 모든 수집된 결과를 포함하는 리스트
    val context: List<RetrievedContext> // 관련 문맥 정보를 담는 리스트
)