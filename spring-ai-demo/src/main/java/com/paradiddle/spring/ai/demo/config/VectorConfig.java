package com.paradiddle.spring.ai.demo.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenLanguageModel;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;

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
@Configuration
public class VectorConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    @Value("${spring.ai.vectorstore.chroma.client.host}")
    private String host;
    @Value("${spring.ai.vectorstore.chroma.client.port}")
    private int port;
    @Value("${spring.ai.vectorstore.chroma.collection-name}")
    private String collectionName;


    @Bean
    public ChromaEmbeddingStore chromaEmbeddingStore(){
        return ChromaEmbeddingStore.builder()
                .baseUrl(host+":"+port) // Chroma 服务器的地址
                .collectionName(collectionName) // 自定义集合名称
                .build();
    }

    @Bean
    public QwenEmbeddingModel qwenEmbeddingModel(){
        return new QwenEmbeddingModel.QwenEmbeddingModelBuilder().apiKey(apiKey).build();
    }

    @Bean
    public QwenLanguageModel qwenLanguageModel(){
        return new QwenLanguageModel.QwenLanguageModelBuilder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public QwenChatModel qwenChatModel(){
        return new QwenChatModel.QwenChatModelBuilder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public DashScopeRerankModel dashScopeRerankModel(){
        return new DashScopeRerankModel(new DashScopeApi(apiKey));
    }

}
