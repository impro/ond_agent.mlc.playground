package com.lamrnd.docqa.domain.readers

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import java.io.InputStream

class EMAILReader : Reader() {
    override fun readFromInputStream(inputStream: InputStream): String? {
        // 이메일 본문을 읽는 로직 구현
        // 예시로 MIME 형식으로 이메일을 파싱하는 등의 작업 수행
        return parseEmailContent(inputStream)
    }

    private fun parseEmailContent(inputStream: InputStream): String {
        // 이메일 파싱 로직을 구현합니다.
        // 이메일 헤더, 본문, 첨부 파일 등을 처리할 수 있습니다.
        // 여기서는 간단하게 본문만 반환하는 로직을 가정합니다.
        val emailContent = String(inputStream.readBytes())
        return emailContent
    }
}