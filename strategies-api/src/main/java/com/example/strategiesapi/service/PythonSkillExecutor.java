package com.example.strategiesapi.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PythonSkillExecutor {

    private static final String SKILLS_DIR = "skills";

    public String executeSkill(String skillName, String prompt) {
        log.info("Executing skill: {}, prompt: {}", skillName, prompt);
        try {
            // 检查Python是否可用
            if (!isPythonAvailable()) {
                log.error("Python is not available. Please install Python and add it to system PATH.");
                return "Error: Python is not available. Please install Python and add it to system PATH.";
            }
            
            // 构建技能文件路径，例如 skills/mx-data/mx_data.py
            String skillPath = SKILLS_DIR + File.separator + skillName + File.separator + skillName.replace("-", "_") + ".py";
            log.info("Looking for skill file at: {}", skillPath);
            
            // 检查文件是否存在
            ClassPathResource resource = new ClassPathResource(skillPath);
            if (!resource.exists()) {
                log.error("Skill file not found: {}", skillPath);
                return "Skill not found: " + skillName;
            }
            log.info("Skill file found: {}", skillPath);
            
            // 创建临时文件
            File tempFile = File.createTempFile(skillName, ".py");
            tempFile.deleteOnExit();
            log.info("Created temporary file: {}", tempFile.getAbsolutePath());
            
            // 将资源文件复制到临时文件
            try (InputStream inputStream = resource.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            log.info("Copied skill file to temporary location");

            // 尝试使用py命令（直接使用py命令，因为测试显示py命令可用）
            // 创建ProcessBuilder并设置环境变量
            ProcessBuilder processBuilder = new ProcessBuilder("py", tempFile.getAbsolutePath(), prompt);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            
            // 继承当前环境变量
            java.util.Map<String, String> env = processBuilder.environment();
            log.info("Setting up environment variables for Python process");
            
            // 不要重定向错误流，分别读取标准输出和标准错误
            // processBuilder.redirectErrorStream(true);
            log.info("Starting Python process: py {} {}", tempFile.getAbsolutePath(), prompt);

            Process process = processBuilder.start();
            log.info("Python process started successfully");

            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // 读取标准输出
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("Python output: {}", line);
                        output.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error reading Python output: {}", e.getMessage());
                }
            });
            
            // 读取标准错误
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error("Python error: {}", line);
                        errorOutput.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error reading Python error: {}", e.getMessage());
                }
            });
            
            // 启动线程
            outputThread.start();
            errorThread.start();

            int exitCode = process.waitFor();
            log.info("Python process exited with code: {}", exitCode);
            if (exitCode != 0) {
                log.error("Python script execution failed with exit code: {}", exitCode);
                log.error("Python error output: {}", errorOutput.toString());
                return "Error executing skill: " + skillName + " (exit code: " + exitCode + ")\n" + errorOutput.toString();
            }
            
            log.info("Python script execution successful");

            String result = output.toString().trim();
            log.info("Skill execution result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error executing Python skill: {}", e.getMessage(), e);
            return "Error executing skill: " + e.getMessage();
        }
    }

    private boolean isPythonAvailable() {
        try {
            // 直接使用py命令，因为测试显示py命令可用
            Process process = new ProcessBuilder("py", "--version").start();
            int exitCode = process.waitFor();
            log.info("Python availability check exit code: {}", exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Error checking Python availability: {}", e.getMessage());
            return false;
        }
    }

}
