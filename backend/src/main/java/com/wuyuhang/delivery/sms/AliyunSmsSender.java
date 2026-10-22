package com.wuyuhang.delivery.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 阿里云短信通道（真实发送）。
 * <p>
 * 实现说明：不引入阿里云 SDK，而是按官方 RPC 签名规则（HMAC-SHA1）直接构造 HTTP 请求，
 * 好处是不给项目增加一个几十 MB 的依赖，也便于看清签名过程。
 * <p>
 * <b>启用前提</b>（三者缺一不可，且在阿里云控制台申请通过）：
 * <ol>
 *   <li>{@code sms.aliyun.access-key-id} / {@code access-key-secret}：建议用环境变量注入</li>
 *   <li>{@code sms.aliyun.sign-name}：短信签名，如「快递驿站系统」</li>
 *   <li>{@code sms.aliyun.template-code}：模板 CODE，模板变量名为 {@code code}</li>
 * </ol>
 * 然后设置 {@code sms.provider: aliyun}。
 * <p>
 * <b>注意</b>：本类按官方文档实现，但项目开发阶段未申请到真实签名与模板，
 * 因此没有做过线上联调。启用前请先用一条真实短信验证签名与模板变量是否正确。
 *
 * @author 吴宇航
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunSmsSender implements SmsSender {

    private static final String ENDPOINT = "https://dysmsapi.aliyuncs.com/";
    private static final String API_VERSION = "2017-05-25";
    private static final String ALGORITHM = "HMAC-SHA1";

    private final SmsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String provider() {
        return "aliyun";
    }

    @Override
    public boolean send(SmsMessage message) {
        SmsProperties.Aliyun cfg = properties.getAliyun();
        if (!StringUtils.hasText(cfg.getAccessKeyId())
                || !StringUtils.hasText(cfg.getAccessKeySecret())
                || !StringUtils.hasText(cfg.getSignName())
                || !StringUtils.hasText(cfg.getTemplateCode())) {
            log.warn("[ALIYUN-SMS] AccessKeyId / AccessKeySecret / signName / templateCode 未配置完整，无法发送");
            return false;
        }
        try {
            // 1) 业务参数
            Map<String, String> params = new TreeMap<>();
            params.put("AccessKeyId", cfg.getAccessKeyId());
            params.put("Action", "SendSms");
            params.put("Format", "JSON");
            params.put("PhoneNumbers", message.getPhone());
            params.put("RegionId", cfg.getRegionId());
            params.put("SignName", cfg.getSignName());
            params.put("SignatureMethod", ALGORITHM);
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("SignatureVersion", "1.0");
            params.put("TemplateCode", cfg.getTemplateCode());
            params.put("TemplateParam", objectMapper.writeValueAsString(
                    Map.of(cfg.getTemplateParamName(), message.getCode())));
            params.put("Timestamp", ZonedDateTime.now(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
            params.put("Version", API_VERSION);

            // 2) 计算签名（阿里云 RPC 规则）
            String signature = sign(params, cfg.getAccessKeySecret());
            params.put("Signature", signature);

            // 3) 发起 GET 请求
            String url = ENDPOINT + "?" + toQueryString(params);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // 4) 解析结果：Code == "OK" 视为成功
            Map<?, ?> result = objectMapper.readValue(response.body(), Map.class);
            String code = String.valueOf(result.get("Code"));
            if ("OK".equalsIgnoreCase(code)) {
                log.info("[ALIYUN-SMS] 发送成功 手机号={} RequestId={}", message.getPhone(), result.get("RequestId"));
                return true;
            }
            log.warn("[ALIYUN-SMS] 发送失败 手机号={} Code={} Message={}", message.getPhone(), code, result.get("Message"));
            return false;
        } catch (Exception e) {
            log.error("[ALIYUN-SMS] 调用异常：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 计算阿里云 RPC 签名。
     * <p>
     * StringToSign = GET & percentEncode("/") & percentEncode(按 key 排序的查询串)，
     * 再用 {@code accessKeySecret + "&"} 作为密钥做 HMAC-SHA1，最后 Base64。
     */
    private String sign(Map<String, String> params, String accessKeySecret) throws Exception {
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (canonical.length() > 0) {
                canonical.append('&');
            }
            canonical.append(percentEncode(e.getKey())).append('=').append(percentEncode(e.getValue()));
        }
        String stringToSign = "GET&" + percentEncode("/") + "&" + percentEncode(canonical.toString());

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] digest = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }

    /**
     * 阿里云要求的特殊 URL 编码：标准 URL 编码后把 + %2A %7E 再替换一次。
     */
    private String percentEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            throw new IllegalStateException("URL 编码失败", e);
        }
    }

    /** 拼装查询串（key 已按字母序排序）。 */
    private String toQueryString(Map<String, String> params) {
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, String> e : new LinkedHashMap<>(params).entrySet()) {
            pairs.add(percentEncode(e.getKey()) + "=" + percentEncode(e.getValue()));
        }
        return String.join("&", pairs);
    }
}
