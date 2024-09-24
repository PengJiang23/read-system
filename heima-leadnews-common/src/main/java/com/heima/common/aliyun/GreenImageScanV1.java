package com.heima.common.aliyun;


import com.alibaba.fastjson.JSON;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.DescribeUploadTokenResponse;
import com.aliyun.green20220302.models.DescribeUploadTokenResponseBody;
import com.aliyun.green20220302.models.ImageModerationRequest;
import com.aliyun.green20220302.models.ImageModerationResponse;
import com.aliyun.green20220302.models.ImageModerationResponseBody;
import com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyData;
import com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyDataResult;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aliyun")
public class GreenImageScanV1 {


    private String accessKeyId;
    private String secret;


    //服务是否部署在vpc上
    public static boolean isVPC = false;

    //文件上传token endpoint->token
    public static Map<String, DescribeUploadTokenResponseBody.DescribeUploadTokenResponseBodyData> tokenMap = new HashMap<>();

    //上传文件请求客户端
    public static OSS ossClient = null;

    /**
     * 创建请求客户端
     *
     * @param accessKeyId
     * @param accessKeySecret
     * @param endpoint
     * @return
     * @throws Exception
     */
    public static Client createClient(String accessKeyId, String accessKeySecret, String endpoint) throws Exception {
        Config config = new Config();
        config.setAccessKeyId(accessKeyId);
        config.setAccessKeySecret(accessKeySecret);
        // 接入区域和地址请根据实际情况修改
        config.setEndpoint(endpoint);
        return new Client(config);
    }

    /**
     * 创建上传文件请求客户端
     *
     * @param tokenData
     * @param isVPC
     */
    public static void getOssClient(DescribeUploadTokenResponseBody.DescribeUploadTokenResponseBodyData tokenData, boolean isVPC) {
        //注意，此处实例化的client请尽可能重复使用，避免重复建立连接，提升检测性能。
        if (isVPC) {
            ossClient = new OSSClientBuilder().build(tokenData.ossInternalEndPoint, tokenData.getAccessKeyId(), tokenData.getAccessKeySecret(), tokenData.getSecurityToken());
        } else {
            ossClient = new OSSClientBuilder().build(tokenData.ossInternetEndPoint, tokenData.getAccessKeyId(), tokenData.getAccessKeySecret(), tokenData.getSecurityToken());
        }
    }

    /**
     * 上传文件
     *
     * @param filePath
     * @param tokenData
     * @return
     * @throws Exception
     */
    public static String uploadFile(String filePath, DescribeUploadTokenResponseBody.DescribeUploadTokenResponseBodyData tokenData) throws Exception {
        String[] split = filePath.split("\\.");
        String objectName;
        if (split.length > 1) {
            objectName = tokenData.getFileNamePrefix() + UUID.randomUUID() + "." + split[split.length - 1];
        } else {
            objectName = tokenData.getFileNamePrefix() + UUID.randomUUID();
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(tokenData.getBucketName(), objectName, new File(filePath));
        ossClient.putObject(putObjectRequest);
        return objectName;
    }

    public static ImageModerationResponse invokeFunction(String accessKeyId, String accessKeySecret, String endpoint, String filePath) throws Exception {
        //注意，此处实例化的client请尽可能重复使用，避免重复建立连接，提升检测性能。

        Client client = createClient(accessKeyId, accessKeySecret, endpoint);

        RuntimeOptions runtime = new RuntimeOptions();

        //本地文件的完整路径，例如D:\localPath\exampleFile.png。
//        String filePath = "D:\\Users\\JP\\Desktop\\1.jpg";
        //获取文件上传token
        if (tokenMap.get(endpoint) == null || tokenMap.get(endpoint).expiration <= System.currentTimeMillis() / 1000) {
            DescribeUploadTokenResponse tokenResponse = client.describeUploadToken();
            tokenMap.put(endpoint, tokenResponse.getBody().getData());
        }
        //上传文件请求客户端
        getOssClient(tokenMap.get(endpoint), isVPC);

        //上传文件
        String objectName = uploadFile(filePath, tokenMap.get(endpoint));

        // 检测参数构造。
        Map<String, String> serviceParameters = new HashMap<>();
        //文件上传信息
        serviceParameters.put("ossBucketName", tokenMap.get(endpoint).getBucketName());
        serviceParameters.put("ossObjectName", objectName);
        serviceParameters.put("dataId", UUID.randomUUID().toString());

        ImageModerationRequest request = new ImageModerationRequest();
        // 图片检测service：内容安全控制台图片增强版规则配置的serviceCode，示例：baselineCheck
        request.setService("baselineCheck");
        request.setServiceParameters(JSON.toJSONString(serviceParameters));

        ImageModerationResponse response = null;
        try {
            response = client.imageModerationWithOptions(request, runtime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }


    public Map imageScan(byte[] bytes) throws Exception {


        File tempFile = writeBytesToTempFile(bytes);


        Map<String, Object> map = new HashMap<>();

        // 接入区域和地址请根据实际情况修改。
        ImageModerationResponse response = invokeFunction(accessKeyId, secret, "green-cip.cn-shanghai.aliyuncs.com", tempFile.getAbsolutePath());
        try {
            // 自动路由。
            if (response != null) {
                //区域切换到cn-beijing。
                if (500 == response.getStatusCode() || (response.getBody() != null && 500 == (response.getBody().getCode()))) {
                    // 接入区域和地址请根据实际情况修改。
                    response = invokeFunction(accessKeyId, secret, "green-cip.cn-beijing.aliyuncs.com", tempFile.getAbsolutePath());
                }
            }
            // 打印检测结果。
            if (response != null) {
                if (response.getStatusCode() == 200) {
                    ImageModerationResponseBody body = response.getBody();
                    map.put("requestId", body.getRequestId());
                    map.put("code", body.getCode());
                    map.put("msg", body.getMsg());
                    if (body.getCode() == 200) {
                        ImageModerationResponseBodyData data = body.getData();
                        map.put("dataId", data.getDataId());
                        List<ImageModerationResponseBodyDataResult> results = data.getResult();
                        for (ImageModerationResponseBodyDataResult result : results) {
                            map.put("description", result.getDescription());
                            map.put("label", result.getLabel());
                            map.put("confidence", result.getConfidence());
                        }
                    } else {
                        System.out.println("image moderation not success. code:" + body.getCode());
                    }
                } else {
                    System.out.println("response not success. status:" + response.getStatusCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (deleted) {
                    System.out.println("Temporary file deleted successfully.");
                } else {
                    System.out.println("Failed to delete temporary file.");
                }
            }
        }

        return map;
    }


    public File writeBytesToTempFile(byte[] bytes) throws IOException {
        // 创建临时文件
        File tempFile = Files.createTempFile("image", ".png").toFile();

        // 将字节流写入临时文件
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(bytes);
            fos.flush();
        }
        return tempFile;
    }


}
