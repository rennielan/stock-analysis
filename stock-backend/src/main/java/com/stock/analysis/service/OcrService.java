package com.stock.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.analysis.entity.TradeRecord;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    @Value("${alibaba.dashscope.api-key:YOUR_API_KEY}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TradeRecord> recognizeTradeRecords(MultipartFile imageFile) throws IOException {
        // 1. 将图片转换为 Base64
        byte[] imageBytes = imageFile.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + imageFile.getContentType() + ";base64," + base64Image;

        // 2. 构建请求 JSON
        // 针对 qwen-vl-ocr-latest 的请求结构
        String jsonPayload = buildPayload(dataUrl);

        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 3. 调用 API
        logger.info("Calling Qwen VL OCR API...");
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("API call failed: {}", response.body() != null ? response.body().string() : "No response body");
                throw new RuntimeException("OCR API 调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            logger.debug("API Response: {}", responseBody);
            
            // 4. 解析结果
            return parseResponse(responseBody);
        }
    }

    private String buildPayload(String dataUrl) {
        // 我们需要强引导模型以 JSON 格式输出结构化数据
        String prompt = "你是一个股票交易记录识别专家。请识别图片中的交易记录。\n" +
                "请仔细观察图片，识别出所有字段。\n" +
                "请严格按照以下 JSON 数组格式返回数据，不要输出任何其他的文字或解释。\n" +
                "如果有多条记录，请返回包含多个对象的数组。\n" +
                "格式要求：\n" +
                "[\n" +
                "  {\n" +
                "    \"orderNumber\": \"委托编号(主键，从图片中提取)\",\n" +
                "    \"stockCode\": \"证券代码(如: 000807)\",\n" +
                "    \"stockName\": \"证券名称(如: 云铝股份)\",\n" +
                "    \"settlementDate\": \"交收日期(如: 20260106)\",\n" +
                "    \"businessName\": \"业务名称(如: 证券买入、证券卖出、股息红利税补缴)\",\n" +
                "    \"tradePrice\": 成交价格(浮点数),\n" +
                "    \"quantity\": 成交数量(整数),\n" +
                "    \"tradeAmount\": 成交金额(浮点数),\n" +
                "    \"commission\": 手续费(浮点数),\n" +
                "    \"stampTax\": 印花税(浮点数),\n" +
                "    \"transferFee\": 过户费(浮点数),\n" +
                "    \"additionalFee\": 附加费(浮点数),\n" +
                "    \"exchangeClearingFee\": 交易所清算费(浮点数),\n" +
                "    \"fundCommission\": 基金手续费(浮点数),\n" +
                "    \"regulatoryFee\": 规费(浮点数),\n" +
                "    \"exchangeDifference\": 换汇尾差(浮点数),\n" +
                "    \"clearingAmount\": 清算金额(浮点数，负数表示支出，正数表示收入),\n" +
                "    \"fundBalance\": 资金本次余额(浮点数),\n" +
                "    \"settlementFlag\": \"交收标志(如: 已交收)\",\n" +
                "    \"tradeTime\": \"成交时间(必须是 yyyy-MM-dd HH:mm:ss 格式，请从图片中提取日期和时间组成此格式)\",\n" +
                "    \"shareholderCode\": \"股东代码\",\n" +
                "    \"fundAccount\": \"资金账号\",\n" +
                "    \"customerCode\": \"客户代码\",\n" +
                "    \"currency\": \"币种(如: 人民币)\",\n" +
                "    \"exchangeName\": \"交易所名称(如: 深圳A股、上海A股)\"\n" +
                "  }\n" +
                "]";

        // 构建符合 OpenAI 兼容格式的 payload
        return String.format(
            "{\n" +
            "  \"model\": \"qwen-vl-ocr-latest\",\n" +
            "  \"messages\": [\n" +
            "    {\n" +
            "      \"role\": \"user\",\n" +
            "      \"content\": [\n" +
            "        {\"type\": \"text\", \"text\": \"%s\"},\n" +
            "        {\"type\": \"image_url\", \"image_url\": {\"url\": \"%s\"}}\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}", escapeJson(prompt), dataUrl
        );
    }
    
    private String escapeJson(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private List<TradeRecord> parseResponse(String responseBody) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        String content = rootNode.path("choices").path(0).path("message").path("content").asText();
        
        // 有时候模型可能会加上 markdown 的 code block 标记 ```json ... ```
        content = extractJsonFromMarkdown(content);
        
        logger.info("Extracted JSON content: {}", content);
        
        List<TradeRecord> records = new ArrayList<>();
        JsonNode arrayNode = objectMapper.readTree(content);
        
        if (arrayNode.isArray()) {
            DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (JsonNode node : arrayNode) {
                try {
                    TradeRecord record = new TradeRecord();
                    
                    // 设置主键 - 委托编号
                    if (node.has("orderNumber") && !node.get("orderNumber").isNull()) {
                        record.setOrderNumber(node.get("orderNumber").asText());
                    }
                    
                    // 证券代码和名称
                    record.setStockCode(node.has("stockCode") && !node.get("stockCode").isNull() 
                            ? node.get("stockCode").asText() : "");
                    record.setStockName(node.has("stockName") && !node.get("stockName").isNull() 
                            ? node.get("stockName").asText() : "");
                    
                    // 交收日期
                    String settlementDate = null;
                    if (node.has("settlementDate") && !node.get("settlementDate").isNull()) {
                        settlementDate = node.get("settlementDate").asText();
                        record.setSettlementDate(settlementDate);
                    }
                    
                    // 业务名称
                    if (node.has("businessName") && !node.get("businessName").isNull()) {
                        record.setBusinessName(node.get("businessName").asText());
                    }
                    
                    // 成交价格
                    if (node.has("tradePrice") && !node.get("tradePrice").isNull()) {
                        record.setTradePrice(new BigDecimal(node.get("tradePrice").asText()));
                    }
                    
                    // 成交数量
                    if (node.has("quantity") && !node.get("quantity").isNull()) {
                        record.setQuantity(node.get("quantity").asInt());
                    }
                    
                    // 成交金额
                    if (node.has("tradeAmount") && !node.get("tradeAmount").isNull()) {
                        record.setTradeAmount(new BigDecimal(node.get("tradeAmount").asText()));
                    }
                    
                    // 手续费
                    if (node.has("commission") && !node.get("commission").isNull()) {
                        record.setCommission(new BigDecimal(node.get("commission").asText()));
                    }
                    
                    // 印花税
                    if (node.has("stampTax") && !node.get("stampTax").isNull()) {
                        record.setStampTax(new BigDecimal(node.get("stampTax").asText()));
                    }
                    
                    // 过户费
                    if (node.has("transferFee") && !node.get("transferFee").isNull()) {
                        record.setTransferFee(new BigDecimal(node.get("transferFee").asText()));
                    }
                    
                    // 附加费
                    if (node.has("additionalFee") && !node.get("additionalFee").isNull()) {
                        record.setAdditionalFee(new BigDecimal(node.get("additionalFee").asText()));
                    }
                    
                    // 交易所清算费
                    if (node.has("exchangeClearingFee") && !node.get("exchangeClearingFee").isNull()) {
                        record.setExchangeClearingFee(new BigDecimal(node.get("exchangeClearingFee").asText()));
                    }
                    
                    // 基金手续费
                    if (node.has("fundCommission") && !node.get("fundCommission").isNull()) {
                        record.setFundCommission(new BigDecimal(node.get("fundCommission").asText()));
                    }
                    
                    // 规费
                    if (node.has("regulatoryFee") && !node.get("regulatoryFee").isNull()) {
                        record.setRegulatoryFee(new BigDecimal(node.get("regulatoryFee").asText()));
                    }
                    
                    // 换汇尾差
                    if (node.has("exchangeDifference") && !node.get("exchangeDifference").isNull()) {
                        record.setExchangeDifference(new BigDecimal(node.get("exchangeDifference").asText()));
                    }
                    
                    // 清算金额
                    if (node.has("clearingAmount") && !node.get("clearingAmount").isNull()) {
                        record.setClearingAmount(new BigDecimal(node.get("clearingAmount").asText()));
                    }
                    
                    // 资金本次余额
                    if (node.has("fundBalance") && !node.get("fundBalance").isNull()) {
                        record.setFundBalance(new BigDecimal(node.get("fundBalance").asText()));
                    }
                    
                    // 交收标志
                    if (node.has("settlementFlag") && !node.get("settlementFlag").isNull()) {
                        record.setSettlementFlag(node.get("settlementFlag").asText());
                    }
                    
                    // 处理时间解析：使用交收日期 + 成交时间拼接
                    // 交收日期格式：20260106
                    // 成交时间格式：10:24:07
                    LocalDateTime tradeTime = null;
                    
                    String tradeTimeStr = null;
                    if (node.has("tradeTime") && !node.get("tradeTime").isNull()) {
                        tradeTimeStr = node.get("tradeTime").asText();
                    }
                    
                    if (settlementDate != null && tradeTimeStr != null) {
                        // 拼接交收日期和成交时间
                        String dateTimeStr = settlementDate + " " + tradeTimeStr;
                        try {
                            // 尝试解析格式：yyyyMMdd HH:mm:ss
                            DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
                            tradeTime = LocalDateTime.parse(dateTimeStr, customFormatter);
                        } catch (DateTimeParseException e) {
                            logger.warn("Failed to parse combined datetime '{}': {}", dateTimeStr, e.getMessage());
                            // 如果拼接失败，尝试单独解析成交时间
                            if (tradeTimeStr != null) {
                                try {
                                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                                    java.time.LocalTime time = java.time.LocalTime.parse(tradeTimeStr, timeFormatter);
                                    // 使用当前日期 + 解析的时间
                                    tradeTime = java.time.LocalDate.now().atTime(time);
                                } catch (DateTimeParseException ex) {
                                    logger.warn("Failed to parse time '{}', using current time", tradeTimeStr);
                                    tradeTime = LocalDateTime.now();
                                }
                            }
                        }
                    } else if (tradeTimeStr != null) {
                        // 只有成交时间，没有交收日期
                        try {
                            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                            java.time.LocalTime time = java.time.LocalTime.parse(tradeTimeStr, timeFormatter);
                            tradeTime = java.time.LocalDate.now().atTime(time);
                        } catch (DateTimeParseException e) {
                            logger.warn("Failed to parse time '{}', using current time", tradeTimeStr);
                            tradeTime = LocalDateTime.now();
                        }
                    } else {
                        // 都没有，使用当前时间
                        tradeTime = LocalDateTime.now();
                    }
                    
                    record.setTradeTime(tradeTime);
                    
                    // 股东代码
                    if (node.has("shareholderCode") && !node.get("shareholderCode").isNull()) {
                        record.setShareholderCode(node.get("shareholderCode").asText());
                    }
                    
                    // 资金账号
                    if (node.has("fundAccount") && !node.get("fundAccount").isNull()) {
                        record.setFundAccount(node.get("fundAccount").asText());
                    }
                    
                    // 客户代码
                    if (node.has("customerCode") && !node.get("customerCode").isNull()) {
                        record.setCustomerCode(node.get("customerCode").asText());
                    }
                    
                    // 币种
                    if (node.has("currency") && !node.get("currency").isNull()) {
                        record.setCurrency(node.get("currency").asText());
                    }
                    
                    // 交易所名称
                    if (node.has("exchangeName") && !node.get("exchangeName").isNull()) {
                        record.setExchangeName(node.get("exchangeName").asText());
                    }
                    
                    records.add(record);
                } catch (Exception e) {
                    logger.error("Failed to parse individual record: {}", node.toString(), e);
                }
            }
        }
        
        return records;
    }
    
    private String extractJsonFromMarkdown(String content) {
        Pattern pattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return content;
    }
}
