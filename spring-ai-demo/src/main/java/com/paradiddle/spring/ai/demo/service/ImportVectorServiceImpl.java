package com.paradiddle.spring.ai.demo.service;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * Copyright: 2025 . All rights reserved.
 * </p>
 * <p>
 * Company: Zsoft
 * </p>
 * <p>
 * CreateDate:2025/10/22
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/10/22；
 */
@Service
public class ImportVectorServiceImpl implements ImportVectorService{


    @Autowired
    private ChromaEmbeddingStore chromaEmbeddingStore;

    @Autowired
    private QwenEmbeddingModel qwenEmbeddingModel;


    @Override
    public void importVector() {
        try {

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingStore(chromaEmbeddingStore)
                    .embeddingModel(qwenEmbeddingModel)
                    .documentSplitter(DocumentSplitters.recursive(500, 100))
                    .build();

            List<Document> documentList = ClassPathDocumentLoader.loadDocuments("documents");
            ingestor.ingest(documentList);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void importVectorBak() {
        try {

//            TextSplitter textSplitter = new SentenceSplitter();
//
//            PDDocument document = PDDocument.load(new File("C:\\Users\\Paradiddle\\Desktop\\_20221130：初识Docker-20221130：初识Docker(1).pdf"));
//            PDFTextStripper stripper = new PDFTextStripper();
//            String fullText = stripper.getText(document);
//            // 将长文本按语义分割成块（Chunking）
//            List<Document> textChunks = textSplitter.split(new Document(fullText));
//
//            vectorStore.add(textChunks);
//
//            document.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }


}
