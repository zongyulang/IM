package com.vim.webpage.Utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 用途：
 * 1.校验cdn资源请求，并且解密资源路径
 * 2.进行加密签名操作
 * CDN 解密与签名工具类
 */
@Component
public class CDN_decrypt {

    @Value("${cdn.domain}")
    private String cdnDomain;
    @Value("${cdn.secretIGSK}")
    private String secretIGSK;
    @Value("${cdn.secretTGSK}")
    private String secretTGSK;
    @Value("${cdn.noCacheCdn}")
    private String noCacheCdn;

    // 获取 CDN 域名列表
    private List<String> getCdnList() {
        return Arrays.asList(cdnDomain.split(","));
    }

    // base64url 编码
    public static String base64urlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    // base64url 解码
    public static byte[] base64urlDecode(String input) {
        return Base64.getUrlDecoder().decode(input);
    }

    // HMAC-SHA256 并 base64url
    public static String hmacSha256B64Url(String input, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64urlEncode(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }

    // getCdnHost
    public String getCdnHost(String pathSig) {
        List<String> cdnList = getCdnList();
        char lastChar = pathSig.trim().isEmpty() ? 'a'
                : Character.toLowerCase(pathSig.trim().charAt(pathSig.trim().length() - 1));
        int charCode = lastChar;
        if ((charCode >= 97 && charCode <= 122) || (charCode >= 48 && charCode <= 57)) {
            int idx = charCode % cdnList.size();
            return cdnList.get(idx);
        }
        return cdnList.get(cdnList.size() - 1);
    }

    // sign
    public static String sign(String input, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String b64 = Base64.getEncoder().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
            return b64.replace('+', '-').replace('/', '_').replaceAll("=+$", "");
        } catch (Exception e) {
            throw new IllegalStateException("sign error", e);
        }
    }

