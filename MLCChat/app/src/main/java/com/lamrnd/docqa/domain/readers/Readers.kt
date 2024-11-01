package com.lamrnd.docqa.domain.readers

class Readers {

    enum class DocumentType {
        PDF,
        MS_DOCX,
        EMAIL // EMAIL 타입 추가
    }

    companion object {

        fun getReaderForDocType(docType: DocumentType): Reader {
            return when (docType) {
                DocumentType.PDF -> PDFReader()
                DocumentType.MS_DOCX -> DOCXReader()
                DocumentType.EMAIL -> EMAILReader() // 이메일 리더 추가
            }
        }
    }
}
