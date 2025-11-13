package com.paradiddle.spring.ai.demo.contorller;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.paradiddle.spring.ai.demo.model.DefaultScoringModel;
import com.paradiddle.spring.ai.demo.service.ImportVectorService;

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
 * CreateDate:2025/10/23
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/10/23；
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ImportVectorService importVectorService;

    @Autowired
    private ChromaEmbeddingStore chromaEmbeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private QwenChatModel qwenChatModel;

    @Autowired
    private DefaultScoringModel defaultScoringModel;

    @PostMapping("/import")
    public ResponseEntity<String> importVector(){
        importVectorService.importVector();
        return ResponseEntity.ok("success");
    }

    @GetMapping("/get")
    public ResponseEntity<String> get(@RequestParam("question") String question) {

        ReRankingContentAggregator reRankingContentAggregator =
                ReRankingContentAggregator.builder()
                        .scoringModel(defaultScoringModel)
                        .maxResults(3).build();

        // 3. 创建检索器
        EmbeddingStoreContentRetriever embeddingStoreContentRetriever =
                new EmbeddingStoreContentRetriever(chromaEmbeddingStore, embeddingModel, 10, 0.5d);

        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentAggregator(reRankingContentAggregator)
                .contentRetriever(embeddingStoreContentRetriever).build();


        // 4. 创建具备记忆功能的对话检索链
        ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
                .chatLanguageModel(qwenChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();


        return ResponseEntity.ok(chain.execute(question));
    }

}