    // setM3u8TsPath,设置加密m3u8文件
    public String setM3u8TsPath(String m3u8Content, String fullPathPrefix, String secret, String ipa, String hdl,
            String userAgent, int expireSeconds) {
        if (secret == null || secret.isEmpty())
            secret = secretTGSK;
        if (ipa == null)
            ipa = "127.0.0.1";
        if (hdl == null)
            hdl = "-1";
        if (userAgent == null)
            userAgent = "";
        int now = (int) (System.currentTimeMillis() / 1000);
        int validFrom = now;
        int validTo = now + (expireSeconds > 0 ? expireSeconds : 3600);
        String normalizedPrefix = fullPathPrefix.replace("\\", "/").replaceAll("/+$", "") + "/";
        String base64path = base64urlEncode(normalizedPrefix.getBytes(StandardCharsets.UTF_8));
        String pathSig = hmacSha256B64Url(base64path, secret);
        String dataToSign = validFrom + "" + validTo + ipa + hdl + userAgent;
        String hash = java.net.URLEncoder.encode(hmacSha256B64Url(dataToSign, secret), StandardCharsets.UTF_8);
        String query = "?validfrom=" + validFrom + "&validto=" + validTo + "&ipa=" + ipa + "&hdl=" + hdl + "&hash="
                + hash;
        String signedPrefix = getCdnHost(pathSig) + "/hls/" + pathSig + "/" + base64path + "/";
        StringBuilder rewritten = new StringBuilder();
        for (String line : m3u8Content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#EXT-X-KEY")) {
                int uriIdx = trimmed.indexOf("URI=\"");
                if (uriIdx >= 0) {
                    int endIdx = trimmed.indexOf('"', uriIdx + 5);
                    if (endIdx > uriIdx) {
                        String keyFile = trimmed.substring(uriIdx + 5, endIdx);
                        String keyUrl = signedPrefix + keyFile + query;
                        rewritten.append(trimmed.replaceAll("URI=\"[^\"]+\"", "URI=\"" + keyUrl + "\"")).append("\n");
                        continue;
                    }
                }
            }
            //如果不是最后一行，并且不是注释行，且以 .ts 结尾
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.endsWith(".ts")) {
                String fileName = trimmed.substring(trimmed.lastIndexOf('/') + 1);
                String hd = "-1".equals(hdl) ? "720P/" : "1080P/";
                rewritten.append(signedPrefix).append(hd).append(fileName).append(query).append("\n");
                continue;
            }
            rewritten.append(line).append("\n");
        }
        return rewritten.toString();
    }

    // generateSignedPathWithVersion
    public String generateSignedPathWithVersion(String path) {
        path = path.replace("\\", "/");
        String base64path = base64urlEncode(path.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha256B64Url(base64path, secretIGSK);
        return getCdnHost(signature) + "/proxy/" + signature + "/" + base64path;
    }

    // generateM3u8Hash
    public String generateM3u8Hash(String path) {
        path = path.replace("\\", "/");
        String secret = secretTGSK;
        int now = (int) (System.currentTimeMillis() / 1000);
        int validFrom = now;
        int validTo = now + 7200;
        int lastSlashIndex = path.lastIndexOf('/');
        String fileName = path.substring(lastSlashIndex + 1);
        String prefixPath = path.substring(0, lastSlashIndex + 1);
        if (!fileName.endsWith(".m3u8"))
            throw new IllegalArgumentException("路径不是 .m3u8 文件");
        String base64path = base64urlEncode(prefixPath.getBytes(StandardCharsets.UTF_8));
        String pathSig = hmacSha256B64Url(base64path, secret);
        String dataToSign = path + validFrom + validTo;
        String hash = java.net.URLEncoder.encode(hmacSha256B64Url(dataToSign, secret), StandardCharsets.UTF_8);
        String query = "?validfrom=" + validFrom + "&validto=" + validTo + "&hash=" + hash;
        return noCacheCdn + "/M3u8/" + pathSig + "/" + base64path + "/" + fileName + query;
    }

    // verifyM3u8Hash
    public VerifyResult verifyM3u8Hash(String fullUrl, String secret) {
        try {
            java.net.URL url = new java.net.URL(fullUrl);
            String pathname = url.getPath();
            String query = url.getQuery();
            String[] params = query.split("&");
            String validFrom = null, validTo = null, hash = null;
            for (String p : params) {
                if (p.startsWith("validfrom="))
                    validFrom = p.substring(9);
                else if (p.startsWith("validto="))
                    validTo = p.substring(8);
                else if (p.startsWith("hash="))
                    hash = p.substring(5);
            }
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/M3u8/([^/]+)/([^/]+)/(.+\\.m3u8)$");
            java.util.regex.Matcher matcher = pattern.matcher(pathname);
            if (!matcher.find())
                return new VerifyResult(false, "Invalid path format", null);
            String pathSig = matcher.group(1);
            String base64path = matcher.group(2);
            String fileName = matcher.group(3);
            String prefixPath = new String(base64urlDecode(base64path), StandardCharsets.UTF_8);
            String originalPath = prefixPath + fileName;
            String expectedPathSig = hmacSha256B64Url(base64path, secret);
            if (!expectedPathSig.equals(pathSig))
                return new VerifyResult(false, "Path signature mismatch", null);
            int now = (int) (System.currentTimeMillis() / 1000);
            int vf = Integer.parseInt(validFrom);
            int vt = Integer.parseInt(validTo);
            if (now < vf || now > vt)
                return new VerifyResult(false, "Link expired or invalid time range", null);
            String dataToSign = originalPath + validFrom + validTo;
            String expectedHash = hmacSha256B64Url(dataToSign, secret);
            if (!java.net.URLDecoder.decode(hash, StandardCharsets.UTF_8).equals(expectedHash))
                return new VerifyResult(false, "Hash mismatch", null);
            return new VerifyResult(true, null, originalPath);
        } catch (Exception e) {
            return new VerifyResult(false, e.getMessage(), null);
        }
    }

    // generateSignedPreviewPrefix
    public String generateSignedPreviewPrefix(String path) {
        String base64path = base64urlEncode(path.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha256B64Url(base64path, secretIGSK);
        return getCdnHost(signature) + "/preview/" + signature + "/" + base64path;
    }

    // verifyPreviewImagePath
    public VerifyResult verifyPreviewImagePath(String request, String secret) {
        try {
            java.net.URL url = new java.net.URL("http://localhost" + request);
            String pathname = url.getPath();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/preview/([^/]+)/([^/]+)$");
            java.util.regex.Matcher matcher = pattern.matcher(pathname);
            if (!matcher.find())
                return new VerifyResult(false, "Invalid path format", null);
            String signature = matcher.group(1);
            String base64Path = matcher.group(2);
            String decodedPath = new String(base64urlDecode(base64Path), StandardCharsets.UTF_8);
            if (!decodedPath.startsWith("/") || decodedPath.contains(".."))
                return new VerifyResult(false, "Unsafe or invalid path", null);
            String expectedSig = sign(base64Path, secret);
            if (!signature.equals(expectedSig))
                return new VerifyResult(false, "Signature verification failed", null);
            String indexStr = url.getQuery() != null ? url.getQuery().replace("index=", "") : "0";
            int index = Integer.parseInt(indexStr);
            if (index < 0 || index > 100)
                return new VerifyResult(false, "Invalid index", null);
            String fullPath = decodedPath + "/preview_image_" + index + ".jpg";
            return new VerifyResult(true, null, fullPath);
        } catch (Exception e) {
            return new VerifyResult(false, e.getMessage(), null);
        }
    }

    // verifySignedPathWithVersion
    public VerifyResult verifySignedPathWithVersion(String request, String secret) {
        String[] parts = request.split("/");
        if (parts.length < 4)
            return new VerifyResult(false, "Invalid format", null);
        String signature = parts[2];
        String base64path = parts[3];
        String expectedSig = sign(base64path, secret);
        if (!signature.equals(expectedSig))
            return new VerifyResult(false, "Invalid signature", null);
        try {
            String decodedPath = new String(base64urlDecode(base64path), StandardCharsets.UTF_8);
            return new VerifyResult(true, null, decodedPath);
        } catch (Exception e) {
            return new VerifyResult(false, "Invalid base64", null);
        }
    }

    // 校验结果类
    public static class VerifyResult {
        public boolean valid;
        public String reason;
        public String decodedPath;

        public VerifyResult(boolean valid, String reason, String decodedPath) {
            this.valid = valid;
            this.reason = reason;
            this.decodedPath = decodedPath;
        }
    }
}
