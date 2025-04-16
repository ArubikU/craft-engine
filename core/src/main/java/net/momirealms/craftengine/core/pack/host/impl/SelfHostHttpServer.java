package net.momirealms.craftengine.core.pack.host.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class SelfHostHttpServer {
    private static SelfHostHttpServer instance;
    private final Cache<String, Boolean> oneTimePackUrls = Caffeine.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();
    private final Cache<String, IpAccessRecord> ipAccessCache = Caffeine.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private final ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private HttpServer server;

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong blockedRequests = new AtomicLong();

    private int rateLimit = 1;
    private long rateLimitInterval = 1000;
    private String ip = "localhost";
    private int port = -1;

    private volatile byte[] resourcePackBytes;
    private String packHash;
    private UUID packUUID;

    public void setIp(String ip) {
        this.ip = ip;
    }

    @NotNull
    public ResourcePackDownloadData generateOneTimeUrl(UUID player) {
        String token = UUID.randomUUID().toString();
        this.oneTimePackUrls.put(token, true);
        return new ResourcePackDownloadData(
                url() + "download?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8),
                packUUID,
                packHash
        );
    }

    public String url() {
        return Config.hostProtocol() + "://" + ip + ":" + port + "/";
    }

    public void setResourcePackPath(Path resourcePackPath) {
        try {
            if (Files.exists(resourcePackPath)) {
                this.resourcePackBytes = Files.readAllBytes(resourcePackPath);
                calculateHash();
            } else {
                CraftEngine.instance().logger().warn("Resource pack file not found: " + resourcePackPath);
            }
        } catch (IOException e) {
            CraftEngine.instance().logger().severe("Failed to load resource pack", e);
        }
    }

    private void calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(resourcePackBytes);
            byte[] hashBytes = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            this.packHash = hexString.toString();
            this.packUUID = UUID.nameUUIDFromBytes(packHash.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            CraftEngine.instance().logger().severe("SHA-1 algorithm not available", e);
        }
    }

    public void updatePort(int port) {
        if (port == this.port) return;
        if (server != null) disable();
        this.port = port;
        try {
            server = HttpServer.create(new InetSocketAddress("::", port), 0);
            server.createContext("/download", new ResourcePackHandler());
            server.createContext("/metrics", this::handleMetrics);
            server.setExecutor(threadPool);
            server.start();
            CraftEngine.instance().logger().info("HTTP server started on port: " + port);
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to start HTTP server", e);
        }
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        String metrics = "# TYPE total_requests counter\n"
                + "total_requests " + totalRequests.get() + "\n"
                + "# TYPE blocked_requests counter\n"
                + "blocked_requests " + blockedRequests.get();

        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, metrics.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(metrics.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void disable() {
        if (server != null) {
            server.stop(0);
            server = null;
            threadPool.shutdownNow();
        }
    }

    public void adjustRateLimit(int requestsPerSecond, int rateLimitInterval) {
        this.rateLimit = requestsPerSecond;
        this.rateLimitInterval = rateLimitInterval;
        CraftEngine.instance().logger().info("Updated rate limit to " + requestsPerSecond + "/s");
    }

    private class ResourcePackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            totalRequests.incrementAndGet();

            String clientIp = getClientIp(exchange);

            if (checkRateLimit(clientIp)) {
                handleBlockedRequest(exchange, 429, "Rate limit exceeded");
                return;
            }

            String token = parseToken(exchange);
            if (!validateToken(token)) {
                handleBlockedRequest(exchange, 403, "Invalid token");
                return;
            }

            if (!validateClient(exchange)) {
                handleBlockedRequest(exchange, 403, "Invalid client");
                return;
            }

            if (resourcePackBytes == null) {
                handleBlockedRequest(exchange, 404, "Resource pack missing");
                return;
            }

            sendResourcePack(exchange);
        }

        private String getClientIp(HttpExchange exchange) {
            return exchange.getRemoteAddress().getAddress().getHostAddress();
        }

        private boolean checkRateLimit(String clientIp) {
            IpAccessRecord record = ipAccessCache.getIfPresent(clientIp);
            long now = System.currentTimeMillis();
            if (record == null) {
                record = new IpAccessRecord(now, 1);
                ipAccessCache.put(clientIp, record);
            } else {
                if (now - record.lastAccessTime > rateLimitInterval) {
                    record = new IpAccessRecord(now, 1);
                    ipAccessCache.put(clientIp, record);
                } else {
                    record.accessCount++;
                }
            }
            return record.accessCount > rateLimit;
        }

        private String parseToken(HttpExchange exchange) {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            return params.get("token");
        }

        private boolean validateToken(String token) {
            if (token == null || token.length() != 36) return false;

            Boolean valid = oneTimePackUrls.getIfPresent(token);
            if (valid != null) {
                oneTimePackUrls.invalidate(token);
                return true;
            }
            return false;
        }

        private boolean validateClient(HttpExchange exchange) {
            if (!Config.denyNonMinecraftRequest()) return true;

            String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            return userAgent != null && userAgent.startsWith("Minecraft Java/");
        }

        private void sendResourcePack(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(resourcePackBytes.length));
            exchange.sendResponseHeaders(200, resourcePackBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resourcePackBytes);
            } catch (IOException e) {
                CraftEngine.instance().logger().warn("Failed to send resource pack", e);
                throw e;
            }
        }

        private void handleBlockedRequest(HttpExchange exchange, int code, String reason) throws IOException {
            blockedRequests.incrementAndGet();
            CraftEngine.instance().debug(() ->
                    String.format("Blocked request [%s] %s: %s",
                            code,
                            exchange.getRemoteAddress(),
                            reason)
            );
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> params = new HashMap<>();
            if (query == null) return params;

            for (String pair : query.split("&")) {
                int idx = pair.indexOf("=");
                String key = idx > 0 ? pair.substring(0, idx) : pair;
                String value = idx > 0 ? pair.substring(idx + 1) : "";
                params.put(key, value);
            }
            return params;
        }
    }

    private static class IpAccessRecord {
        final long lastAccessTime;
        int accessCount;

        IpAccessRecord(long lastAccessTime, int accessCount) {
            this.lastAccessTime = lastAccessTime;
            this.accessCount = accessCount;
        }
    }

    public static SelfHostHttpServer instance() {
        if (instance == null) {
            instance = new SelfHostHttpServer();
        }
        return instance;
    }
}