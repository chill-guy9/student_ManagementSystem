package com.sms.nexus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.nexus.common.enums.AdminRole;
import com.sms.nexus.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShellHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private static final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private static final int MAX_CONCURRENT_PROCESSES = 50;

    // Whitelist of allowed commands (Linux)
    private static final Set<String> ALLOWED_COMMANDS_LINUX = Set.of(
            "ls", "dir", "cat", "head", "tail", "grep", "find", "pwd",
            "echo", "whoami", "date", "df", "du", "free", "top",
            "ps", "netstat", "ifconfig", "ip", "uname", "uptime",
            "java", "javac", "mvn", "git", "docker", "mysql",
            "systemctl", "journalctl", "tailf", "clear"
    );

    // Whitelist of allowed commands (Windows)
    private static final Set<String> ALLOWED_COMMANDS_WIN = Set.of(
            "dir", "type", "whoami", "date", "echo", "hostname",
            "netstat", "ipconfig", "systeminfo", "tasklist",
            "java", "javac", "mvn", "git", "docker", "mysql",
            "cd", "ver", "tree", "where", "find", "findstr",
            "chcp", "set", "cls", "clear", "wmic"
    );

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    // Dangerous patterns that are always blocked
    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
            Pattern.compile("(?i)rm\\s+(-rf?|-fr?|-r\\s+-f|-f\\s+-r)\\s+/"),
            Pattern.compile("(?i)sudo\\s+"),
            Pattern.compile("(?i)chmod\\s+777"),
            Pattern.compile("(?i)\\bformat\\s"),       // only block 'format' as a command, not --format flag
            Pattern.compile("(?i)\\bdd\\s+if="),
            Pattern.compile("(?i)\\bshutdown\\b"),
            Pattern.compile("(?i)\\breboot\\b"),
            Pattern.compile("(?i)\\binit\\s+[06]"),
            Pattern.compile("(?i):\\(\\)\\s*\\{"),  // fork bomb
            Pattern.compile("(?i)\\bmkfs\\b"),
            Pattern.compile("(?i)\\bcrontab\\b"),
            Pattern.compile("(?i)\\bpasswd\\b"),
            Pattern.compile("(?i)\\buseradd\\b"),
            Pattern.compile("(?i)\\buserdel\\b"),
            // Shell metacharacter injection prevention
            Pattern.compile("[;|&`$]"),     // command chaining/injection
            Pattern.compile("\\$\\("),       // command substitution
            Pattern.compile("\\{,"),         // brace expansion
            Pattern.compile("~"),            // home directory expansion
            Pattern.compile("\\n"),          // newline injection
            Pattern.compile(">|>>"),         // output redirection
            Pattern.compile("<"),            // input redirection
            Pattern.compile("\\^"),          // Windows cmd.exe escape character
            Pattern.compile("%[^%]*%"),      // Windows environment variable expansion
            // Sensitive file access prevention
            Pattern.compile("(?i)/etc/(shadow|passwd|ssh)"),
            Pattern.compile("(?i)\\.env\\b"),
            Pattern.compile("(?i)application\\.(yml|yaml|properties)"),
            Pattern.compile("(?i)/root/"),
            Pattern.compile("(?i)id_rsa"),
            Pattern.compile("(?i)\\.ssh/")
    );

    // Commands requiring super_admin
    private static final Set<String> RESTRICTED_COMMANDS = Set.of(
            "systemctl", "journalctl", "docker", "netstat", "ifconfig", "ip",
            "wmic", "tasklist", "ipconfig", "systeminfo"
    );

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String adminId = (String) session.getAttributes().get("adminId");
        log.info("Shell WebSocket connected: adminId={}", adminId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String type = (String) msg.get("type");

        switch (type) {
            case "shell.input" -> handleShellInput(session, msg);
            case "shell.interrupt" -> handleShellInterrupt(session);
            case "ping" -> sendToSession(session, Map.of("type", "pong", "data", System.currentTimeMillis()));
            default -> sendToSession(session, Map.of("type", "error", "data", "Unknown message type: " + type));
        }
    }

    private void handleShellInterrupt(WebSocketSession session) {
        Process process = activeProcesses.get(session.getId());
        if (process != null && process.isAlive()) {
            killProcessTree(process);
            activeProcesses.remove(session.getId());
            sendToSession(session, Map.of("type", "shell.output", "data", "^C"));
            sendToSession(session, Map.of("type", "shell.exit", "data", 130));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        Process process = activeProcesses.remove(sessionId);
        if (process != null && process.isAlive()) {
            killProcessTree(process);
        }
        String adminId = (String) session.getAttributes().get("adminId");
        log.info("Shell WebSocket disconnected: adminId={}, status={}", adminId, status);
    }

    /**
     * Kill a process and its children. On Windows, cmd.exe may spawn child processes
     * that survive destroyForcibly() on the parent. Use taskkill /T /F to kill the tree.
     */
    private void killProcessTree(Process process) {
        if (process.isAlive()) {
            if (IS_WINDOWS) {
                try {
                    long pid = process.pid();
                    new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid))
                            .inheritIO().start().waitFor();
                } catch (Exception e) {
                    log.warn("Failed to kill process tree, falling back to destroyForcibly", e);
                    process.destroyForcibly();
                }
            } else {
                process.destroyForcibly();
            }
        }
    }

    private void handleShellInput(WebSocketSession session, Map<String, Object> msg) {
        String command = (String) msg.get("data");
        if (command == null || command.isBlank()) {
            sendToSession(session, Map.of("type", "shell.output", "data", "Empty command"));
            return;
        }

        command = command.trim();

        // Check blocked patterns
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(command).find()) {
                sendToSession(session, Map.of("type", "shell.output", "data", "Command blocked: dangerous pattern detected"));
                sendToSession(session, Map.of("type", "shell.exit", "data", 1));
                return;
            }
        }

        // Extract base command
        String baseCommand = command.split("\\s+")[0];
        String baseCommandName = baseCommand.contains("/") ?
                baseCommand.substring(baseCommand.lastIndexOf("/") + 1) : baseCommand;

        // Check if command is in whitelist
        Set<String> allowedCommands = IS_WINDOWS ? ALLOWED_COMMANDS_WIN : ALLOWED_COMMANDS_LINUX;
        if (!allowedCommands.contains(baseCommandName)) {
            sendToSession(session, Map.of("type", "shell.output", "data",
                    "Command not allowed: " + baseCommandName + ". Allowed: " + allowedCommands));
            sendToSession(session, Map.of("type", "shell.exit", "data", 1));
            return;
        }

        // Check restricted commands
        if (RESTRICTED_COMMANDS.contains(baseCommandName)) {
            String role = (String) session.getAttributes().get("role");
            if (!AdminRole.SUPER_ADMIN.getValue().equals(role)) {
                sendToSession(session, Map.of("type", "shell.output", "data",
                        "Command requires super_admin role: " + baseCommandName));
                sendToSession(session, Map.of("type", "shell.exit", "data", 1));
                return;
            }
        }

        // Execute command using ProcessBuilder with argument array (no shell invocation)
        try {
            // Limit concurrent processes
            if (activeProcesses.size() >= MAX_CONCURRENT_PROCESSES) {
                sendToSession(session, Map.of("type", "shell.output", "data",
                        "Too many concurrent processes. Please wait and try again."));
                sendToSession(session, Map.of("type", "shell.exit", "data", 1));
                return;
            }

            // Only one active process per session
            Process existingProcess = activeProcesses.get(session.getId());
            if (existingProcess != null && existingProcess.isAlive()) {
                sendToSession(session, Map.of("type", "shell.output", "data",
                        "A command is already running. Please wait for it to complete."));
                sendToSession(session, Map.of("type", "shell.exit", "data", 1));
                return;
            }

            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new java.io.File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);

            // Parse command into arguments and execute directly without shell
            String[] args = command.split("\\s+");
            if (IS_WINDOWS) {
                // On Windows, use cmd.exe /c with chcp 65001 for UTF-8 output.
                // cmd.exe /c concatenates everything after /c as a single command string.
                // Blocked patterns above already prevent dangerous metacharacters from user input.
                List<String> cmdList = new ArrayList<>();
                cmdList.add("cmd.exe");
                cmdList.add("/c");
                cmdList.add("chcp 65001 >nul &");
                cmdList.addAll(List.of(args));
                pb.command(cmdList);
            } else {
                // On Unix, execute directly without shell to prevent command injection
                pb.command(List.of(args));
            }

            Process process = pb.start();
            activeProcesses.put(session.getId(), process);

            // Read output in a separate thread
            Thread outputThread = new Thread(() -> {
                // Use UTF-8 charset for reading process output (chcp 65001 ensures UTF-8 on Windows)
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sendToSession(session, Map.of("type", "shell.output", "data", line));
                    }
                } catch (IOException e) {
                    log.error("Error reading shell output", e);
                }
            });
            outputThread.setDaemon(true);
            outputThread.start();

            // Wait for process in a separate thread
            Thread exitThread = new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    sendToSession(session, Map.of("type", "shell.exit", "data", exitCode));
                    activeProcesses.remove(session.getId());
                } catch (InterruptedException e) {
                    log.error("Shell process interrupted", e);
                }
            });
            exitThread.setDaemon(true);
            exitThread.start();

        } catch (IOException e) {
            sendToSession(session, Map.of("type", "shell.output", "data", "Error: " + e.getMessage()));
            sendToSession(session, Map.of("type", "shell.exit", "data", 1));
        }
    }

    private void sendToSession(WebSocketSession session, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("Failed to send WebSocket message", e);
        }
    }
}
