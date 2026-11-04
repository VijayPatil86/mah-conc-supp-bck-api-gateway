package com.logging;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LogFolderCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LogFolderCheck.class);

    private final String logRoot;
    private final String app;

    public LogFolderCheck(@Value("${LOG_DIR:C:/Users/Lenovo/logs/mah_conc_supp}") String logRoot,
                          @Value("${spring.application.name}") String app) {
        this.logRoot = logRoot;
        this.app = app;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path dir = Path.of(logRoot, app);
        if (!Files.isDirectory(dir) || !Files.isWritable(dir)) {
            log.error("LOG FOLDER NOT WRITABLE: {} . Log files are NOT being written.", dir);
            // for production, throw new IllegalStateException(...) to refuse to start instead
        } else {
            log.info("Log files are written to {}", dir);
        }
    }
}