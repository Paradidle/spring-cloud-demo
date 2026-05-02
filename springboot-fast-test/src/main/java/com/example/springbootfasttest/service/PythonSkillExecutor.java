package com.example.springbootfasttest.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PythonSkillExecutor {

    @Value("${python.skill.path:./skills}")
    private String skillPath;

    private final Map<String, String> skillScriptCache = new ConcurrentHashMap<>();

    public JSONObject executeSkill(String skillName, Map<String, Object> params) {
        try {
            String scriptPath = getSkillScript(skillName);
            
            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            String jsonInput = new JSONObject(params).toString();
            process.getOutputStream().write(jsonInput.getBytes("UTF-8"));
            process.getOutputStream().flush();
            process.getOutputStream().close();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return new JSONObject(output.toString());
            } else {
                log.error("Python脚本执行失败, exitCode: {}, output: {}", exitCode, output);
                throw new RuntimeException("Skill执行失败: " + skillName);
            }
        } catch (Exception e) {
            log.error("执行Python skill异常: {}", skillName, e);
            throw new RuntimeException("Skill执行异常: " + e.getMessage());
        }
    }

    private String getSkillScript(String skillName) {
        return skillScriptCache.computeIfAbsent(skillName, name -> {
            String scriptFile = skillPath + File.separator + name + ".py";
            if (!FileUtil.exist(scriptFile)) {
                throw new RuntimeException("Skill脚本不存在: " + scriptFile);
            }
            return scriptFile;
        });
    }
}
