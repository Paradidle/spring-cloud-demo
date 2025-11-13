package com.paradiddle.spring.ai.demo.model;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;

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
 * CreateDate:2025/11/6
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/11/6；
 */
@Component
public class DefaultScoringModel implements ScoringModel {

    @Autowired
    private DashScopeRerankModel dashScopeRerankModel;

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> textSegmentList, String query) {

        List<Document> documentList = new ArrayList<>();
        List<String> sortKeys = new ArrayList<>();
        textSegmentList.forEach(textSegment -> {
            documentList.add(new Document(textSegment.text(), textSegment.metadata().toMap()));

            sortKeys.add(DigestUtils.md5Hex(textSegment.text()));
        });

        RerankRequest rerankRequest = new RerankRequest(query, documentList, DashScopeRerankOptions.builder().withTopN(documentList.size()).build());

        RerankResponse rerankResponse = dashScopeRerankModel.call(rerankRequest);
        if(Objects.nonNull(rerankResponse)){

            Map<String, DocumentWithScore> documentMap = rerankResponse.getResults().stream()
                    .collect(Collectors.toMap(
                            document -> DigestUtils.md5Hex(document.getOutput().getText()),
                            document -> document));

            List<Double> collect = sortKeys.stream()
                    .map(key -> documentMap.get(key).getScore())
                    .collect(Collectors.toList());

            return Response.from(collect);
        }

        return null;
    }
}
